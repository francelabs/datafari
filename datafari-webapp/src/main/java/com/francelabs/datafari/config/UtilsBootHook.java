package com.francelabs.datafari.config;

import com.francelabs.datafari.ldap.LdapUsers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UtilsBootHook {

  private static final Logger LOGGER = LogManager.getLogger(UtilsBootHook.class);

  /**
   * Called when the Spring application context is shutting down.
   * Stops the LDAP users sweeper to release threads/resources.
   * Idempotent: calling stop multiple times should be safe.
   */
  @EventListener(ContextClosedEvent.class)
  public void onShutdown() {
    try {
      LdapUsers.getInstance().stopSweep();
      LOGGER.info("LDAP sweep stopped.");
    } catch (Exception e) {
      // Do not block shutdown; just log.
      LOGGER.warn("Error while stopping LDAP sweep.", e);
    }
  }
}