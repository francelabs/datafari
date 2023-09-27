package com.francelabs.datafari.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpringSecurityConfiguration {

  private static SpringSecurityConfiguration instance = null;

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final Logger LOGGER = LogManager.getLogger(SpringSecurityConfiguration.class.getName());

  private Properties appProps;

  private SpringSecurityConfiguration() {
    loadProperties();
  }

  public static SpringSecurityConfiguration getInstance() {
    if (instance == null) {
      instance = new SpringSecurityConfiguration();
    }

    return instance;
  }

  public String getProperty(final String key) {
    lock.readLock().lock();
    final String prop = (String) appProps.get(key);
    if (prop == null) {
      LOGGER.warn("Property '" + key + "' not found in the Spring Security application.properties file");
    }
    lock.readLock().unlock();
    return prop;
  }

  public String getProperty(final String key, final String defaultValue) {
    lock.readLock().lock();
    final String prop = appProps.getProperty(key, defaultValue);
    if (prop == null) {
      LOGGER.warn("Property '" + key + "' not found in the Spring Security application.properties file");
    }
    lock.readLock().unlock();
    return prop;
  }

  private void loadProperties() {
    lock.writeLock().lock();
    appProps = new Properties();
    try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties"); final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);) {
      appProps.load(isr);
    } catch (final IOException e) {
      LOGGER.error("Cannot read file Spring Security application.properties file", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

}
