/*******************************************************************************
 * Copyright 2020 France Labs
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

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class GDPRConfiguration extends AbstractConfigClass {

  // Config filename
  private static final String configFilename = "gdpr.properties";

  // Properties
  public static final String USER_DATA_TTL = "USER_DATA_TTL";
  public static final String USER_ACTIONS_TTL = "USER_ACTIONS_TTL";

  private static GDPRConfiguration instance;

  /**
   *
   * Get the instance
   *
   */
  public static synchronized GDPRConfiguration getInstance() {
    if (null == instance) {
      instance = new GDPRConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private GDPRConfiguration() {

    super(configFilename, LogManager.getLogger(GDPRConfiguration.class.getName()));
  }

}