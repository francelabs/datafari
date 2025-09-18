package com.francelabs.datafari.config;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserManagementBootHook {

  private static final Logger LOGGER = LogManager.getLogger(UserManagementBootHook.class);

  private final UserDataService users;

  public UserManagementBootHook(UserDataService users) {
    this.users = users;
  }

  @EventListener(ContextRefreshedEvent.class)
  public void onReady() {
    try {
      // Get admin existence using service
      if (!users.isInBase("admin")) {
        LOGGER.info("UserManagement: creating admin user...");

        // Get temporary admin password from legacy singleton (not a Spring bean)
        String tmpPwd = DatafariMainConfiguration.getInstance()
            .getProperty(DatafariMainConfiguration.TEMP_ADMIN_PASSWORD);
        if (tmpPwd == null || tmpPwd.isEmpty()) {
          // Fallback to a sane default to avoid NPE on first boot
          tmpPwd = "admin";
          LOGGER.warn("TEMP_ADMIN_PASSWORD is empty; using default fallback value.");
        }

        // Create user + role
        User user = new User("admin", tmpPwd);
        List<String> roles = List.of(UserDataService.SEARCHADMINISTRATOR);
        user.signup(roles);

        LOGGER.info("Admin user created.");
        AuditLogUtil.log("Postgresql", "automated", "none", "Creation of the admin user");
      } else {
        LOGGER.info("Admin user already exists; skipping bootstrap.");
      }
    } catch (Exception e) {
      LOGGER.error("Cannot create admin account", e);
    }
  }
}