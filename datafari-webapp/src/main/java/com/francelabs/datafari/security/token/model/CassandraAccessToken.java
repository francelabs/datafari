package com.francelabs.datafari.security.token.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraAccessToken.TABLE)
public class CassandraAccessToken {

  public static final String TABLE = "oauth_access_tokens";

  @PrimaryKey
  private final String accessTokenId;
  private final String accessToken;

  public CassandraAccessToken(final String accessTokenId, final String accessToken) {
    this.accessTokenId = accessTokenId;
    this.accessToken = accessToken;
  }

  public String getAccessTokenId() {
    return accessTokenId;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getAccessTokenStr() {
    return accessToken;
  }

}
