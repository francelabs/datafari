package com.francelabs.datafari.security.auth;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.service.db.UserDataTTLService;
import com.francelabs.datafari.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for Datafari local user lookup, role resolution and
 * first-login provisioning.
 *
 * <p>This service centralizes the local user logic shared by multiple security
 * components:
 * </p>
 * <ul>
 *   <li>loading the legacy SHA-256 password used by the standard local login,</li>
 *   <li>resolving local Datafari roles for an already identified user,</li>
 *   <li>creating a local Datafari user on first login when needed,</li>
 *   <li>refreshing local user-related TTL data for existing users.</li>
 * </ul>
 *
 * <p>By extracting this logic into a dedicated service, authentication providers
 * and {@code UserDetailsService} implementations can focus on their own
 * responsibilities while reusing a single source of truth for local user and
 * authority management.</p>
 */
@Service
public class DatafariLocalUserService {

  private static final Logger LOGGER = LogManager.getLogger(DatafariLocalUserService.class);

  private static final String DEFAULT_ROLE = "ConnectedSearchUser";
  private static final String ADMIN_USERNAME = "admin";

  private final UserDataService userDataService;

  public DatafariLocalUserService(UserDataService userDataService) {
    this.userDataService = userDataService;
  }

  /**
   * Returns the legacy SHA-256 password hash stored for the given username.
   *
   * <p>This method is intended for the legacy username/password authentication
   * flow still used by the standard Datafari login.</p>
   *
   * @param username the username to look up
   * @return the stored SHA-256 password hash, or {@code null} if no password is found
   */
  public String getLegacySha256Password(String username) {
    try {
      return userDataService.getPassword(username);
    } catch (DatafariServerException e) {
      LOGGER.warn("Unable to retrieve legacy SHA-256 password for user '{}': {}", username, e.getMessage());
      return null;
    }
  }

  /**
   * Returns the granted authorities associated with the given user in the
   * Datafari local user base.
   *
   * <p>If the user does not yet exist locally, a new local account is created
   * with the default role {@value #DEFAULT_ROLE}.</p>
   *
   * @param username the incoming username
   * @return the corresponding granted authorities, never {@code null}
   */
  public List<GrantedAuthority> getGrantedAuthorities(String username) {
    List<String> roles = getOrProvisionRoles(username);
    List<GrantedAuthority> grantedAuthorities = new ArrayList<>(roles.size());
    for (String role : roles) {
      grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    }
    return grantedAuthorities;
  }

  /**
   * Normalizes a username for local Datafari user lookup.
   *
   * <p>The current normalization strategy converts the username to lowercase
   * and removes the domain suffix when the username is expressed as an email
   * address.</p>
   *
   * @param username the incoming username
   * @return the normalized username
   */
  public String normalizeUsername(String username) {
    if (!StringUtils.hasText(username)) {
      return username;
    }

    String normalized = username.toLowerCase();
    int atIndex = normalized.indexOf('@');
    if (atIndex >= 0) {
      normalized = normalized.substring(0, atIndex);
    }
    return normalized;
  }

  /**
   * Resolves the local roles for the given user and provisions a local user if
   * needed.
   *
   * <p>For existing users, this method also refreshes the local TTL-managed user
   * data, except for the built-in {@code admin} account.</p>
   *
   * @param username the incoming username
   * @return the resolved role names, never {@code null}
   */
  private List<String> getOrProvisionRoles(String username) {
    String normalizedUsername = normalizeUsername(username);
    List<String> roles = new ArrayList<>();

    try {
      if (userDataService.isInBase(normalizedUsername)) {
        roles.addAll(userDataService.getRoles(normalizedUsername));

        if (!ADMIN_USERNAME.equals(normalizedUsername)) {
          UserDataTTLService.refreshUserDataTTL(normalizedUsername);
        }
      } else {
        LOGGER.info("First local login for user '{}', provisioning default Datafari account", normalizedUsername);
        User newUser = new User(normalizedUsername, "", true);
        newUser.signup(DEFAULT_ROLE);
        roles.add(DEFAULT_ROLE);
      }
    } catch (DatafariServerException e) {
      LOGGER.warn("Unable to resolve local roles for user '{}': {}", normalizedUsername, e.getMessage());
    }

    return roles;
  }
}