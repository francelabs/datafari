package com.francelabs.datafari.security.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.CassandraManager;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.service.db.UserDataTTLService;

public class CassandraAuthenticationProvider implements AuthenticationProvider {

//db constants
  private final static String USERCOLLECTION = "user";
  private final static String ROLECOLLECTION = "role";

  private static final String USERNAMECOLUMN = "username";
  private final static String PASSWORDCOLUMN = "password";
  private final static String ROLECOLUMN = "role";

  final static Logger logger = LogManager.getLogger(CassandraAuthenticationProvider.class.getName());

  @Override
  public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
    final String username = authentication.getName();
    final String password = authentication.getCredentials().toString();

    final String dbPassword = getPassword(username);
    if (dbPassword != null && !dbPassword.isEmpty()) {
      final String digestPassword = digest(password);
      if (digestPassword.contentEquals(dbPassword)) {
        final List<String> roles = getRoles(username);
        final List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        roles.forEach(role -> {
          grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return new UsernamePasswordAuthenticationToken(username, dbPassword, grantedAuthorities);
      } else {
        throw new BadCredentialsException("Authentication failed for " + username);
      }
    } else {
      return null;
    }
  }

  private String getPassword(final String username) {
    final ResultSet results = CassandraManager.getInstance().getSession().execute("SELECT * FROM " + USERCOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "'");
    final Row entry = results.one();
    if (entry == null) {
      return null;
    } else {
      return entry.getString(PASSWORDCOLUMN);
    }
  }

  protected static List<String> getRoles(final String username) {
    String rawUsername = username;
    if (username.contains("@")) {
      rawUsername = username.substring(0, username.indexOf("@"));
    }
    final List<String> roles = new ArrayList<>();

    try {
      if (UserDataService.getInstance().isInBase(username)) {
        roles.addAll(UserDataService.getInstance().getRoles(rawUsername));

        // Refresh the user data TTL
        if (!username.contentEquals("admin")) {
          UserDataTTLService.refreshUserDataTTL(username);
        }
      }
    } catch (final DatafariServerException e) {
      logger.error("Unable to perform request on Cassandra", e);
    }
    return roles;
  }

  /**
   * Digest the password using the specified algorithm and convert the result to a corresponding hexadecimal string. If exception, the plain
   * credentials string is returned.
   *
   * @param credentials
   *          Password or other credentials to use in authenticating this username
   */
  protected String digest(final String password) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] digest = md.digest(password.getBytes("UTF-8"));
      return HexUtils.convert(digest);
    } catch (final UnsupportedEncodingException ex) {
      return null;

    } catch (final NoSuchAlgorithmException ex) {
      return null;

    }
  }

  @Override
  public boolean supports(final Class<?> authentication) {
    return authentication.equals(UsernamePasswordAuthenticationToken.class);
  }

}
