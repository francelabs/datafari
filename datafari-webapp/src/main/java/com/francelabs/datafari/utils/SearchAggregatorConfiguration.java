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
public class SearchAggregatorConfiguration extends AbstractConfigClass {

  // Config filename
  private static final String configFilename = "search-aggregator.properties";

  // Properties
  public static final String EXTERNAL_DATAFARIS = "EXTERNAL_DATAFARIS";
  public static final String ACTIVATED = "ACTIVATED";
  public static final String TIMEOUT_PER_REQUEST = "TIMEOUT_PER_REQUEST";
  public static final String GLOBAL_TIMEOUT = "GLOBAL_TIMEOUT";
  public static final String DEFAULT_DATAFARI = "DEFAULT_DATAFARI";
  public static final String USERS_ALLOWED_SOURCES_FILE = "USERS_ALLOWED_SOURCES_FILE";
  public static final String USERS_DEFAULT_SOURCE_FILE = "USERS_DEFAULT_SOURCE_FILE";


  private static SearchAggregatorConfiguration instance;

  /**
   *
   * Get the instance
   *
   */
  public static synchronized SearchAggregatorConfiguration getInstance() {
    if (null == instance) {
      instance = new SearchAggregatorConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private SearchAggregatorConfiguration() {

    super(configFilename, LogManager.getLogger(SearchAggregatorConfiguration.class.getName()));
  }

}