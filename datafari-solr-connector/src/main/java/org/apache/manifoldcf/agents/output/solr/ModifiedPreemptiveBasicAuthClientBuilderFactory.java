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
import org.apache.solr.client.solrj.util.SolrBasicAuthentication;
import org.apache.solr.common.StringUtils;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.StrUtils;
import org.eclipse.jetty.client.HttpAuthenticationStore;
import org.eclipse.jetty.client.ProxyAuthenticationProtocolHandler;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;

public class ModifiedPreemptiveBasicAuthClientBuilderFactory implements ModifiedHttpClientBuilderFactory {
  /**
   * A system property used to specify a properties file containing default parameters used for creating a HTTP client. This is specifically useful for configuring the HTTP basic auth credentials
   * (i.e. username/password). The name of the property must match the relevant Solr config property name.
   */
  public static final String SYS_PROP_HTTP_CLIENT_CONFIG = "solr.httpclient.config";

  /**
   * A system property to configure the Basic auth credentials via a java system property. Since this will expose the password on the command-line, it is not very secure. But this mechanism is added
   * for backwards compatibility.
   */
  public static final String SYS_PROP_BASIC_AUTH_CREDENTIALS = "basicauth";

  private static PreemptiveAuth requestInterceptor = new PreemptiveAuth(new BasicScheme());

  private static CredentialsResolver CREDENTIAL_RESOLVER = new CredentialsResolver();

  /**
   * This method enables configuring system wide defaults (apart from using a config file based approach).
   */
  public static void setDefaultSolrParams(final SolrParams params) {
    CREDENTIAL_RESOLVER.defaultParams = params;
  }

  @Override
  public void close() throws IOException {
    HttpClientUtil.removeRequestInterceptor(requestInterceptor);
  }

  @Override
  public void setup(final ModifiedHttp2SolrClient client) {
    final String basicAuthUser = CREDENTIAL_RESOLVER.defaultParams.get(HttpClientUtil.PROP_BASIC_AUTH_USER);
    final String basicAuthPass = CREDENTIAL_RESOLVER.defaultParams.get(HttpClientUtil.PROP_BASIC_AUTH_PASS);
    this.setup(client, basicAuthUser, basicAuthPass);
  }

  public void setup(final ModifiedHttp2SolrClient client, final String basicAuthUser, final String basicAuthPass) {
    if (basicAuthUser == null || basicAuthPass == null) {
      throw new IllegalArgumentException("username & password must be specified with " + getClass().getName());
    }

    final HttpAuthenticationStore authenticationStore = new HttpAuthenticationStore();
    authenticationStore.addAuthentication(new SolrBasicAuthentication(basicAuthUser, basicAuthPass));
    client.getHttpClient().setAuthenticationStore(authenticationStore);
    client.getProtocolHandlers().put(new WWWAuthenticationProtocolHandler(client.getHttpClient()));
    client.getProtocolHandlers().put(new ProxyAuthenticationProtocolHandler(client.getHttpClient()));
  }

  @Override
  public SolrHttpClientBuilder getHttpClientBuilder(final SolrHttpClientBuilder builder) {
    final String basicAuthUser = CREDENTIAL_RESOLVER.defaultParams.get(HttpClientUtil.PROP_BASIC_AUTH_USER);
    final String basicAuthPass = CREDENTIAL_RESOLVER.defaultParams.get(HttpClientUtil.PROP_BASIC_AUTH_PASS);
    if (basicAuthUser == null || basicAuthPass == null) {
      throw new IllegalArgumentException("username & password must be specified with " + getClass().getName());
    }

    return initHttpClientBuilder(builder == null ? SolrHttpClientBuilder.create() : builder, basicAuthUser, basicAuthPass);
  }

  private SolrHttpClientBuilder initHttpClientBuilder(final SolrHttpClientBuilder builder, final String basicAuthUser, final String basicAuthPass) {
    builder.setDefaultCredentialsProvider(() -> {
      final CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(basicAuthUser, basicAuthPass));
      return credsProvider;
    });

    HttpClientUtil.addRequestInterceptor(requestInterceptor);
    return builder;
  }

  static class CredentialsResolver {

    public volatile SolrParams defaultParams;

    public CredentialsResolver() {
      final String credentials = System.getProperty(PreemptiveBasicAuthClientBuilderFactory.SYS_PROP_BASIC_AUTH_CREDENTIALS);
      final String configFile = System.getProperty(PreemptiveBasicAuthClientBuilderFactory.SYS_PROP_HTTP_CLIENT_CONFIG);

      if (credentials != null && configFile != null) {
        throw new IllegalArgumentException("Basic authentication credentials passed via a configuration file" + " as well as java system property. Please choose one mechanism!");
      }

      if (credentials != null) {
        final List<String> ss = StrUtils.splitSmart(credentials, ':');
        if (ss.size() != 2 || StringUtils.isEmpty(ss.get(0)) || StringUtils.isEmpty(ss.get(1))) {
          throw new IllegalArgumentException("Invalid Authentication credentials: Please provide 'basicauth' in the 'user:password' format");
        }
        final Map<String, String> paramMap = new HashMap<>();
        paramMap.put(HttpClientUtil.PROP_BASIC_AUTH_USER, ss.get(0));
        paramMap.put(HttpClientUtil.PROP_BASIC_AUTH_PASS, ss.get(1));
        defaultParams = new MapSolrParams(paramMap);
      } else if (configFile != null) {
        final Properties defaultProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(configFile), StandardCharsets.UTF_8)) {
          defaultProps.load(reader);
        } catch (final IOException e) {
          throw new IllegalArgumentException("Unable to read credentials file at " + configFile, e);
        }
        final Map<String, String> map = new HashMap<>();
        defaultProps.forEach((k, v) -> map.put((String) k, (String) v));
        defaultParams = new MapSolrParams(map);
      } else {
        defaultParams = null;
      }
    }
  }
}
