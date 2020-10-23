package com.francelabs.datafari.security.token.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class AuthSerialization {

  private static final Logger LOGGER = LogManager.getLogger(AuthSerialization.class.getName());

  public static String serialize(final OAuth2Authentication authentication) {
    try {
      final byte[] bytes = SerializationUtils.serialize(authentication);
      return Base64.encodeBase64String(bytes);
    } catch (final Exception e) {
      LOGGER.error("Unable to serialize OAuth2 Authentication", e);
      throw e;
    }
  }

  public static OAuth2Authentication deserialize(final String authenticationStr) {
    try {
      final byte[] bytes = Base64.decodeBase64(authenticationStr);
      return (OAuth2Authentication) SerializationUtils.deserialize(bytes);
    } catch (final Exception e) {
      LOGGER.error("Unable to deserialize OAuth2 Authentication", e);
      throw e;
    }
  }
}
