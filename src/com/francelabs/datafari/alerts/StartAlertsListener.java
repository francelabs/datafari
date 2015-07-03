/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.alerts;
/**
 * 
 * This class is used to launch the alerts (if they are activated) at Datafari's start.
 * Also avoid accumulating scheduled task on reloading Datafari (useful only on development environment
 * @author Alexis Karassev
 *
 */

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
@WebListener
public class StartAlertsListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		AlertsManager.getInstance().turnOff();
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		AlertsManager.getInstance().getParameter();
	}
}
