package com.francelabs.datafari.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class ObfuscationTool {

  private static final int IV_LENGTH = 16;

  private static Cipher getCipher(final String saltValue, final int mode, final String passCode, final byte[] iv) throws GeneralSecurityException {

    final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    final KeySpec keySpec = new PBEKeySpec(passCode.toCharArray(), saltValue.getBytes(StandardCharsets.UTF_8), 1024, 128);
    final SecretKey secretKey = factory.generateSecret(keySpec);

    final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    final SecretKeySpec key = new SecretKeySpec(secretKey.getEncoded(), "AES");
    final IvParameterSpec parameterSpec = new IvParameterSpec(iv);
    cipher.init(mode, key, parameterSpec);
    return cipher;

  }

  private static byte[] getSecureRandom() {
    final SecureRandom random = new SecureRandom();
    final byte[] iv = new byte[IV_LENGTH];
    random.nextBytes(iv);
    return iv;
  }

  private static String OBFUSCATION_PASSCODE = "DataFariRox";
  private static String OBFUSCATION_SALT = "PeloPelo";

  public static String obfuscate(final String input) throws Exception {
    return encrypt(OBFUSCATION_SALT, OBFUSCATION_PASSCODE, input);
  }

  public static String deobfuscate(final String input) throws Exception {
    return decrypt(OBFUSCATION_SALT, OBFUSCATION_PASSCODE, input);
  }

  /**
   * Encrypt a string in a reversible encryption.
   *
   * @param saltValue is the salt value.
   * @param passCode  is the pass code.
   * @param input     is the input string.
   * @return the output string.
   */
  protected static String encrypt(final String saltValue, final String passCode, final String input) throws Exception {
    if (input == null) {
      return null;
    }
    if (input.length() == 0) {
      return input;
    }

    final ByteArrayOutputStream os = new ByteArrayOutputStream();

    // Write IV as a prefix:
    final byte[] iv = getSecureRandom();
    os.write(iv);
    os.flush();

    final Cipher cipher = getCipher(saltValue, Cipher.ENCRYPT_MODE, passCode, iv);
    final CipherOutputStream cos = new CipherOutputStream(os, cipher);
    final Writer w = new OutputStreamWriter(cos, java.nio.charset.StandardCharsets.UTF_8);
    w.write(input);
    w.flush();
    // These two shouldn't be necessary, but they are.
    cos.flush();
    cos.close();
    final byte[] bytes = os.toByteArray();
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Decrypt a string.
   *
   * @param saltValue is the salt value.
   * @param passCode  is the pass code.
   * @param input     is the input string.
   * @return the decoded string.
   * @throws Exception
   */
  protected static String decrypt(final String saltValue, final String passCode, final String input) throws Exception {
    if (input == null) {
      return null;
    }
    if (input.length() == 0) {
      return input;
    }

    final ByteArrayInputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(input));

    final byte[] iv = new byte[IV_LENGTH];
    int pointer = 0;
    while (pointer < iv.length) {
      final int amt = is.read(iv, pointer, iv.length - pointer);
      if (amt == -1) {
        throw new Exception("String can't be decrypted: too short");
      }
      pointer += amt;
    }

    final Cipher cipher = getCipher(saltValue, Cipher.DECRYPT_MODE, passCode, iv);
    final CipherInputStream cis = new CipherInputStream(is, cipher);
    try (final InputStreamReader reader = new InputStreamReader(cis, java.nio.charset.StandardCharsets.UTF_8);) {
      final StringBuilder sb = new StringBuilder();
      final char[] buffer = new char[65536];
      while (true) {
        final int amt = reader.read(buffer, 0, buffer.length);
        if (amt == -1) {
          break;
        }
        sb.append(buffer, 0, amt);
      }
      return sb.toString();
    }

  }

}
