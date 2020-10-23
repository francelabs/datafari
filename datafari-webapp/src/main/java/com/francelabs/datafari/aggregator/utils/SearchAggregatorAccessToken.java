package com.francelabs.datafari.aggregator.utils;

public class SearchAggregatorAccessToken {

  private final String accessToken;
  private final long timeout;

  public SearchAggregatorAccessToken(final String accessToken, final long timeout) {
    this.accessToken = accessToken;
    this.timeout = timeout;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public long getTimeout() {
    return timeout;
  }

}
