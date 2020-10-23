package com.francelabs.datafari.security.token.utils;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class UsernameUtils {

  public static String getApprovalKey(final OAuth2Authentication authentication) {
    final String userName = authentication.getUserAuthentication() == null ? "" : authentication.getUserAuthentication().getName();
    return getApprovalKey(authentication.getOAuth2Request().getClientId(), userName);
  }

  public static String getApprovalKey(final String clientId, final String userName) {
    return clientId + (userName == null ? "" : ":" + userName);
  }
}
