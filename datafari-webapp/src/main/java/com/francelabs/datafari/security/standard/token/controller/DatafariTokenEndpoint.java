package com.francelabs.datafari.security.standard.token.controller;

import com.francelabs.datafari.security.standard.token.DatafariClientAuthenticator;
import com.francelabs.datafari.security.standard.token.DatafariOAuthProperties;
import com.francelabs.datafari.security.standard.token.service.DatafariTokenService;
import com.francelabs.datafari.security.standard.token.service.LegacyAccessToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Legacy endpoint used to issue a Datafari access token from client
 * credentials and user credentials.
 *
 * <p>This controller restores the historical {@code /oauth/token} behavior
 * expected by existing Datafari clients after the migration away from the
 * former Spring Security OAuth implementation.</p>
 *
 * <p>The endpoint accepts a {@code POST} request with
 * {@code application/x-www-form-urlencoded} parameters and currently supports
 * the legacy {@code password} grant style for compatibility purposes.</p>
 *
 * <p>Token issuance is performed in three steps:</p>
 * <ol>
 *   <li>validate the client credentials received through the
 *   {@code Authorization: Basic ...} header,</li>
 *   <li>authenticate the end user through the shared
 *   {@link org.springframework.security.authentication.AuthenticationManager},</li>
 *   <li>issue and persist a new access token through
 *   {@link DatafariTokenService}.</li>
 * </ol>
 *
 * <p>This endpoint is intentionally legacy-oriented and should not be confused
 * with a full OAuth2 Authorization Server implementation. Its purpose is to
 * preserve backward compatibility for Datafari clients that historically relied
 * on {@code /oauth/token}.</p>
 */
@RestController
@ConditionalOnExpression("${oidc.enabled:false}==false && ${saml.enabled:false}==false && ${keycloak.enabled:false}==false && ${kerberos.enabled:false}==false && ${cas.enabled:false}==false && ${header.enabled:false}==false")
public class DatafariTokenEndpoint {
  private static final Logger LOGGER = LogManager.getLogger(DatafariTokenEndpoint.class);
  private final AuthenticationManager authenticationManager;
  private final DatafariClientAuthenticator clientAuthenticator;
  private final DatafariTokenService tokenService;

  private String tokenClientId;

  /**
   * Creates the legacy token endpoint.
   *
   * @param authenticationManager authentication manager used to authenticate
   *                              the end user's username/password
   * @param clientAuthenticator component responsible for validating client
   *                            credentials provided through HTTP Basic
   * @param tokenService service responsible for issuing and storing access tokens
   * @param oAuthProperties configuration properties used to get the client id to issue a new access token.
   */
  public DatafariTokenEndpoint(AuthenticationManager authenticationManager,
                               DatafariClientAuthenticator clientAuthenticator,
                               DatafariTokenService tokenService,
                               DatafariOAuthProperties oAuthProperties) {
    this.authenticationManager = authenticationManager;
    this.clientAuthenticator = clientAuthenticator;
    this.tokenService = tokenService;
    this.tokenClientId = oAuthProperties.getClientId();
  }

  /**
   * Issues a new access token for a client and a user.
   *
   * <p>The request must provide:</p>
   * <ul>
   *   <li>a valid {@code Authorization: Basic ...} header representing the client,</li>
   *   <li>the form parameter {@code grant_type=password},</li>
   *   <li>the user credentials through {@code username} and {@code password}.</li>
   * </ul>
   *
   * <p>If the client credentials are invalid, or if the user cannot be
   * authenticated, the request fails with a Spring Security authentication
   * exception. If the grant type is not supported, the method returns a
   * {@code 400 Bad Request} response containing an OAuth-like error payload.</p>
   *
   * <p>On success, the response contains the generated bearer token, its type
   * and its lifetime in seconds.</p>
   *
   * @param authorization value of the {@code Authorization} header containing
   *                      the client credentials encoded with HTTP Basic
   * @param grantType requested grant type, currently expected to be {@code password}
   * @param username end user's login
   * @param password end user's password
   * @return an HTTP response containing the issued access token information
   */
  @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<?> token(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestParam("grant_type") String grantType,
      @RequestParam("username") String username,
      @RequestParam("password") String password) {

    LOGGER.debug("'/oauth/token' request reached");
    try {
      clientAuthenticator.checkBasicClientCredentials(authorization);
      LOGGER.debug("Good credential found");

      if (!"password".equals(grantType)) {
        return ResponseEntity.badRequest().body(Map.of(
              "error", "unsupported_grant_type",
              "error_description", "Only grant_type=password is supported"
        ));
      }

      Authentication authRequest =
          UsernamePasswordAuthenticationToken.unauthenticated(username, password);

      Authentication authentication = authenticationManager.authenticate(authRequest);

      LegacyAccessToken token = tokenService.issueToken(authentication, tokenClientId);

      return ResponseEntity.ok(Map.of(
          "access_token", token.value(),
          "token_type", "bearer",
          "expires_in", token.expiresIn()
      ));
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
          "error", "invalid_client_or_user",
          "error_description", e.getMessage()
      ));
    }
  }
}