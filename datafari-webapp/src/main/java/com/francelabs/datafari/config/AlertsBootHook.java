package com.francelabs.datafari.config;

import com.francelabs.datafari.alerts.AlertsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy; // <-- use javax with Spring 5
import java.io.IOException;

@Component
@Order(400) // Runs fairly early; adjust if alerts depend on other hooks
public class AlertsBootHook {

  private static final Logger LOGGER = LogManager.getLogger(AlertsBootHook.class);

  /** Called once when the Spring application context is fully initialized. */
  @EventListener(ContextRefreshedEvent.class)
  public void onReady() {
    try {
      // Turn alerts on at application startup (idempotent if AlertsManager is safe)
      AlertsManager.getInstance().turnOn();
      LOGGER.info("Alerts have been turned ON at startup.");
    } catch (IOException e) {
      // Do not break startup; just log the problem
      LOGGER.error("Error while turning ON alerts at startup.", e);
    }
  }

  /** Called when the application is shutting down (container is stopping). */
  @PreDestroy
  public void onShutdown() {
    try {
      // Cleanly stop scheduled tasks to avoid duplication on redeploys
      AlertsManager.getInstance().turnOff();
      LOGGER.info("Alerts have been turned OFF on shutdown.");
    } catch (Exception e) {
      LOGGER.warn("Error while turning OFF alerts on shutdown.", e);
    }
  }
}