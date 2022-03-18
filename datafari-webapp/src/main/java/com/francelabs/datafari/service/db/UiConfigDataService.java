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
package com.francelabs.datafari.service.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class UiConfigDataService extends CassandraService {
    final static Logger logger = LogManager.getLogger(UiConfigDataService.class.getName());

    public static final String USERNAMECOLUMN = "username";
    public static final String UICONFIGCOLLECTION = "ui_config";
    public static final String UICONFIGCOLUMN = "ui_config";
    public final static String LASTREFRESHCOLUMN = "last_refresh";

    private final String userDataTTL;

    private static UiConfigDataService instance;

    public static synchronized UiConfigDataService getInstance() throws DatafariServerException {
        try {
            if (instance == null) {
                instance = new UiConfigDataService();
            }
            instance.refreshSession();
            return instance;
        } catch (final DriverException e) {
            logger.warn("Unable to get instance : " + e.getMessage());
            // TODO catch specific exception
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    private UiConfigDataService() {
        refreshSession();
        userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
    }

    /**
     * Get user specific ui configuration
     *
     * @param username
     * @return the user specific ui configuration
     */
    public synchronized String getUiConfig(final String username) {
        String uiConfig = null;
        try {
            final String query = "SELECT " + UICONFIGCOLUMN + " FROM " + UICONFIGCOLLECTION + " where " + USERNAMECOLUMN + "='"
                    + username + "'";
            final ResultSet result = session.execute(query);
            final Row row = result.one();
            if (row != null && !row.isNull(UICONFIGCOLUMN) && !row.getString(UICONFIGCOLUMN).isEmpty()) {
                uiConfig = row.getString(UICONFIGCOLUMN);
            }
        } catch (final Exception e) {
            logger.warn("Unable to get ui config for user " + username + " : " + e.getMessage());
        }
        return uiConfig;
    }

    /**
     * Set user ui config
     *
     * @param username
     * @param uiConfig
     * @return CodesReturned.ALLOK if all was ok
     * @throws DatafariServerException
     */
    public int setUiConfig(final String username, final String uiConfig) throws DatafariServerException {
        try {
            String ttlToUse = userDataTTL;
            if (username.contentEquals("admin")) {
                ttlToUse = "0";
            }
            final String query = "INSERT INTO " + UICONFIGCOLLECTION + " (" + USERNAMECOLUMN + "," + UICONFIGCOLUMN + ","
                    + LASTREFRESHCOLUMN + ")" + " values ('" + username + "','" + uiConfig
                    + "',toTimeStamp(NOW())) USING TTL " + ttlToUse;
            session.execute(query);
        } catch (final Exception e) {
            logger.warn("Unable to insert ui config for user " + username + " : " + e.getMessage());
            // TODO catch specific exception
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        return CodesReturned.ALLOK.getValue();
    }

    /**
     * Update user ui config
     *
     * @param username
     * @param uiConfig
     * @return CodesReturned.ALLOK if all was ok
     * @throws DatafariServerException
     */
    public int updateUiConfig(final String username, final String uiConfig) throws DatafariServerException {
        try {
            final String query = "UPDATE " + UICONFIGCOLLECTION + " SET " + UICONFIGCOLUMN + " = '" + uiConfig + "' WHERE "
                    + USERNAMECOLUMN + " = '" + username + "'";
            session.execute(query);
        } catch (final Exception e) {
            logger.warn("Unable to update ui config for user " + username + " : " + e.getMessage());
            // TODO catch specific exception
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        return CodesReturned.ALLOK.getValue();
    }

    public void refreshUiConfig(final String username) throws DatafariServerException {
        final String userUiConfig = getUiConfig(username);
        if (userUiConfig != null) {
            deleteUiConfig(username);
            setUiConfig(username, userUiConfig);
        }
    }

    /**
     *
     * @param username
     * @return CodesReturned.ALLOK value if all was ok
     * @throws DatafariServerException
     */
    public int deleteUiConfig(final String username) throws DatafariServerException {
        try {
            final String query = "DELETE FROM " + UICONFIGCOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username
                    + "' IF EXISTS";
            session.execute(query);
        } catch (final Exception e) {
            logger.warn("Unable to update ui config for user " + username + " : " + e.getMessage());
            // TODO catch specific exception
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        return CodesReturned.ALLOK.getValue();
    }
}
