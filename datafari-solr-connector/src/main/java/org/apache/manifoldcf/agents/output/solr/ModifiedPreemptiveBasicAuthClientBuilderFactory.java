package org.apache.manifoldcf.agents.output.solr;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.PreemptiveAuth;
import org.apache.solr.client.solrj.impl.PreemptiveBasicAuthClientBuilderFactory;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.apache.solr.common.StringUtils;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.StrUtils;

public class ModifiedPreemptiveBasicAuthClientBuilderFactory implements ModifiedHttpClientBuilderFactory {
  public static final String SYS_PROP_HTTP_CLIENT_CONFIG = "solr.httpclient.config";
  public static final String SYS_PROP_BASIC_AUTH_CREDENTIALS = "basicauth";

  private static final PreemptiveAuth requestInterceptor = new PreemptiveAuth(new BasicScheme());
  private static final CredentialsResolver CREDENTIAL_RESOLVER = new CredentialsResolver();

  public static void setDefaultSolrParams(final SolrParams params) {
    CREDENTIAL_RESOLVER.defaultParams = params;
  }

  @Override
  public void close() throws IOException {
    HttpClientUtil.removeRequestInterceptor(requestInterceptor);
  }

  @Override
  public void setup(final ModifiedHttp2SolrClient client) {
    final String user = CREDENTIAL_RESOLVER.defaultParams.get(HttpClientUtil.PROP_BASIC_AUTH_USER);
    final String pass = CREDENTIAL_RESOLVER.defaultParams.get(HttpClientUtil.PROP_BASIC_AUTH_PASS);
    if (user == null || pass == null) {
      throw new IllegalArgumentException("username & password must be specified");
    }
    // Pas d’accès au client Jetty interne ; on se contente de l’intercepteur côté SolrJ
    HttpClientUtil.addRequestInterceptor(requestInterceptor);
  }

  @Override
  public SolrHttpClientBuilder getHttpClientBuilder(final SolrHttpClientBuilder builder) {
    final String user = CREDENTIAL_RESOLVER.defaultParams.get(HttpClientUtil.PROP_BASIC_AUTH_USER);
    final String pass = CREDENTIAL_RESOLVER.defaultParams.get(HttpClientUtil.PROP_BASIC_AUTH_PASS);
    if (user == null || pass == null) {
      throw new IllegalArgumentException("username & password must be specified");
    }
    return initHttpClientBuilder(builder == null ? SolrHttpClientBuilder.create() : builder, user, pass);
  }

  private SolrHttpClientBuilder initHttpClientBuilder(final SolrHttpClientBuilder builder, final String user, final String pass) {
    builder.setDefaultCredentialsProvider(() -> {
      final CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
      return credsProvider;
    });
    HttpClientUtil.addRequestInterceptor(requestInterceptor);
    return builder;
  }

  static class CredentialsResolver {
    public volatile SolrParams defaultParams;

    CredentialsResolver() {
      final String credentials = System.getProperty(PreemptiveBasicAuthClientBuilderFactory.SYS_PROP_BASIC_AUTH_CREDENTIALS);
      final String configFile = System.getProperty(PreemptiveBasicAuthClientBuilderFactory.SYS_PROP_HTTP_CLIENT_CONFIG);

      if (credentials != null && configFile != null) {
        throw new IllegalArgumentException("Configure either basicauth or solr.httpclient.config, not both.");
      }

      if (credentials != null) {
        final List<String> ss = StrUtils.splitSmart(credentials, ':');
        if (ss.size() != 2 || StringUtils.isEmpty(ss.get(0)) || StringUtils.isEmpty(ss.get(1))) {
          throw new IllegalArgumentException("Invalid basicauth format, expected user:password");
        }
        final Map<String, String> paramMap = new HashMap<>();
        paramMap.put(HttpClientUtil.PROP_BASIC_AUTH_USER, ss.get(0));
        paramMap.put(HttpClientUtil.PROP_BASIC_AUTH_PASS, ss.get(1));
        defaultParams = new MapSolrParams(paramMap);
      } else if (configFile != null) {
        final Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(configFile), StandardCharsets.UTF_8)) {
          props.load(reader);
        } catch (final IOException e) {
          throw new IllegalArgumentException("Unable to read credentials file at " + configFile, e);
        }
        final Map<String, String> map = new HashMap<>();
        props.forEach((k, v) -> map.put((String) k, (String) v));
        defaultParams = new MapSolrParams(map);
      } else {
        defaultParams = new MapSolrParams(new HashMap<>());
      }
    }
  }
}
