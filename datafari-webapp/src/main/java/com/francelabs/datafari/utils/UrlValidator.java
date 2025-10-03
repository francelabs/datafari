package com.francelabs.datafari.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class UrlValidator {

  private UrlValidator() {}

  private static final Pattern SCHEME_RX = Pattern.compile("^([A-Za-z][A-Za-z0-9+\\-.]*):");

  private static Set<String> getAllowedProtocols() {
    final Set<String> allowed = new HashSet<>();
    allowed.add("http");
    allowed.add("https");
    allowed.add("file");
    allowed.add("datafari");

    
    final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
    String extra = config.getProperty(DatafariMainConfiguration.ALLOWED_PROTOCOLS_URL);
    if (extra != null && !extra.isBlank()) {
      List<String> extraList = Arrays.asList(extra.split(","));
      for (String proto : extraList) {
        allowed.add(proto.trim().toLowerCase(Locale.ROOT));
      }
    }
    return allowed;
  }

  
  public static boolean isAllowed(final String raw) {
    if (raw == null || raw.isBlank()) {
      return false;
    }

    final var m = SCHEME_RX.matcher(raw);
   
    if (!m.find()) {
      return false; 
    }

    final String scheme = m.group(1).toLowerCase(Locale.ROOT);
    return getAllowedProtocols().contains(scheme);
  }
}