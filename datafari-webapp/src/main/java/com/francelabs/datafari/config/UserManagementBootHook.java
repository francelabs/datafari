package com.francelabs.datafari.config;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.francelabs.datafari.utils.SecretFileReader;
import java.util.List;
import java.io.File;


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
      if (!users.isInBase("admin")) {
        LOGGER.info("UserManagement: creating admin user...");

        String tmpPwd = SecretFileReader.readRequiredSecret(
            getAdminPasswordFilePath()
        );

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

  private String getAdminPasswordFilePath() {
    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) {
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }

    return environnement
        + File.separator + "secrets"
        + File.separator + "datafari_admin_password";
  }
}
