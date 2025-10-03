package com.francelabs.datafari.config;

import com.francelabs.datafari.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class ConfigWatchersBootHook {

  private static final Logger LOGGER = LogManager.getLogger(ConfigWatchersBootHook.class);

  // Called once the Spring context is fully initialized and ready
  @EventListener(ContextRefreshedEvent.class)
  public void onReady() {
    try {
      // Ensure configuration watchers are initialized
      ConfigManager.getInstance();
      LOGGER.info("ConfigManager initialized (watchers ready).");
    } catch (Exception e) {
      LOGGER.error("Failed to initialize ConfigManager.", e);
    }
  }

  // Called on application shutdown to release resources cleanly
  @PreDestroy
  public void onShutdown() {
    try {
      ConfigManager.getInstance().stopListeningChanges();
      LOGGER.info("ConfigManager watchers stopped.");
    } catch (Exception e) {
      LOGGER.error("Error while stopping ConfigManager watchers.", e);
    }
  }
}