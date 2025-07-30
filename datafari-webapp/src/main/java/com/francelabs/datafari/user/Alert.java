/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.user;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.AlertDataServicePostgres;

public class Alert {

  private final static Logger logger = LogManager.getLogger(Alert.class);

  /**
   * Add an alert to the user list of alerts
   *
   * @param alertProp
   *          the alert properties
   * @return the alert uuid
   * @throws DatafariServerException
   * @throws IOException
   */
  public static String addAlert(final Properties alertProp) throws DatafariServerException, IOException {
    return AlertDataServicePostgres.getInstance().addAlert(alertProp);
  }

  /**
   * delete an alert
   *
   * @param alertID
   *          the alert id
   * @throws IOException
   * @throws DatafariServerException
   */
  public static void deleteAlert(final String alertID) throws DatafariServerException, IOException {
    AlertDataServicePostgres.getInstance().deleteAlert(alertID);
  }

  /**
   * Get all the alerts
   *
   * @return the list of all alerts of all users
   * @throws DatafariServerException
   * @throws IOException
   */
  public static List<Properties> getAlerts() throws DatafariServerException, IOException {
    return AlertDataServicePostgres.getInstance().getAlerts();
  }

  /**
   * delete all user alerts
   *
   * @param username
   *          the username
   * @throws IOException
   * @throws DatafariServerException
   */
  public static void deleteAllAlerts(final String username) throws DatafariServerException {
    AlertDataServicePostgres.getInstance().deleteUserAlerts(username);
  }

}
