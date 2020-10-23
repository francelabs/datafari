package com.francelabs.datafari.security.token.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraRefreshToken.TABLE)
public class CassandraRefreshToken {

  public static final String TABLE = "oauth_refresh_tokens";

  @PrimaryKey
  private final String refreshTokenId;
  private final String refreshToken;

  public CassandraRefreshToken(final String refreshTokenId, final String refreshToken) {
    this.refreshTokenId = refreshTokenId;
    this.refreshToken = refreshToken;
  }

  public String getRefreshTokenId() {
    return refreshTokenId;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

}
