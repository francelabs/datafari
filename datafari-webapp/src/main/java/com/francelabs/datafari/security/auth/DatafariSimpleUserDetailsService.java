package com.francelabs.datafari.security.auth;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatafariSimpleUserDetailsService implements UserDetailsService {

  @Override
  public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
    final Collection<GrantedAuthority> authorities = PostgresAuthenticationProvider.getGrantedAuthorities(username);
    return new User(username, "notUsed", true, true, true, true, authorities);
  }

}
