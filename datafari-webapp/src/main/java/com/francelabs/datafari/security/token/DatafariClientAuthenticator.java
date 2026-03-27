package com.francelabs.datafari.security.token;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class DatafariClientAuthenticator {
  private static final Logger LOGGER = LogManager.getLogger(DatafariClientAuthenticator.class);

  @Value("${datafari.oauth.client-id}")
  private String expectedClientId;

  @Value("${datafari.oauth.client-secret}")
  private String expectedClientSecret;

  public void checkBasicClientCredentials(String authorizationHeader) {
    LOGGER.debug("checkBasicClientCredentials: in function ");
    if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
      throw new BadCredentialsException("Missing client credentials");
    }

    String base64 = authorizationHeader.substring("Basic ".length());
    String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    String[] parts = decoded.split(":", 2);

    String clientId = parts.length > 0 ? parts[0] : "";
    String clientSecret = parts.length > 1 ? parts[1] : "";
    LOGGER.debug("clientId: {} - clientSecret: {}", clientId, clientSecret);

    if (!expectedClientId.equals(clientId) || !expectedClientSecret.equals(clientSecret)) {
      throw new BadCredentialsException("Invalid client credentials");
    }
  }
}