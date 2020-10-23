package com.francelabs.datafari.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.utils.Environment;

public abstract class AbstractConfigClass implements IConfigClass {

  protected final String configPropertiesFileNameAbsolutePath;

  private final Logger LOGGER;

  protected Properties properties;

  protected final ReadWriteLock lock = new ReentrantReadWriteLock();

  /**
   * Load the provided properties file from $CONFIG_HOME
   *
   * @param configPropertiesFileName
   *          the properties file name
   * @param logger
   *          the logger to use
   */
  protected AbstractConfigClass(final String configPropertiesFileName, final Logger logger) {
    LOGGER = logger;
    String envPath = Environment.getEnvironmentVariable("CONFIG_HOME");
    if (envPath == null) {
      envPath = "/opt/datafari/tomcat/conf";
      LOGGER.warn("The CONFIG_HOME environment variable is not set, using default value: " + envPath);
    } else {
      LOGGER.debug("Using CONFIG_HOME: " + envPath);
    }
    configPropertiesFileNameAbsolutePath = envPath + File.separator + configPropertiesFileName;
    loadProperties();
    ConfigManager.getInstance().addFileToWatch(configPropertiesFileNameAbsolutePath, this);
  }

  /**
   * Load the properties file referenced by the configPropertiesFileNameAbsolutePath parameter
   *
   * @param configPropertiesFileName
   *          the properties file name
   * @param configPropertiesFileNameAbsolutePath
   *          the properties file absolute path
   * @param logger
   *          the logger to use
   */
  protected AbstractConfigClass(final String configPropertiesFileName, final String configPropertiesFileNameAbsolutePath, final Logger logger) {
    LOGGER = logger;
    LOGGER.debug("Using direct file path: " + configPropertiesFileNameAbsolutePath);
    this.configPropertiesFileNameAbsolutePath = configPropertiesFileNameAbsolutePath;
    loadProperties();
    ConfigManager.getInstance().addFileToWatch(configPropertiesFileNameAbsolutePath, this);
  }

  /**
   * Return the value of the property given as parameter
   *
   * @param key
   *          the property name
   * @return the property value
   */
  @Override
  public String getProperty(final String key) {
    lock.readLock().lock();
    final String prop = (String) properties.get(key);
    if (prop == null) {
      LOGGER.warn("Property " + key + " not found in the following property file: " + this.configPropertiesFileNameAbsolutePath);
    }
    lock.readLock().unlock();
    return prop;
  }

  /**
   * Return the value of the property given as parameter
   *
   * @param key
   *          the property name
   * @param defaultValue
   *          the default value to return in case of null or error
   * @return the property value
   */
  @Override
  public String getProperty(final String key, final String defaultValue) {
    String prop = getProperty(key);
    if (prop == null) {
      prop = defaultValue;
    }
    return prop;
  }

  @Override
  public void setProperty(final String key, final String value) {
    lock.writeLock().lock();
    properties.setProperty(key, value);
    lock.writeLock().unlock();
  }

  @Override
  public void saveProperties() throws IOException {
    lock.writeLock().lock();
    try (final FileWriterWithEncoding propWriter = new FileWriterWithEncoding(new File(configPropertiesFileNameAbsolutePath), StandardCharsets.UTF_8);) {
      properties.store(propWriter, null);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Load/reload the properties file
   */
  protected void loadProperties() {
    lock.writeLock().lock();
    final File configFile = new File(configPropertiesFileNameAbsolutePath);
    // Make a copy of the current inmemory properties (if available), to restore it in case of errors
    Properties currentState = null;
    if (properties != null) {
      currentState = new Properties();
      currentState.putAll(properties);
      properties.clear();
    } else {
      properties = new Properties();
    }
    try (final InputStream stream = new FileInputStream(configFile); final InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);) {
      properties.load(isr);
    } catch (final IOException e) {
      LOGGER.error("Cannot read file : " + configPropertiesFileNameAbsolutePath, e);
      if (currentState != null) {
        LOGGER.warn("Restoring last known inmemory state of " + configPropertiesFileNameAbsolutePath);
        properties.putAll(currentState);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Method that is executed when the properties file is reloaded
   */
  @Override
  public void onPropertiesReloaded() {
    // TODO Auto-generated method stub

  }

}
