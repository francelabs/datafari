package com.francelabs.datafari.security.auth;

import java.util.Collection;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

/**
 * Replace the DefaultLdapAuthoritiesPopulator so that the authorities are replaced by the Datafari roles
 *
 */
public class DatafariLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

  /**
   * Retrieves the Datafari roles from the provided username and set them as the GrantedAuthorities collection
   */
  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(final DirContextOperations userData, final String username) {
    return CassandraAuthenticationProvider.getGrantedAuthorities(username);
  }

}
