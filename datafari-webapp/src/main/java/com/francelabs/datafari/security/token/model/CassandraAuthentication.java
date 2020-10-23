package com.francelabs.datafari.security.token.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraAuthentication.TABLE)
public class CassandraAuthentication {

  public static final String TABLE = "oauth_authentications";

  @PrimaryKey
  private final String accessTokenId;
  private final String authentication;

  public CassandraAuthentication(final String accessTokenId, final String authentication) {
    this.accessTokenId = accessTokenId;
    this.authentication = authentication;
  }

  public String getAccessTokenId() {
    return accessTokenId;
  }

  public String getAuthentication() {
    return authentication;
  }

}
