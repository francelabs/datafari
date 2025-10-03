package com.francelabs.datafari.security.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.service.db.UserDataTTLService;
import com.francelabs.datafari.user.User;

public class PostgresAuthenticationProvider implements AuthenticationProvider {

  private static final String USERNAMECOLUMN = "username";
  private static final String PASSWORDCOLUMN = "password";

  @Override
  public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
    if (authentication.getName() == null || authentication.getCredentials() == null) {
      return null;
    }
    final String username = authentication.getName();
    final String password = authentication.getCredentials().toString();

    final String dbPassword = getPassword(username);
    if (dbPassword != null && !dbPassword.isEmpty()) {
      final String digestPassword = digest(password);
      if (digestPassword.contentEquals(dbPassword)) {
        final List<String> roles = getRoles(username);
        final List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        roles.forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        return new UsernamePasswordAuthenticationToken(username, dbPassword, grantedAuthorities);
      } else {
        throw new BadCredentialsException("Authentication failed for " + username);
      }
    } else {
      return null;
    }
  }

  private String getPassword(final String username) {
    try {
      // Utilisation du UserDataServicePostgres
      return UserDataService.getInstance().getPassword(username);
    } catch (Exception e) {
      // Log à ajouter si besoin
      return null;
    }
  }

  public static List<GrantedAuthority> getGrantedAuthorities(final String username) {
    final List<String> roles = getRoles(username);
    final List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
    roles.forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
    return grantedAuthorities;
  }

  private static List<String> getRoles(final String username) {
    String rawUsername = username.toLowerCase();
    if (rawUsername.contains("@")) {
      rawUsername = rawUsername.substring(0, rawUsername.indexOf("@"));
    }
    final List<String> roles = new ArrayList<>();

    try {
      if (UserDataService.getInstance().isInBase(rawUsername)) {
        roles.addAll(UserDataService.getInstance().getRoles(rawUsername));
        // Refresh the user data TTL
        if (!rawUsername.contentEquals("admin")) {
          UserDataTTLService.refreshUserDataTTL(rawUsername); // Peut être à ajuster pour Postgres !
        }
      } else {
        // First time the user logs into Datafari, add it the default role "ConnectedSearchUser"
        final User newUser = new User(rawUsername, "", true);
        newUser.signup("ConnectedSearchUser");
        roles.add("ConnectedSearchUser");
      }
    } catch (final DatafariServerException e) {
      // Log à ajouter si besoin
    }
    return roles;
  }

  protected String digest(final String password) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] digest = md.digest(password.getBytes("UTF-8"));
      return HexUtils.convert(digest);
    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  public boolean supports(final Class<?> authentication) {
    return authentication.equals(UsernamePasswordAuthenticationToken.class);
  }
}