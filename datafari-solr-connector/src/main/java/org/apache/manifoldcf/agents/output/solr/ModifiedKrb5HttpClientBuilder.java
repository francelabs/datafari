package org.apache.manifoldcf.agents.output.solr;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.apache.solr.client.solrj.impl.SolrPortAwareCookieSpecFactory;
import org.eclipse.jetty.client.HttpAuthenticationStore;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.client.util.SPNEGOAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModifiedKrb5HttpClientBuilder implements ModifiedHttpClientBuilderFactory {

  public static final String LOGIN_CONFIG_PROP = "java.security.auth.login.config";
  private static final String SPNEGO_OID = "1.3.6.1.5.5.2";
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static Configuration jaasConfig = new SolrJaasConfiguration();

  public ModifiedKrb5HttpClientBuilder() {
  }

  /**
   * The jaasConfig is static, which makes it problematic for testing in the same jvm. Call this function to regenerate the static config (this is not thread safe). Note: only used for tests
   */
  public static void regenerateJaasConfiguration() {
    jaasConfig = new SolrJaasConfiguration();
  }

  public SolrHttpClientBuilder getBuilder() {
    return getBuilder(HttpClientUtil.getHttpClientBuilder());
  }

  @Override
  public void close() {
    HttpClientUtil.removeRequestInterceptor(bufferedEntityInterceptor);
  }

  @Override
  public SolrHttpClientBuilder getHttpClientBuilder(final SolrHttpClientBuilder builder) {
    return builder == null ? getBuilder() : getBuilder(builder);
  }

  private SPNEGOAuthentication createSPNEGOAuthentication() {
    final SPNEGOAuthentication authentication = new SPNEGOAuthentication(null) {

      @Override
      public boolean matches(final String type, final URI uri, final String realm) {
        return this.getType().equals(type);
      }
    };
    final String clientAppName = System.getProperty("solr.kerberos.jaas.appname", "Client");
    final AppConfigurationEntry[] entries = jaasConfig.getAppConfigurationEntry(clientAppName);
    if (entries == null) {
      log.warn("Could not find login configuration entry for {}. SPNego authentication may not be successful.", clientAppName);
      return authentication;
    }
    if (entries.length != 1) {
      log.warn("Multiple login modules are specified in the configuration file");
      return authentication;
    }

    final Map<String, ?> options = entries[0].getOptions();
    setAuthenticationOptions(authentication, options, (String) options.get("principal"));
    return authentication;
  }

  static void setAuthenticationOptions(final SPNEGOAuthentication authentication, final Map<String, ?> options, final String username) {
    final String keyTab = (String) options.get("keyTab");
    if (keyTab != null) {
      authentication.setUserKeyTabPath(Paths.get(keyTab));
    }
    authentication.setServiceName("HTTP");
    authentication.setUserName(username);
    if ("true".equalsIgnoreCase((String) options.get("useTicketCache"))) {
      authentication.setUseTicketCache(true);
      final String ticketCachePath = (String) options.get("ticketCache");
      if (ticketCachePath != null) {
        authentication.setTicketCachePath(Paths.get(ticketCachePath));
      }
      authentication.setRenewTGT("true".equalsIgnoreCase((String) options.get("renewTGT")));
    }
  }

  @Override
  public void setup(final ModifiedHttp2SolrClient http2Client) {
    final HttpAuthenticationStore authenticationStore = new HttpAuthenticationStore();
    authenticationStore.addAuthentication(createSPNEGOAuthentication());
    http2Client.getHttpClient().setAuthenticationStore(authenticationStore);
    http2Client.getProtocolHandlers().put(new WWWAuthenticationProtocolHandler(http2Client.getHttpClient()));
  }

  public SolrHttpClientBuilder getBuilder(final SolrHttpClientBuilder builder) {
    if (System.getProperty(LOGIN_CONFIG_PROP) != null) {
      final String configValue = System.getProperty(LOGIN_CONFIG_PROP);

      if (configValue != null) {
        log.info("Setting up SPNego auth with config: {}", configValue);
        final String useSubjectCredsProp = "javax.security.auth.useSubjectCredsOnly";
        final String useSubjectCredsVal = System.getProperty(useSubjectCredsProp);

        // "javax.security.auth.useSubjectCredsOnly" should be false so that the underlying
        // authentication mechanism can load the credentials from the JAAS configuration.
        if (useSubjectCredsVal == null) {
          System.setProperty(useSubjectCredsProp, "false");
        } else if (!useSubjectCredsVal.toLowerCase(Locale.ROOT).equals("false")) {
          // Don't overwrite the prop value if it's already been written to something else,
          // but log because it is likely the Credentials won't be loaded correctly.
          log.warn("System Property: {} set to: {} not false.  SPNego authentication may not be successful.", useSubjectCredsProp, useSubjectCredsVal);
        }

        javax.security.auth.login.Configuration.setConfiguration(jaasConfig);
        // Enable only SPNEGO authentication scheme.

        builder.setAuthSchemeRegistryProvider(() -> {
          final Lookup<AuthSchemeProvider> authProviders = RegistryBuilder.<AuthSchemeProvider>create().register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true, false)).build();
          return authProviders;
        });
        // Get the credentials from the JAAS configuration rather than here
        final Credentials useJaasCreds = new Credentials() {
          @Override
          public String getPassword() {
            return null;
          }

          @Override
          public Principal getUserPrincipal() {
            return null;
          }
        };

        HttpClientUtil.setCookiePolicy(SolrPortAwareCookieSpecFactory.POLICY_NAME);

        builder.setCookieSpecRegistryProvider(() -> {
          final SolrPortAwareCookieSpecFactory cookieFactory = new SolrPortAwareCookieSpecFactory();

          final Lookup<CookieSpecProvider> cookieRegistry = RegistryBuilder.<CookieSpecProvider>create().register(SolrPortAwareCookieSpecFactory.POLICY_NAME, cookieFactory).build();

          return cookieRegistry;
        });

        builder.setDefaultCredentialsProvider(() -> {
          final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
          credentialsProvider.setCredentials(AuthScope.ANY, useJaasCreds);
          return credentialsProvider;
        });
        HttpClientUtil.addRequestInterceptor(bufferedEntityInterceptor);
      }
    } else {
      log.warn("{} is configured without specifying system property '{}'", getClass().getName(), LOGIN_CONFIG_PROP);
    }

    return builder;
  }

  // Set a buffered entity based request interceptor
  private final HttpRequestInterceptor bufferedEntityInterceptor = (request, context) -> {
    if (request instanceof HttpEntityEnclosingRequest) {
      final HttpEntityEnclosingRequest enclosingRequest = ((HttpEntityEnclosingRequest) request);
      final HttpEntity requestEntity = enclosingRequest.getEntity();
      enclosingRequest.setEntity(new BufferedHttpEntity(requestEntity));
    }
  };

  public static class SolrJaasConfiguration extends javax.security.auth.login.Configuration {

    private javax.security.auth.login.Configuration baseConfig;

    // the com.sun.security.jgss appNames
    private final Set<String> initiateAppNames = new HashSet<>(Arrays.asList("com.sun.security.jgss.krb5.initiate", "com.sun.security.jgss.initiate"));

    public SolrJaasConfiguration() {
      try {

        this.baseConfig = javax.security.auth.login.Configuration.getConfiguration();
      } catch (final SecurityException e) {
        this.baseConfig = null;
      }
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(final String appName) {
      if (baseConfig == null)
        return null;

      if (log.isDebugEnabled()) {
        log.debug("Login prop: {}", System.getProperty(LOGIN_CONFIG_PROP));
      }

      final String clientAppName = System.getProperty("solr.kerberos.jaas.appname", "Client");
      if (initiateAppNames.contains(appName)) {
        log.debug("Using AppConfigurationEntry for appName '{}' instead of: '{}'", clientAppName, appName);
        return baseConfig.getAppConfigurationEntry(clientAppName);
      }
      return baseConfig.getAppConfigurationEntry(appName);
    }
  }
}
