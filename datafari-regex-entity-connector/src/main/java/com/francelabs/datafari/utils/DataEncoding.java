package com.francelabs.datafari.utils;

import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Utility class for all data encoding tasks.
 */
public class DataEncoding {
  private DataEncoding(){}

  /**
   * Detect the character set of the given input stream.
   *
   * @param is
   * @return The character set or null if not found.
   * @throws IOException
   */
  public static Charset detect(InputStream is) throws IOException {
    CharsetDetector detector = new CharsetDetector();
    detector.setText(is);
    CharsetMatch match = detector.detect();
    return match != null ? Charset.forName(match.getName()) : null;
  }
}
