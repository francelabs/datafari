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
package com.francelabs.datafari.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class WidgetManagerConfiguration extends AbstractConfigClass {

  // Config filename
  private static final String configFilename = "widget-manager.properties";

  // Properties
  public static final String TIKA_SERVER_HOST = "TIKASERVERHOST";
  public static final String TIKA_SERVER_PORT = "TIKASERVERPORT";
  public static final String WRITE_LIMIT = "WRITELIMIT";
  public static final String LOWER_NAMES = "LOWERNAMES";

  private static WidgetManagerConfiguration instance;

  /**
   *
   * Get the instance
   *
   */
  public static synchronized WidgetManagerConfiguration getInstance() {
    if (null == instance) {
      instance = new WidgetManagerConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private WidgetManagerConfiguration() {
    super(configFilename, LogManager.getLogger(WidgetManagerConfiguration.class.getName()));
  }

}