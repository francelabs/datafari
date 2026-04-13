package com.francelabs.datafari.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SecretFileReader {

  private static final Logger LOGGER = LogManager.getLogger(SecretFileReader.class);

  private SecretFileReader() {
    // Utility class → no instantiation
  }

  /**
   * Read a required secret file.
   * Throws an exception if:
   * - file does not exist
   * - file is not readable
   * - file is empty
   */
  public static String readRequiredSecret(String filePath) {
    return readRequiredSecret(Path.of(filePath));
  }

  public static String readRequiredSecret(Path path) {
    try {
      if (!Files.exists(path)) {
        throw new IllegalStateException("Secret file does not exist: " + path);
      }

      if (!Files.isReadable(path)) {
        throw new IllegalStateException("Secret file is not readable: " + path);
      }

      String secret = Files.readString(path, StandardCharsets.UTF_8).trim();

      if (secret.isEmpty()) {
        throw new IllegalStateException("Secret file is empty: " + path);
      }

      return secret;

    } catch (IOException e) {
      throw new IllegalStateException("Unable to read secret file: " + path, e);
    }
  }

}
