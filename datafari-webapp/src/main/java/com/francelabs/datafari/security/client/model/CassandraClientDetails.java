package com.francelabs.datafari.security.client.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraClientDetails.TABLE)
public class CassandraClientDetails {

  public static final String TABLE = "oauth_clients";

  @PrimaryKey
  private final String clientId;
  private final String clientSecret;
  private final Set<String> resourceIds = new HashSet<>();
  private final Set<String> scope = new HashSet<>();
  private final Set<String> authorizedGrantTypes = new HashSet<>();
  private final Set<String> registeredRedirectUri = new HashSet<>();
  private final Set<String> authorities = new HashSet<>();
  private final int accessTokenValiditySeconds;
  private final int refreshTokenValiditySeconds;

  public CassandraClientDetails(final String clientId, final String clientSecret, final Set<String> resourceIds, final Set<String> scope, final Set<String> authorizedGrantTypes,
      final Set<String> registeredRedirectUri, final Set<String> authorities, final int accessTokenValiditySeconds, final int refreshTokenValiditySeconds) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    if (resourceIds != null) {
      this.resourceIds.addAll(resourceIds);
    }
    if (scope != null) {
      this.scope.addAll(scope);
    }
    if (authorizedGrantTypes != null) {
      this.authorizedGrantTypes.addAll(authorizedGrantTypes);
    }
    if (registeredRedirectUri != null) {
      this.registeredRedirectUri.addAll(registeredRedirectUri);
    }
    if (authorities != null) {
      this.authorities.addAll(authorities);
    }
    this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public Set<String> getResourceIds() {
    return resourceIds;
  }

  public Set<String> getScope() {
    return scope;
  }

  public Set<String> getAuthorizedGrantTypes() {
    return authorizedGrantTypes;
  }

  public Set<String> getRegisteredRedirectUri() {
    return registeredRedirectUri;
  }

  public Set<String> getAuthorities() {
    return authorities;
  }

  public int getAccessTokenValiditySeconds() {
    return accessTokenValiditySeconds;
  }

  public int getRefreshTokenValiditySeconds() {
    return refreshTokenValiditySeconds;
  }

}
