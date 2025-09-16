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
  public static final String ZK_HOSTS = "SOLRHOSTS";
  public static final String ONTOLOGY_ENABLED = "ontologyEnabled";
  public static final String ONTOLOGY_LANGUAGE_SELECTION = "ontologyLanguageSelection";
  public static final String ONTOLOGY_PARENTS_LABELS = "ontologyParentsLabels";
  public static final String ONTOLOGY_CHILDREN_LABELS = "ontologyChildrenLabels";
  public final static String LIKESANDFAVORTES = "IS_LIKES_AND_FAVORITES_ENABLED";
  public final static String TEMP_ADMIN_PASSWORD = "TEMPADMINPASSWORD";
  public static final String SOLR_MAIN_COLLECTION = "SOLRMAINCOLLECTION";
  public static final String SOLR_SECONDARY_COLLECTIONS = "SOLRSECONDARYCOLLECTION";
  public static final String EMAIL_LICENSE = "emailLicense";
  public static final String USER_ALLOWED_HANDLERS = "userAllowedHandlers";
  public static final String SESSION_TIMEOUT_UNAUTH = "sessionTimeoutUnauthenticated";
  public static final String SESSION_TIMEOUT_AUTH = "sessionTimeoutAuthenticated";
  public static final String MAX_CONCURRENT_SESSIONS = "maxConcurrentSessions";
  public static final String EMAIL_DPO = "email_dpo";
  public static final String EMAIL_FEEDBACKS = "email_feedbacks";
  public static final String EMAIL_BUGS = "email_bugs";
  public static final String MCF_PASSWORD = "MCFPASSWORD";
  public static final String ANALYTICS_ACTIVATION = "AnalyticsActivation";
  public static final String MONIT_STATE = "MONIT_STATE";
  public static final String TIKASERVER_ANNOTATOR= "TIKASERVER_ANNOTATOR";
  public static final String TIKASERVER_OCR= "TIKASERVER_OCR";
  public static final String ENABLE_MONITORING_TIMER = "ENABLE_MONITORING_TIMER";
  public static final String CUSTOM_PROXY_URL = "custom_proxy_url";
  public static final String ALLOWED_PROTOCOLS_URL = "ALLOWED_PROTOCOLS_URL";

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
    super(configFilename, Environment.getEnvironmentVariable("MAIN_DATAFARI_CONFIG_HOME") + File.separator + configFilename, LOGGER);

  }

  public List<String> getZkHosts() throws IOException {
    final String strList = getProperty(ZK_HOSTS);
    return Arrays.asList(strList.split(","));
  }

}