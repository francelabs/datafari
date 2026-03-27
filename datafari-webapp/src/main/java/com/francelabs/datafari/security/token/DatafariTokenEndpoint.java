package com.francelabs.datafari.security.token;

import com.francelabs.datafari.security.token.DatafariClientAuthenticator;
import com.francelabs.datafari.security.token.DatafariTokenService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint to get an access token to the Datafari Authorization Server. It uses a legacy grant type of credentials: the login/password grant.  
 */
@RestController
public class DatafariTokenEndpoint {
  private static final Logger LOGGER = LogManager.getLogger(DatafariTokenEndpoint.class);
  private final AuthenticationManager authenticationManager;
  private final DatafariClientAuthenticator clientAuthenticator;
  private final DatafariTokenService tokenService;

  public DatafariTokenEndpoint(AuthenticationManager authenticationManager,
                               DatafariClientAuthenticator clientAuthenticator,
                               DatafariTokenService tokenService) {
    this.authenticationManager = authenticationManager;
    this.clientAuthenticator = clientAuthenticator;
    this.tokenService = tokenService;
  }

  @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<?> token(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestParam("grant_type") String grantType,
      @RequestParam("username") String username,
      @RequestParam("password") String password) {

    LOGGER.debug("'/oauth/token' request reached");
    clientAuthenticator.checkBasicClientCredentials(authorization);
    LOGGER.debug("Good credential found");

    if (!"password".equals(grantType)) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "unsupported_grant_type"
      ));
    }

    Authentication authRequest =
        UsernamePasswordAuthenticationToken.unauthenticated(username, password);

    Authentication authentication = authenticationManager.authenticate(authRequest);

    LegacyAccessToken token = tokenService.issueToken(authentication, "datafari-client");

    return ResponseEntity.ok(Map.of(
        "access_token", token.value(),
        "token_type", "bearer",
        "expires_in", token.expiresIn()
    ));
  }
}