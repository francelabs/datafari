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
public class ELKConfiguration extends AbstractConfigClass {

  // Properties
  public static final String ELK_ACTIVATION = "ELKactivation";
  public static final String SERVER_TYPE = "NODETYPE";
  private static final String configFilename = "datafari.properties";

  private static ELKConfiguration instance;

  private final static Logger LOGGER = LogManager.getLogger(ELKConfiguration.class.getName());

  /**
   *
   * Get the instance
   *
   */
  public static synchronized ELKConfiguration getInstance() {
    if (null == instance) {
      instance = new ELKConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private ELKConfiguration() {
    super(configFilename, LOGGER);

  }

}