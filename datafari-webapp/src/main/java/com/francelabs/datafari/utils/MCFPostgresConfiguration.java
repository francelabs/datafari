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
public class MCFPostgresConfiguration extends AbstractConfigClass {

  // Properties
  public static final String HOST = "HOST";
  public static final String PORT = "PORT";
  public static final String DATABASE = "DATABASE";
  public static final String USER = "USER";
  public static final String PASSWORD = "PASSWORD";
  public static final String TABLE_NAME = "TABLENAME";

  // Config filename
  private static final String configFilename = "mcf-postgres.properties";

  private static MCFPostgresConfiguration instance;

  /**
   *
   * Get the instance
   *
   */
  public synchronized static MCFPostgresConfiguration getInstance() {
    if (null == instance) {
      instance = new MCFPostgresConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private MCFPostgresConfiguration() {

    super(configFilename, LogManager.getLogger(MCFPostgresConfiguration.class.getName()));
  }

}