package com.francelabs.datafari.aggregator.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.francelabs.datafari.utils.HttpClientProvider;

public class SearchAggregatorAccessTokenManager {

  private static SearchAggregatorAccessTokenManager instance = null;

  private final Map<String, SearchAggregatorAccessToken> tokens;

  private static final Logger LOGGER = LogManager.getLogger(SearchAggregatorAccessTokenManager.class.getName());

  private SearchAggregatorAccessTokenManager() {
    tokens = new HashMap<String, SearchAggregatorAccessToken>();

  }

  public static SearchAggregatorAccessTokenManager getInstance() {
    if (instance == null) {
      instance = new SearchAggregatorAccessTokenManager();
    }
    return instance;
  }

  public String getAccessToken(final String tokenUri, final String searchAggregatorSecret) throws IOException {
    final String key = tokenUri + "||" + searchAggregatorSecret;
    String accessToken = "";
    if (tokens.containsKey(key)) {
      final SearchAggregatorAccessToken token = tokens.get(key);
      if (token.getTimeout() >= System.currentTimeMillis()) {
        accessToken = token.getAccessToken();
      } else {
        tokens.remove(key);
        final SearchAggregatorAccessToken newToken = requestNewToken(tokenUri, searchAggregatorSecret);
        accessToken = newToken.getAccessToken();
        tokens.put(key, newToken);
      }
    } else {
      final SearchAggregatorAccessToken newToken = requestNewToken(tokenUri, searchAggregatorSecret);
      accessToken = newToken.getAccessToken();
      tokens.put(key, newToken);
    }
    return accessToken;
  }

  private SearchAggregatorAccessToken requestNewToken(final String tokenUri, final String searchAggregatorSecret) throws IOException {
    final CloseableHttpClient client = HttpClientProvider.getInstance().newClient();
    final HttpPost postReq = new HttpPost(tokenUri);
    final String auth = "search-aggregator:" + searchAggregatorSecret;
    final byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
    final String authHeader = "Basic " + new String(encodedAuth);
    postReq.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
    final HttpEntity entity = EntityBuilder.create().setText("grant_type=client_credentials").build();
    postReq.setEntity(entity);
    postReq.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

    try (final CloseableHttpResponse response = client.execute(postReq);) {
      if (response.getStatusLine().getStatusCode() == 200) {
        final String jsonToken = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        final JSONParser parser = new JSONParser();
        final JSONObject tokenInfos = (JSONObject) parser.parse(jsonToken);
        final String accessToken = tokenInfos.get("access_token").toString();
        final long expires = (Long) tokenInfos.get("expires_in");
        // Deduce timeout = current time milli - 1s (to be safe) + expires in milliseconds
        final long timeout = System.currentTimeMillis() - 1000 + expires * 1000;
        return new SearchAggregatorAccessToken(accessToken, timeout);
      } else {
        LOGGER.error("Error " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase() + " while requesting access token to URL " + tokenUri);
        throw new IOException("Error " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase() + " while requesting access token to URL " + tokenUri);
      }
    } catch (final Exception e) {
      LOGGER.error("Error requesting access token to URL " + tokenUri, e);
      throw new IOException("Error requesting access token to URL " + tokenUri, e);
    } finally {
      client.close();
    }
  }

}
