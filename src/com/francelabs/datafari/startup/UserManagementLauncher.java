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

import java.util.Collections;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.francelabs.datafari.service.db.UserDataService;

public class UserManagementLauncher implements ServletContextListener {


	private static Logger LOGGER = Logger.getLogger(UserManagementLauncher.class
			.getName());
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		try {
			if (!UserDataService.getInstance().isInBase("admin")){
				
				UserDataService.getInstance().addUser("admin", "admin", Collections.singletonList(UserDataService.SEARCHADMINISTRATOR));
			}
		} catch (Exception e) {
			LOGGER.error("Cannot create admin account",e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}




}
