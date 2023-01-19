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

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class UniversalConnectorConfiguration extends AbstractConfigClass {

  // Config filename
  private static final String configFilename = "universal-connector.properties";

  // Properties
  public static final String STORAGE_FOLDER_PATH = "STORAGE_FOLDER_PATH";

  private static UniversalConnectorConfiguration instance;

  /**
   *
   * Get the instance
   *
   */
  public static synchronized UniversalConnectorConfiguration getInstance() {
    if (null == instance) {
      instance = new UniversalConnectorConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private UniversalConnectorConfiguration() {

    super(configFilename, LogManager.getLogger(UniversalConnectorConfiguration.class.getName()));
  }

}