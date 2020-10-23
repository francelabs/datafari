package com.francelabs.datafari.utils;

public class Environment {
  public static final String defaultConfigHome = "/opt/datafari/tomcat/conf";

  public static String getEnvironmentVariable(final String variable) {
    return System.getenv(variable);
  }

  public static String getProperty(final String key) {
    return System.getProperty(key);
  }
}
