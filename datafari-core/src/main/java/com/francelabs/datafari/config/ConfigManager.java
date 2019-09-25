package com.francelabs.datafari.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigManager {

  private static ConfigManager instance = null;
  private final Map<String, AbstractConfigClass> filesToWatch = new HashMap<>();
  private Thread watcherThread;
  private WatchService watchService;
  private final static Logger LOGGER = LogManager.getLogger(ConfigManager.class.getName());

  public static synchronized ConfigManager getInstance() {
    if (instance == null) {
      instance = new ConfigManager();
    } else if (instance.watchService == null || instance.watcherThread == null || instance.watcherThread.isInterrupted() || !instance.watcherThread.isAlive()) {
      if (instance.watcherThread != null && instance.watcherThread.isAlive()) {
        instance.watcherThread.interrupt();
      }
      instance.watchPropertiesFiles();
    }
    return instance;
  }

  private ConfigManager() {
    watchPropertiesFiles();
  }

  public void addFileToWatch(final String absoluteFilePath, final AbstractConfigClass config) {
    String parentDir = new File(absoluteFilePath).getAbsolutePath();
    parentDir = parentDir.substring(0, parentDir.lastIndexOf(File.separator));
    final Path path = new File(parentDir).toPath();
    // Register the service on MODIFY events
    try {
      // we only register "ENTRY_MODIFY" so the context is
      // always
      // a Path.
      path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
      filesToWatch.put(absoluteFilePath, config);
    } catch (final IOException e) {
      LOGGER.error("Unable to register the file " + absoluteFilePath, e);
    }
  }

  /**
   * Start a FileWatcher thread to detect every modification of the advanced search properties file and automatically reload it
   */
  private void watchPropertiesFiles() {
    watcherThread = new Thread(new Runnable() {

      @Override
      public void run() {
        // Java Watchers work on directories so we need to extract the
        // parent directory path of the properties file

        try {
          watchService = FileSystems.getDefault().newWatchService();
          while (true) {
            final WatchKey wk = watchService.take();
            for (final WatchEvent<?> event : wk.pollEvents()) {
              final Path changed = (Path) event.context();
              if (event.count() == 1) {
                for (final String absolutePath : filesToWatch.keySet()) {
                  if (absolutePath.endsWith(changed.toString())) {
                    LOGGER.info(absolutePath + " has been modified, reloading the properties");
                    Thread.sleep(200);
                    final AbstractConfigClass config = filesToWatch.get(absolutePath);
                    config.loadProperties();
                    config.onPropertiesReloaded();
                  }
                }
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
        } finally {
          try {
            watchService.close();
          } catch (final IOException e) {
            LOGGER.error("Unable to close the WatchService", e);
          }
        }
      }
    });
    watcherThread.start();
    try {
      Thread.sleep(1500);
    } catch (final InterruptedException e) {
      LOGGER.error("Unable to sleep", e);
    }
  }

  /**
   * Order to stop checking any modification of the registered properties files
   */
  public synchronized void stopListeningChanges() {
    if (watcherThread != null && watchService != null) {
      try {
        watcherThread.interrupt();
        watchService.close();
        Thread.sleep(1500);
        watcherThread = null;
        watchService = null;
      } catch (final InterruptedException | IOException e) {
        LOGGER.error("Unable to correctly stop the listening thread", e);
      }
    }
  }

}
