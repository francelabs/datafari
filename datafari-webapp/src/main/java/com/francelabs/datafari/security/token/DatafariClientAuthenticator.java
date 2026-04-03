package com.francelabs.datafari.security.token;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Component responsible for validating client credentials sent to the legacy
 * token endpoint.
 *
 * <p>This authenticator expects the client to provide credentials through an
 * {@code Authorization} header using the HTTP Basic scheme. The header is
 * decoded and compared against the configured Datafari client identifier and
 * client secret.</p>
 *
 * <p>This validation step protects the legacy {@code /oauth/token} endpoint by
 * ensuring that only known clients are allowed to request an access token on
 * behalf of a user.</p>
 *
 * <p>The expected client identifier and secret are provided through the
 * application configuration properties:
 * {@code datafari.oauth.client-id} and
 * {@code datafari.oauth.client-secret}.</p>
 */
@Component
public class DatafariClientAuthenticator {
  private static final Logger LOGGER = LogManager.getLogger(DatafariClientAuthenticator.class);

  @Autowired
  private DatafariOAuthProperties oAuthProperties;

  /**
   * Validates the client credentials contained in an HTTP Basic
   * {@code Authorization} header.
   *
   * <p>The header must follow the format {@code Basic base64(clientId:secret)}.
   * The decoded values are compared with the configured expected client
   * identifier and client secret.</p>
   *
   * <p>If the header is missing, malformed or does not match the configured
   * client credentials, a {@link org.springframework.security.authentication.BadCredentialsException}
   * is thrown.</p>
   *
   * @param authorizationHeader raw {@code Authorization} header value
   * @throws org.springframework.security.authentication.BadCredentialsException
   *         if the client credentials are missing or invalid
   */
  public void checkBasicClientCredentials(String authorizationHeader) {
    LOGGER.debug("checkBasicClientCredentials: in function ");
    if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
      throw new BadCredentialsException("Missing client credentials");
    }

    final String decoded;
    try {
      String base64 = authorizationHeader.substring("Basic ".length());
      decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new BadCredentialsException("Invalid Basic authentication header", e);
    }

    String[] parts = decoded.split(":", 2);

    String clientId = parts.length > 0 ? parts[0] : "";
    String clientSecret = parts.length > 1 ? parts[1] : "";
    LOGGER.debug("clientId: {} - clientSecret: {}", clientId, clientSecret);

    if (StringUtils.isEmpty(oAuthProperties.getClientId()) || DatafariOAuthProperties.INVALID_CLIENT_ID.equals(oAuthProperties.getClientId())){
      throw new IllegalStateException("This feature is not available in Community version of Datafari");
    }

    if (!oAuthProperties.getClientId().equals(clientId) || !oAuthProperties.getClientSecret().equals(clientSecret)) {
      throw new BadCredentialsException("Invalid client credentials");
    }
  }
}