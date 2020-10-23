package com.francelabs.datafari.security.token.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;

public class AccessTokenSerialization {

  private static final Logger LOGGER = LogManager.getLogger(AccessTokenSerialization.class.getName());

  public static String serialize(final OAuth2AccessToken accessToken) {
    try {
      final byte[] bytes = SerializationUtils.serialize(accessToken);
      return Base64.encodeBase64String(bytes);
    } catch (final Exception e) {
      LOGGER.error("Unable to serialize OAuth2 AccessToken", e);
      throw e;
    }
  }

  public static OAuth2AccessToken deserialize(final String accessTokenStr) {
    try {
      final byte[] bytes = Base64.decodeBase64(accessTokenStr);
      return (OAuth2AccessToken) SerializationUtils.deserialize(bytes);
    } catch (final Exception e) {
      LOGGER.error("Unable to deserialize OAuth2 AccessToken", e);
      throw e;
    }
  }

}
