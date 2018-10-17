/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.startup;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.UpdateNbLikes;

public class LikesLauncher implements ServletContextListener {

  private static boolean islaunched = false;
  private static ScheduledExecutorService scheduler;
  private static Logger logger = LogManager.getLogger(LikesLauncher.class.getName());
  private static ScheduledFuture<?> handler;
  private static boolean doReload = false;
  private static boolean isThreadUpdateNbLikesStarted = false;

  @Override
  public void contextInitialized(final ServletContextEvent arg0) {

    String isEnabled = null;
    try {
      isEnabled = DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES);
    } catch (final IOException e) {
      logger.error("Unable to log property " + DatafariMainConfiguration.LIKESANDFAVORTES + " : " + e.getMessage());
    }
    try {
      final File externalFile = UpdateNbLikes.getInstance().getConfigFile();
      externalFile.createNewFile();
    } catch (DatafariServerException | IOException e) {
      logger.error("Unable to read external file " + e.getMessage());
    }
    if (isEnabled != null && isEnabled.equals("true")) {
      updateNbLikes();
      startScheduler();
    }
    logger.debug("LikesLauncher Servlet Initialized successfully");
  }

  @Override
  public void contextDestroyed(final ServletContextEvent arg0) {
    LikesLauncher.shutDown();
  }

  private void updateNbLikes() {
    if (!LikesLauncher.isThreadUpdateNbLikesStarted) {
      LikesLauncher.isThreadUpdateNbLikesStarted = true;
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            final IndexerServer server = IndexerServerManager.getIndexerServer(Core.FILESHARE);
            final IndexerQuery refreshQuery = IndexerServerManager.createQuery();
            refreshQuery.setRequestHandler("/reloadCache");
            server.executeQuery(refreshQuery);
          } catch (final Exception e) {
            logger.error("Cannot send refresh request", e);
          }
          logger.info("updateNbLikes finished its work");
        }

      }).start();
    }
  }

  public static void startScheduler() {
    if (!islaunched) {
      islaunched = true;
      scheduler = Executors.newScheduledThreadPool(1);
      handler = scheduler.scheduleAtFixedRate(reloadCache, 1, 10, TimeUnit.SECONDS);
    }
  }

  public static void saveChange() {
    LikesLauncher.doReload = true;
  }

  static Runnable reloadCache = new Runnable() {
    @Override
    public void run() {
      if (LikesLauncher.doReload) {
        try {
          final IndexerServer server = IndexerServerManager.getIndexerServer(Core.FILESHARE);
          final IndexerQuery refreshQuery = IndexerServerManager.createQuery();
          refreshQuery.setRequestHandler("/reloadCache");
          server.executeQuery(refreshQuery);
          LikesLauncher.doReload = false;
        } catch (final Exception e) {
          logger.error("Cannot reload cache", e);
        }
      }
    }
  };

  public static void shutDown() {
    logger.debug("-----------------Trying to ShutDown the scheduler---------------------");
    if (islaunched) {
      handler.cancel(true);
      scheduler.shutdown();
    }
    islaunched = false;
  }

}
