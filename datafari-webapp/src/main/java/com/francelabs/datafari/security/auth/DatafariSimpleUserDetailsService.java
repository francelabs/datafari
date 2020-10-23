package com.francelabs.datafari.security.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class DatafariSimpleUserDetailsService implements UserDetailsService {

  @Override
  public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
    final Collection<GrantedAuthority> authorities = new ArrayList<>();
    final List<String> cassandraRoles = CassandraAuthenticationProvider.getRoles(username);
    cassandraRoles.forEach(role -> {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    });
    return new User(username, "notUsed", true, true, true, true, authorities);
  }

}
