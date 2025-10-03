package com.francelabs.datafari.security.client.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Transient;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "oauth_clients")
public class PgsqlClientDetails {

  @Id
  @Column(name = "client_id")
  private String clientId;

  @Column(name = "client_secret")
  private String clientSecret;

  @Column(name = "access_token_validity_seconds")
  private int accessTokenValiditySeconds;

  @Column(name = "refresh_token_validity_seconds")
  private int refreshTokenValiditySeconds;

  // Conservés mais pas gérés par JPA : ignorés au mapping
  @Transient
  private Set<String> resourceIds = new HashSet<>();

  @Transient
  private Set<String> scope = new HashSet<>();

  @Transient
  private Set<String> authorizedGrantTypes = new HashSet<>();

  @Transient
  private Set<String> registeredRedirectUri = new HashSet<>();

  @Transient
  private Set<String> authorities = new HashSet<>();

  public PgsqlClientDetails() {}

  public PgsqlClientDetails(String clientId, String clientSecret, Set<String> resourceIds, Set<String> scope,
                            Set<String> authorizedGrantTypes, Set<String> registeredRedirectUri, Set<String> authorities,
                            int accessTokenValiditySeconds, int refreshTokenValiditySeconds) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    if (resourceIds != null) this.resourceIds.addAll(resourceIds);
    if (scope != null) this.scope.addAll(scope);
    if (authorizedGrantTypes != null) this.authorizedGrantTypes.addAll(authorizedGrantTypes);
    if (registeredRedirectUri != null) this.registeredRedirectUri.addAll(registeredRedirectUri);
    if (authorities != null) this.authorities.addAll(authorities);
    this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
  }

  // Getters et setters (comme tu les avais)


  // --- Getters/Setters ---

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public Set<String> getResourceIds() {
    return resourceIds;
  }

  public void setResourceIds(Set<String> resourceIds) {
    this.resourceIds = resourceIds;
  }

  public Set<String> getScope() {
    return scope;
  }

  public void setScope(Set<String> scope) {
    this.scope = scope;
  }

  public Set<String> getAuthorizedGrantTypes() {
    return authorizedGrantTypes;
  }

  public void setAuthorizedGrantTypes(Set<String> authorizedGrantTypes) {
    this.authorizedGrantTypes = authorizedGrantTypes;
  }

  public Set<String> getRegisteredRedirectUri() {
    return registeredRedirectUri;
  }

  public void setRegisteredRedirectUri(Set<String> registeredRedirectUri) {
    this.registeredRedirectUri = registeredRedirectUri;
  }

  public Set<String> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<String> authorities) {
    this.authorities = authorities;
  }

  public int getAccessTokenValiditySeconds() {
    return accessTokenValiditySeconds;
  }

  public void setAccessTokenValiditySeconds(int accessTokenValiditySeconds) {
    this.accessTokenValiditySeconds = accessTokenValiditySeconds;
  }

  public int getRefreshTokenValiditySeconds() {
    return refreshTokenValiditySeconds;
  }

  public void setRefreshTokenValiditySeconds(int refreshTokenValiditySeconds) {
    this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
  }
}