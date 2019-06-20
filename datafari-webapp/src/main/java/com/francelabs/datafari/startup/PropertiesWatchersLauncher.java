/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.francelabs.datafari.utils.AdvancedSearchConfiguration;
import com.francelabs.datafari.utils.AlertsConfiguration;
import com.francelabs.datafari.utils.CorePropertiesConfiguration;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.ELKConfiguration;
import com.francelabs.datafari.utils.SolrConfiguration;
import com.francelabs.datafari.utils.WidgetManagerConfiguration;

@WebListener
public class PropertiesWatchersLauncher implements ServletContextListener {

  @Override
  public void contextDestroyed(final ServletContextEvent arg0) {
    AdvancedSearchConfiguration.getInstance().stopListeningChanges();
    ELKConfiguration.getInstance().stopListeningChanges();
    CorePropertiesConfiguration.getInstance().stopListeningChanges();
    SolrConfiguration.getInstance().stopListeningChanges();
    WidgetManagerConfiguration.getInstance().stopListeningChanges();
    AlertsConfiguration.getInstance().stopListeningChanges();
    DatafariMainConfiguration.getInstance().stopListeningChanges();
  }

  @Override
  public void contextInitialized(final ServletContextEvent arg0) {
    AdvancedSearchConfiguration.getInstance().listenChanges();
    ELKConfiguration.getInstance().listenChanges();
    CorePropertiesConfiguration.getInstance().listenChanges();
    SolrConfiguration.getInstance().listenChanges();
    WidgetManagerConfiguration.getInstance().listenChanges();
    AlertsConfiguration.getInstance().listenChanges();
    DatafariMainConfiguration.getInstance().listenChanges();
  }
}
