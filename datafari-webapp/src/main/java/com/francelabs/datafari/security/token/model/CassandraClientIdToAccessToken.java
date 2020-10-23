package com.francelabs.datafari.security.token.model;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = CassandraClientIdToAccessToken.TABLE)
public class CassandraClientIdToAccessToken {

  public static final String TABLE = "oauth_client_id_to_access_token";

  @PrimaryKeyColumn(name = "clientId", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
  private final String clientId;
  @PrimaryKeyColumn(name = "accessToken", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
  private final String accessToken;

  public CassandraClientIdToAccessToken(final String clientId, final String accessToken) {
    this.clientId = clientId;
    this.accessToken = accessToken;
  }

  public String getClientId() {
    return clientId;
  }

  public String getAccessToken() {
    return accessToken;
  }

}
