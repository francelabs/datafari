package com.francelabs.datafari.config;

import com.francelabs.datafari.monitoring.IndexMonitoring;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/** Starts/stops index monitoring safely. */
@Component
@Order(350) // après le bridge SQL et avant les jobs qui en dépendent
public class IndexMonitoringBootHook implements ApplicationRunner {

  private static final Logger LOGGER = LogManager.getLogger(IndexMonitoringBootHook.class);
  private volatile boolean started = false;

  @Override
  public void run(ApplicationArguments args) {
    try {
      IndexMonitoring.getInstance().startIndexMonitoring();
      started = true;
      LOGGER.info("Index monitoring started.");
    } catch (Exception e) {
      // Don't fail app startup because of monitoring; just log it.
      LOGGER.error("Error while starting index monitoring.", e);
      started = false;
    }
  }

  @PreDestroy
  public void onShutdown() {
    try {
      if (started) {
        IndexMonitoring.getInstance().stopIndexMonitoring();
        LOGGER.info("Index monitoring stopped.");
      } else {
        LOGGER.info("Index monitoring was not started; skipping stop.");
      }
    } catch (Exception e) {
      // Make shutdown robust; never rethrow here.
      LOGGER.error("Error while stopping index monitoring.", e);
    }
  }
}