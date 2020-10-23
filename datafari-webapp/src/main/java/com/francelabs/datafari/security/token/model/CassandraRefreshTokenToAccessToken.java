package com.francelabs.datafari.security.token.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraRefreshTokenToAccessToken.TABLE)
public class CassandraRefreshTokenToAccessToken {

  public static final String TABLE = "oauth_refresh_token_to_access_token";

  @PrimaryKey
  private final String refreshTokenId;
  private final String accessTokenId;

  public CassandraRefreshTokenToAccessToken(final String refreshTokenId, final String accessTokenId) {
    this.refreshTokenId = refreshTokenId;
    this.accessTokenId = accessTokenId;
  }

  public String getRefreshTokenId() {
    return refreshTokenId;
  }

  public String getAccessTokenId() {
    return accessTokenId;
  }

}
