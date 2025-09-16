package com.francelabs.datafari.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class UrlValidator {

  private UrlValidator() {}

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

  
  public static boolean isAllowed(String raw) {
    
    final URI uri;
    try {
      uri = new URI(raw.strip());
    } catch (Exception e) {
      return false; 
    }

    final String scheme = (uri.getScheme() == null) ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!getAllowedProtocols().contains(scheme)) {
      return false;
    }


    return true;
  }
}