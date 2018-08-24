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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class HighlightConfiguration extends AbstractConfigClass {

  // Properties
  public static final String MAX_ANALYZED_CHARS = "MAXANALYZEDCHARS";

  private static final String configFilename = "highlight.properties";

  private static HighlightConfiguration instance;

  private final static Logger LOGGER = LogManager.getLogger(HighlightConfiguration.class.getName());

  /**
   *
   * Get the instance
   *
   */
  public static synchronized HighlightConfiguration getInstance() throws IOException {
    if (null == instance) {
      instance = new HighlightConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private HighlightConfiguration() throws IOException {
    super(configFilename, LOGGER);

  }

}