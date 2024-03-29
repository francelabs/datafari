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
public class SolrConfiguration extends AbstractConfigClass {

  // Properties
  public static final String SOLRHOST = "SOLRHOST";
  public static final String SOLRWEBAPP = "SOLRWEBAPP";
  public static final String SOLRPORT = "SOLRPORT";
  public static final String ZOOKEEPERPORT = "ZOOKEEPERPORT";
  public static final String SOLRPROTOCOL = "SOLRPROTOCOL";
  public static final String FILESTONOTBEUPLOADED = "FILESTONOTBEUPLOADED";

  // Config filename
  private static final String configFilename = "solr.properties";

  private static SolrConfiguration instance;

  private final static Logger LOGGER = LogManager.getLogger(SolrConfiguration.class.getName());

  /**
   *
   * Get the instance
   *
   */
  public synchronized static SolrConfiguration getInstance() {
    if (null == instance) {
      instance = new SolrConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private SolrConfiguration() {
    super(configFilename, Environment.getEnvironmentVariable("DATAFARI_SOLR_PROPERTIES_HOME") + File.separator + configFilename, LOGGER);

  }

}