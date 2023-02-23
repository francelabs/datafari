package com.francelabs.datafari.security.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class DatafariSAMLUserDetailsService implements SAMLUserDetailsService {

  @Override
  public Object loadUserBySAML(final SAMLCredential credential) throws UsernameNotFoundException {
    final String username = credential.getNameID().getValue();

    final Collection<GrantedAuthority> authorities = new ArrayList<>();
    final List<String> cassandraRoles = CassandraAuthenticationProvider.getRoles(username);
    cassandraRoles.forEach(role -> {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    });

    // In a real scenario, this implementation has to locate user in a arbitrary
    // dataStore based on information present in the SAMLCredential and
    // returns such a date in a form of application specific UserDetails object.
    return new User(username, "notUsed", true, true, true, true, authorities);
  }

}
