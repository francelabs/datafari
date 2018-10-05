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
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class DatafariMainConfiguration extends AbstractConfigClass {

  private static final String configFilename = "datafari.properties";

  private static DatafariMainConfiguration instance;
  public static final String LDAPACTIVATED = "ISLDAPACTIVATED";
  public static final String SOLR_HOSTS = "SOLRHOSTS";
  public static final String ONTOLOGY_ENABLED = "ontologyEnabled";
  public static final String ONTOLOGY_LANGUAGE_SELECTION = "ontologyLanguageSelection";
  public static final String ONTOLOGY_PARENTS_LABELS = "ontologyParentsLabels";
  public static final String ONTOLOGY_CHILDREN_LABELS = "ontologyChildrenLabels";
  public static final String ALLOW_LOCAL_FILE_READING = "ALLOWLOCALFILEREADING";
  public final static String LIKESANDFAVORTES = "IS_LIKES_AND_FAVORITES_ENABLED";
  public final static String TEMP_ADMIN_PASSWORD = "TEMPADMINPASSWORD";

  private final static Logger LOGGER = LogManager.getLogger(DatafariMainConfiguration.class.getName());

  /**
   *
   * Get the instance
   *
   */
  public static synchronized DatafariMainConfiguration getInstance() {
    if (null == instance) {
      instance = new DatafariMainConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private DatafariMainConfiguration() {
    super(configFilename, LOGGER);

  }

  public List<String> getSolrHosts() throws IOException {
    final String strList = getProperty(SOLR_HOSTS);
    return Arrays.asList(strList.split(","));
  }

}