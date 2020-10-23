package com.francelabs.datafari.security.token.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraAuthToAccessToken.TABLE)
public class CassandraAuthToAccessToken {

  public static final String TABLE = "oauth_auth_to_access_token";

  @PrimaryKey
  private final String authKey;
  private final String accessToken;

  public CassandraAuthToAccessToken(final String authKey, final String accessToken) {
    this.authKey = authKey;
    this.accessToken = accessToken;
  }

  public String getAuthKey() {
    return authKey;
  }

  public String getAccessToken() {
    return accessToken;
  }

}
