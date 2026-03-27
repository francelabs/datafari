package com.francelabs.datafari.security.token;

import org.springframework.security.core.Authentication;

public interface DatafariTokenService {

  LegacyAccessToken issueToken(Authentication authentication, String clientId);

  Authentication authenticate(String tokenValue);
}