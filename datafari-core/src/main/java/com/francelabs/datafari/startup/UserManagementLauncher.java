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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.ScriptConfiguration;

public class UserManagementLauncher implements ServletContextListener {


	private static Logger LOGGER = LogManager.getLogger(UserManagementLauncher.class
			.getName());
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		try {
			LOGGER.info("UserManagement info");
			if (!UserDataService.getInstance().isInBase("admin")){
				LOGGER.info("UserManagement admin");
				User user = new User("admin",ScriptConfiguration.getProperty("TEMPADMINPASSWORD"));
				List<String> roleAdmin = new ArrayList<String>(); 
				roleAdmin.add(UserDataService.SEARCHADMINISTRATOR);
				// Set role for tomcat manager app
				roleAdmin.add("manager-gui");
				user.signup(roleAdmin);
				LOGGER.info("Admin user created");
				
			}
		} catch (Exception e) {
			LOGGER.error("Cannot create admin account",e);
		} 
		
		// Keep TEMPADMINPASSWORD in datafari.properties : should be changed by user anyway
		//ScriptConfiguration.setProperty("TEMPADMINPASSWORD", "");
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}




}
