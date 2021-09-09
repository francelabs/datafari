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

import java.io.File;
import java.time.format.DateTimeFormatter;

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class ClusterActionsConfiguration extends AbstractConfigClass {

  // Properties
  public static final String LAST_RESTART_DATE = "LAST_RESTART_DATE";
  public static final String LAST_RESTART_USER = "LAST_RESTART_USER";
  public static final String LAST_RESTART_IP = "LAST_RESTART_IP";
  public static final String LAST_RESTART_REPORT = "LAST_RESTART_REPORT";
  public static final String LAST_REINIT_DATE = "LAST_REINIT_DATE";
  public static final String LAST_REINIT_USER = "LAST_REINIT_USER";
  public static final String LAST_REINIT_IP = "LAST_REINIT_IP";
  public static final String LAST_REINIT_REPORT = "LAST_REINIT_REPORT";
  public static final String LAST_BACKUP_DATE = "LAST_BACKUP_DATE";
  public static final String LAST_BACKUP_USER = "LAST_BACKUP_USER";
  public static final String LAST_BACKUP_IP = "LAST_BACKUP_IP";
  public static final String LAST_BACKUP_REPORT = "LAST_BACKUP_REPORT";
  public static final String FORCE_UNMANAGED_STATE = "FORCE_UNMANAGED_STATE";
  public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_INSTANT;
  

  private static final String RELATIVE_PATH = "bin" + File.separator + "common" + File.separator + "cluster-actions.properties";
  private static final String BASE_PATH = Environment.getEnvironmentVariable("DATAFARI_HOME") == null
      ? File.separator + "opt" + File.separator + "datafari"
      : Environment.getEnvironmentVariable("DATAFARI_HOME");

  private final static Logger LOGGER = LogManager.getLogger(ClusterActionsConfiguration.class.getName());

  private static ClusterActionsConfiguration instance;

  /**
   *
   * Get the instance
   *
   */
  public static synchronized ClusterActionsConfiguration getInstance() {
    if (null == instance) {
      instance = new ClusterActionsConfiguration();
    }
    return instance;
  }

  private ClusterActionsConfiguration() {
    super(RELATIVE_PATH, BASE_PATH + File.separator + RELATIVE_PATH, LOGGER);
  }

}