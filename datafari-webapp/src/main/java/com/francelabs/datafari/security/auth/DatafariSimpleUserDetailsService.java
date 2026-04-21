package com.francelabs.datafari.security.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Simple {@link UserDetailsService} used to expose a Datafari local user with
 * its authorities to Spring Security once the user identity has already been
 * established by another mechanism.
 *
 * <p>This service is mainly used by authentication mechanisms that trust an
 * external identity source, such as SAML, CAS, Kerberos or custom header-based
 * authentication. In those cases, the external system validates the identity,
 * while Datafari still needs to load or provision the local user and resolve
 * its local authorities.</p>
 */
@Service
public class DatafariSimpleUserDetailsService implements UserDetailsService {

  private final DatafariLocalUserService localUserService;

  public DatafariSimpleUserDetailsService(DatafariLocalUserService localUserService) {
    this.localUserService = localUserService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    return new User(
        username,
        "notUsed",
        true,
        true,
        true,
        true,
        localUserService.getGrantedAuthorities(username)
    );
  }
}