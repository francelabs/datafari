package com.francelabs.datafari.utils;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpClientProvider {

  private static HttpClientProvider instance = null;

  private static final Logger LOGGER = LogManager.getLogger(HttpClientProvider.class.getName());

  private static final int connectionTimeout = 60000;
  private static final int socketTimeout = 60000;

  SSLConnectionSocketFactory sslsf;
  RequestConfig.Builder requestBuilder;

  private HttpClientProvider() {
    // Strategy to trust any certificate
    final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
    sslsf = SSLConnectionSocketFactory.getSocketFactory();
    try {
      final SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
      sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
      LOGGER.error("Unable to enable 'trust any cert' strategy", e);
    }

//    final BasicHttpClientConnectionManager poolingConnectionManager = new BasicHttpClientConnectionManager(
//        RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslsf).build());

    requestBuilder = RequestConfig.custom().setCircularRedirectsAllowed(true).setSocketTimeout(socketTimeout).setExpectContinueEnabled(false).setConnectTimeout(connectionTimeout)
        .setConnectionRequestTimeout(socketTimeout);

  }

  public static HttpClientProvider getInstance() {
    if (instance == null) {
      instance = new HttpClientProvider();
    }
    return instance;
  }

  /**
   *
   * @return {@link CloseableHttpClient} with default connection timeout and socket timeout
   */
  public CloseableHttpClient newClient() {
    final HttpClientBuilder builder = HttpClients.custom().setSSLSocketFactory(sslsf).disableAutomaticRetries().setDefaultRequestConfig(requestBuilder.build());
    builder.setRequestExecutor(new HttpRequestExecutor(socketTimeout)).setRedirectStrategy(new DefaultRedirectStrategy());
    return builder.build();
  }

  /**
   *
   * @return {@link CloseableHttpClient} with provided connection timeout and socket timeout
   */
  public CloseableHttpClient newClient(final int connectionTimeout, final int socketTimeout) {
    final Builder rb = RequestConfig.custom().setCircularRedirectsAllowed(true).setSocketTimeout(socketTimeout).setExpectContinueEnabled(false).setConnectTimeout(connectionTimeout)
        .setConnectionRequestTimeout(socketTimeout);
    final HttpClientBuilder builder = HttpClients.custom().setSSLSocketFactory(sslsf).disableAutomaticRetries().setDefaultRequestConfig(rb.build());
    builder.setRequestExecutor(new HttpRequestExecutor(socketTimeout)).setRedirectStrategy(new DefaultRedirectStrategy());
    return builder.build();

  }

}
