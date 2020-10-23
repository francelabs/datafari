/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.utils.relevancy.RelevancySetup;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class RelevancySetupConfiguration {

  private static String relevancySetupFileName = "relevancySetup.json";
  private static String goldenQueriesFileName = "goldenQueries.json";

  private static String relevancySetupFilePath;
  private static String goldenQueriesFilePath;

  private static RelevancySetupConfiguration instance;
  private boolean listenPropertiesChanges = false;
  private static RelevancySetup rs = null;

  private final static Logger LOGGER = LogManager.getLogger(RelevancySetupConfiguration.class.getName());

  public RelevancySetup getRelevancySetup() {
    return rs;
  }

  /**
   *
   * Get the instance
   *
   */
  public static RelevancySetupConfiguration getInstance() {
    if (null == instance) {
      instance = new RelevancySetupConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private RelevancySetupConfiguration() {
    String env = Environment.getEnvironmentVariable("SOLR_INSTALL_DIR");
    if (env == null) {
      env = "/opt/datafari/solr";
    }
    relevancySetupFilePath = env + "/solrcloud/FileShare/conf/" + relevancySetupFileName;
    goldenQueriesFilePath = env + "/solrcloud/FileShare/conf/" + goldenQueriesFileName;
    loadSetup();
    listenChanges();
  }

  /**
   * Load/reload the advanced search properties file
   */
  private void loadSetup() {
    final File configFile = new File(relevancySetupFilePath);
    final File goldenQueriesFile = new File(goldenQueriesFilePath);

    try {
      final boolean configCreation = configFile.createNewFile();
      final boolean goldenCreation = goldenQueriesFile.createNewFile();
      rs = new RelevancySetup(configFile, goldenQueriesFile, configCreation, goldenCreation);
    } catch (final IOException e) {
      LOGGER.error("Impossible to create the file " + configFile.getAbsolutePath() + "and/or the file " + goldenQueriesFile.getAbsolutePath(), e);
    }
  }

  /**
   * Start a FileWatcher thread to detect every modification of the advanced
   * search properties file and automatically reload it
   */
  private void watchPropertiesFile() {
    final Thread watcherThread = new Thread(new Runnable() {

      @Override
      public void run() {
        // Java Watchers work on directories so we need to extract the
        // parent directory path of the properties file
        String relevancySetupParentDir = new File(relevancySetupFilePath).getAbsolutePath();
        relevancySetupParentDir = relevancySetupParentDir.substring(0, relevancySetupParentDir.lastIndexOf(File.separator));
        final Path relevancySetupPath = new File(relevancySetupParentDir).toPath();
        String goldenQueriesParentDir = new File(goldenQueriesFilePath).getAbsolutePath();
        goldenQueriesParentDir = goldenQueriesParentDir.substring(0, goldenQueriesParentDir.lastIndexOf(File.separator));
        final Path goldenQueriesPath = goldenQueriesParentDir.equals(relevancySetupParentDir) ? null : new File(goldenQueriesParentDir).toPath();
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
          // Register the service on MODIFY events
          relevancySetupPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
          if (goldenQueriesPath != null) {
            goldenQueriesPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
          }
          while (true && listenPropertiesChanges) {
            final WatchKey wk = watchService.take();
            for (final WatchEvent<?> event : wk.pollEvents()) {
              // we only register "ENTRY_MODIFY" so the context is
              // always
              // a Path.
              final Path changed = (Path) event.context();
              if ((changed.endsWith(relevancySetupFileName) || changed.endsWith(goldenQueriesFileName)) && event.count() == 1) {
                LOGGER.info("Relevancy setup or golden queries config file has changed, reloading");
                Thread.sleep(200);
                loadSetup();
              }
            }
            // reset the key
            wk.reset();
          }
        } catch (IOException | InterruptedException e) {
          LOGGER.error(e.getMessage());
        }
      }
    });
    watcherThread.start();
  }

  /**
   * Listen for every changes/modifications of the advanced search properties
   * file
   */
  public void listenChanges() {
    listenPropertiesChanges = true;
    watchPropertiesFile();
  }

  /**
   * Order to stop checking any modification of the properties file
   */
  public void stopListeningChanges() {
    listenPropertiesChanges = false;
  }

  public void changeFilePath(final String newRelevancySetupFilePath, final String newGoldenQueriesFilePath) {
    stopListeningChanges();
    final String oldRelevancyFilePath = relevancySetupFilePath;
    final String oldRelevancyFileName = relevancySetupFileName;
    final String oldGoldenQueriesFilePath = goldenQueriesFilePath;
    final String oldGoldenQueriesFileName = goldenQueriesFileName;
    relevancySetupFilePath = newRelevancySetupFilePath;
    relevancySetupFileName = relevancySetupFilePath.substring(relevancySetupFilePath.lastIndexOf(File.separator) + 1);
    goldenQueriesFilePath = newGoldenQueriesFilePath;
    goldenQueriesFileName = goldenQueriesFilePath.substring(goldenQueriesFilePath.lastIndexOf(File.separator) + 1);
    try {
      loadSetup();
    } catch (final Exception e) {
      // If anything wrong happens while loading the new config files, revert
      // back to the previous ones.
      relevancySetupFileName = oldRelevancyFileName;
      relevancySetupFilePath = oldRelevancyFilePath;
      goldenQueriesFileName = oldGoldenQueriesFileName;
      goldenQueriesFilePath = oldGoldenQueriesFilePath;
      loadSetup();
      throw e;
    }
  }

  public String getRelevancySetupFilePath() {
    return relevancySetupFilePath;
  }

  public String getGoldenQueriesFilePath() {
    return goldenQueriesFilePath;
  }

}