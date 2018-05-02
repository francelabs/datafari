package com.francelabs.datafari.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Properties;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.log4j.Logger;

import com.francelabs.datafari.utils.Environment;

public abstract class AbstractConfigClass implements IConfigClass {

  private final String configPropertiesFileName;

  private final String configPropertiesFileNameAbsolutePath;

  private final Logger LOGGER;

  protected Properties properties;

  private Thread watcherThread;

  protected AbstractConfigClass(final String configPropertiesFileName, final Logger logger) {
    LOGGER = logger;
    this.configPropertiesFileName = configPropertiesFileName;
    String envPath = Environment.getProperty("catalina.home");
    if (envPath == null) {
      envPath = "/opt/datafari/tomcat";
    }
    configPropertiesFileNameAbsolutePath = envPath + File.separator + "conf" + File.separator
        + configPropertiesFileName;
    loadProperties();
  }

  /**
   * Return the value of the property given as parameter
   *
   * @param key
   *          the property name
   * @return the property value
   * @throws IOException
   */
  @Override
  public String getProperty(final String key) throws IOException {
    return (String) properties.get(key);
  }

  @Override
  public void setProperty(final String key, final String value) {
    properties.setProperty(key, value);
  }

  @Override
  public void saveProperties() throws IOException {
    try (final FileWriterWithEncoding propWriter = new FileWriterWithEncoding(
        new File(configPropertiesFileNameAbsolutePath), StandardCharsets.UTF_8);) {
      properties.store(propWriter, null);
    }
  }

  /**
   * Load/reload the advanced search properties file
   */
  private void loadProperties() {
    final File configFile = new File(configPropertiesFileNameAbsolutePath);
    properties = new Properties();
    try (final InputStream stream = new FileInputStream(configFile);
        final InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);) {
      properties.load(isr);
    } catch (final IOException e) {
      LOGGER.error("Cannot read file : " + configFile.getAbsolutePath(), e);
    }
  }

  /**
   * Method that is executed when the properties file is reloaded
   */
  @Override
  public void onPropertiesReloaded() {
    // TODO Auto-generated method stub

  }

  /**
   * Start a FileWatcher thread to detect every modification of the advanced
   * search properties file and automatically reload it
   */
  private void watchPropertiesFile() {
    watcherThread = new Thread(new Runnable() {

      @Override
      public void run() {
        // Java Watchers work on directories so we need to extract the
        // parent directory path of the properties file
        String parentDir = new File(configPropertiesFileNameAbsolutePath).getAbsolutePath();
        parentDir = parentDir.substring(0, parentDir.lastIndexOf(File.separator));
        final Path path = new File(parentDir).toPath();
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
          // Register the service on MODIFY events
          path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
          while (true) {
            final WatchKey wk = watchService.take();
            for (final WatchEvent<?> event : wk.pollEvents()) {
              // we only register "ENTRY_MODIFY" so the context is
              // always
              // a Path.
              final Path changed = (Path) event.context();
              if (changed.endsWith(configPropertiesFileName) && (event.count() == 1)) {
                LOGGER.info("Advanced search config file has changed, reloading the properties");
                Thread.sleep(200);
                loadProperties();
                onPropertiesReloaded();
              }
            }
            // reset the key
            wk.reset();
          }
        } catch (final IOException e) {
          LOGGER.error(e.getMessage());
          Thread.currentThread().interrupt();
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    watcherThread.start();
  }

  /**
   * Listen for every changes/modifications of the advanced search properties file
   */
  @Override
  public synchronized void listenChanges() {
    watchPropertiesFile();
  }

  /**
   * Order to stop checking any modification of the properties file
   */
  @Override
  public synchronized void stopListeningChanges() {
    watcherThread.interrupt();
  }

}
