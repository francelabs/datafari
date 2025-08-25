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
package com.francelabs.datafari.initializers;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.DatafariMainConfiguration;

public class UserManagementInitializer implements IInitializer {

  private static Logger LOGGER = LogManager.getLogger(UserManagementInitializer.class.getName());

  @Override
  public void initialize() {
    try {
      if (!UserDataService.getInstance().isInBase("admin")) {
        LOGGER.info("UserManagement : creating admin user...");
        final User user = new User("admin",
            DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.TEMP_ADMIN_PASSWORD));
        final List<String> roleAdmin = new ArrayList<String>();
        roleAdmin.add(UserDataService.SEARCHADMINISTRATOR);
        // Set role for tomcat manager app
        // roleAdmin.add("manager-gui");
        user.signup(roleAdmin);
        LOGGER.info("Admin user created");
        AuditLogUtil.log("Postgresql", "automated", "none", "Creation of the admin user");
      }
    } catch (final Exception e) {
      LOGGER.error("Cannot create admin account", e);
    }

  }

  @Override
  public void shutdown() {
    // Nothing to do

  }

}
