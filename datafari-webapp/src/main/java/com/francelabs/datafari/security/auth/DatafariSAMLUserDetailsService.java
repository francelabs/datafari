package com.francelabs.datafari.security.auth;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
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

    final Collection<GrantedAuthority> authorities = PostgresAuthenticationProvider.getGrantedAuthorities(username);

    // In a real scenario, this implementation has to locate user in a arbitrary
    // dataStore based on information present in the SAMLCredential and
    // returns such a date in a form of application specific UserDetails object.
    return new User(username, "notUsed", true, true, true, true, authorities);
  }

}
