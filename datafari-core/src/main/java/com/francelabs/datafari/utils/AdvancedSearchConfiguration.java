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

import org.apache.log4j.Logger;

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class AdvancedSearchConfiguration extends AbstractConfigClass {

  // Config filename
  private static final String configFilename = "advanced-search.properties";

  // Properties
  public static final String DENIED_FIELD_LIST = "DENIEDFIELDLIST";
  public static final String EXACT_FIELDS = "EXACTFIELDS";
  public static final String AUTOCOMPLETE_FIELDS = "AUTOCOMPLETEFIELDS";
  public static final String FIXEDVALUES_FIELDS = "FIXEDVALUESFIELDS";

  private static AdvancedSearchConfiguration instance;

  /**
   *
   * Get the instance
   *
   */
  public static synchronized AdvancedSearchConfiguration getInstance() {
    if (null == instance) {
      instance = new AdvancedSearchConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private AdvancedSearchConfiguration() {

    super(configFilename, Logger.getLogger(AdvancedSearchConfiguration.class.getName()));
  }

}