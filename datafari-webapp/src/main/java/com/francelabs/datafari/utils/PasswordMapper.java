package com.francelabs.datafari.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PasswordMapper {

  private final String randomPrefix;
  private final Map<String, Integer> passwordToKey = new HashMap<>();
  private final List<String> passwordList = new ArrayList<>();
  private static PasswordMapper instance = null;

  private final static Logger LOGGER = LogManager.getLogger(PasswordMapper.class.getName());

  /** Constructor */
  private PasswordMapper() {
    randomPrefix = generateRandomPrefix();
  }

  public static synchronized PasswordMapper getInstance() {
    if (instance == null) {
      instance = new PasswordMapper();
    }

    return instance;
  }

  /**
   * Map a password to a key.
   *
   * @param password
   *          is the password.
   * @return the key.
   */
  public synchronized String mapPasswordToKey(final String password) {
    // Special case for null or empty password
    if (password == null || password.length() == 0) {
      return password;
    }
    Integer index = passwordToKey.get(password);
    if (index == null) {
      // Need a new key.
      index = new Integer(passwordList.size());
      passwordList.add(password);
      passwordToKey.put(password, index);
    }
    return randomPrefix + index;
  }

  /**
   * Map a key back to a password.
   *
   * @param key
   *          is the key (or a password, if changed)
   * @return the password.
   */
  public synchronized String mapKeyToPassword(final String key) {
    if (key != null && key.startsWith(randomPrefix)) {
      final String intPart = key.substring(randomPrefix.length());
      try {
        final int index = Integer.parseInt(intPart);
        if (index < passwordList.size()) {
          return passwordList.get(index);
        }
      } catch (final NumberFormatException e) {
        LOGGER.error("Something is wrong with the provided password", e);
      }
    }
    return key;
  }

  // Protected methods

  private static final char[] pickChars = new char[] { '\u20f1', '\u20c4', '\u2072', '\u208F' };

  /** Generate a random prefix that will not likely collide with any password */
  private String generateRandomPrefix() {
    final Random r = new Random(System.currentTimeMillis());
    final StringBuilder sb = new StringBuilder("_");
    for (int i = 0; i < 8; i++) {
      final int index = r.nextInt(pickChars.length);
      sb.append(pickChars[index]);
    }
    sb.append("_");
    return sb.toString();
  }

}
