package com.francelabs.datafari.security.token.model;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraUsernameToAccessToken.TABLE)
public class CassandraUsernameToAccessToken {

  public static final String TABLE = "oauth_username_to_access_token";

  @PrimaryKeyColumn(name = "approvalkey", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
  private final String approvalKey;
  @PrimaryKeyColumn(name = "accesstoken", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
  private final String accessToken;

  public CassandraUsernameToAccessToken(final String approvalKey, final String accessToken) {
    this.approvalKey = approvalKey;
    this.accessToken = accessToken;
  }

  public String getApprovalKey() {
    return approvalKey;
  }

  public String getAccessToken() {
    return accessToken;
  }

}
