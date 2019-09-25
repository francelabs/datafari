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
package com.francelabs.datafari.elk;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.francelabs.datafari.utils.ELKConfiguration;

@WebListener
public class ELKLauncher implements ServletContextListener {

  @Override
  public void contextDestroyed(final ServletContextEvent arg0) {
    ActivateELK.getInstance().deactivate();
  }

  @Override
  public void contextInitialized(final ServletContextEvent arg0) {
    boolean activated = false;
    activated = Boolean.parseBoolean(ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_ACTIVATION));
    if (activated) {
      ActivateELK.getInstance().activate();
    }

  }

}