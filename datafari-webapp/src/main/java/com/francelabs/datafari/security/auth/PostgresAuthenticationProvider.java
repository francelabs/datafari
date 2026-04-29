package com.francelabs.datafari.security.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Authentication provider responsible for the legacy local username/password
 * authentication backed by the Datafari PostgreSQL user base.
 *
 * <p>This provider validates the submitted password against the legacy SHA-256
 * password hash stored in the local database. Once the password is validated,
 * the provider loads the local Datafari authorities for the authenticated user
 * through {@link DatafariLocalUserService} and returns a standard authenticated
 * {@link UsernamePasswordAuthenticationToken}.</p>
 *
 * <p>This provider only handles the standard Datafari local authentication flow.
 * External mechanisms such as SAML, CAS, Kerberos or trusted headers reuse the
 * local authority resolution logic through other dedicated components.</p>
 */
public class PostgresAuthenticationProvider implements AuthenticationProvider {

  private final DatafariLocalUserService localUserService;

  public PostgresAuthenticationProvider(DatafariLocalUserService localUserService) {
    this.localUserService = localUserService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (authentication.getName() == null || authentication.getCredentials() == null) {
      return null;
    }

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    String dbPassword = localUserService.getLegacySha256Password(username);
    if (!StringUtils.hasText(dbPassword)) {
      return null;
    }

    String digestPassword = digest(password);
    if (!StringUtils.hasText(digestPassword) || !digestPassword.equals(dbPassword)) {
      throw new BadCredentialsException("Authentication failed for " + username);
    }

    return UsernamePasswordAuthenticationToken.authenticated(
        username,
        null,
        localUserService.getGrantedAuthorities(username)
    );
  }

  /**
   * Computes the legacy SHA-256 hexadecimal digest used by the local Datafari
   * password store.
   *
   * @param password the raw submitted password
   * @return the SHA-256 hexadecimal digest, or {@code null} if digest
   *         computation fails
   */
  protected String digest(String password) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
      return HexUtils.convert(digest);
    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.equals(UsernamePasswordAuthenticationToken.class);
  }
}