package com.francelabs.datafari.config;

import com.francelabs.datafari.service.indexer.IndexerServerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class IndexerServersBootHook {

  private static final Logger LOGGER = LogManager.getLogger(IndexerServersBootHook.class);

  // Nothing to do on startup (kept for future extension)
  @EventListener(ContextRefreshedEvent.class)
  public void onReady() {
    LOGGER.info("IndexerServersBootHook initialized (no startup action).");
  }

  // Proper cleanup on shutdown
  @PreDestroy
  public void onShutdown() {
    try {
      IndexerServerManager.closeAllIndexerServers();
      LOGGER.info("All IndexerServers closed successfully at shutdown.");
    } catch (Exception e) {
      LOGGER.error("Error while closing IndexerServers at shutdown.", e);
    }
  }
}