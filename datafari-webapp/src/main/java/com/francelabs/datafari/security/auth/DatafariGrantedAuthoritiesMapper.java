package com.francelabs.datafari.security.auth;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

public class DatafariGrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

  private static final Logger LOGGER = LogManager.getLogger(DatafariGrantedAuthoritiesMapper.class.getName());

  private final String usernameAttr;

  public DatafariGrantedAuthoritiesMapper(final String usernameAttr) {
    this.usernameAttr = usernameAttr;
  }

  @Override
  public Collection<? extends GrantedAuthority> mapAuthorities(final Collection<? extends GrantedAuthority> authorities) {
    final Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

    authorities.forEach(authority -> {
      // We add the original authority
      mappedAuthorities.add(authority);

      // Determine the authority type to extract the username with the right method
      if (OidcUserAuthority.class.isInstance(authority)) {
        final OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;

        final Map<String, Object> userAttributes = oidcUserAuthority.getAttributes();
        String username;
        if (userAttributes.get(usernameAttr) != null) {
          username = userAttributes.get(usernameAttr).toString();
        } else if (oidcUserAuthority.getUserInfo() != null) {
          username = oidcUserAuthority.getUserInfo().getPreferredUsername();
        } else {
          // Unable to determine username so return
          LOGGER.warn("Unable to determine the username to gather ROLES. Username attribute '" + usernameAttr + "' was not found, neither a default username");
          return;
        }
        // Retrieve Datafari rights
        final List<String> cassandraRoles = CassandraAuthenticationProvider.getRoles(username);
        cassandraRoles.forEach(role -> {
          mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        });

      } else if (OAuth2UserAuthority.class.isInstance(authority)) {
        final OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;

        final Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
        String username;
        if (userAttributes.get(usernameAttr) != null) {
          username = userAttributes.get(usernameAttr).toString();
        } else {
          // Unable to determine username so return
          LOGGER.warn("Unable to determine the username to gather ROLES. Username attribute '" + usernameAttr + "' was not found !");
          return;
        }
        // Retrieve Datafari rights
        final List<String> cassandraRoles = CassandraAuthenticationProvider.getRoles(username);
        cassandraRoles.forEach(role -> {
          mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        });

      }
    });

    return mappedAuthorities;
  }

}
