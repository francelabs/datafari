package com.francelabs.datafari.security.token.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraRefreshTokenAuthentication.TABLE)
public class CassandraRefreshTokenAuthentication {

  public static final String TABLE = "oauth_refresh_token_auth";

  @PrimaryKey
  private final String refreshTokenId;
  private final String authentication;

  public CassandraRefreshTokenAuthentication(final String refreshTokenId, final String authentication) {
    this.refreshTokenId = refreshTokenId;
    this.authentication = authentication;
  }

  public String getRefreshTokenId() {
    return refreshTokenId;
  }

  public String getAuth() {
    return authentication;
  }

}
