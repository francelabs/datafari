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
public class UsageStatisticsConfiguration extends AbstractConfigClass {

  // Properties frequencies
  public final static String ENABLED = "ENABLED";

  private static final String configFilename = "usage_statistics.properties";

  private static UsageStatisticsConfiguration instance;

  private final static Logger LOGGER = LogManager.getLogger(UsageStatisticsConfiguration.class.getName());

  /**
   *
   * Get the instance
   *
   */
  public static synchronized UsageStatisticsConfiguration getInstance() {
    if (null == instance) {
      instance = new UsageStatisticsConfiguration();
    }
    return instance;
  }

  private UsageStatisticsConfiguration() {
    super(configFilename, LOGGER);
  }

}