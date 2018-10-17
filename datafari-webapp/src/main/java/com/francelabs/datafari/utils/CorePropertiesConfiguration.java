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

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class CorePropertiesConfiguration extends AbstractConfigClass {

  // Config filename
  private static final String configFilename = "core.properties";
  public final static String DEDUPLICATION = "DEDUPLICATION_IS_ENABLED";
  public final static String DEDUPLICATION_FACTORY = "deduplication.factory.enabled";

  private static CorePropertiesConfiguration instance;

  private final static Logger LOGGER = LogManager.getLogger(CorePropertiesConfiguration.class.getName());

  /**
   *
   * Get the instance
   *
   */
  public static synchronized CorePropertiesConfiguration getInstance() {
    if (null == instance) {
      instance = new CorePropertiesConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private CorePropertiesConfiguration() {
    super(configFilename, Environment.getEnvironmentVariable("SOLR_INSTALL_DIR") + File.separator + "solrcloud" + File.separator + "FileShare" + File.separator + configFilename, LOGGER);

  }

}
