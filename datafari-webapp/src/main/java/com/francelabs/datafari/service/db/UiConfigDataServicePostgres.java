package com.francelabs.datafari.service.db;

import java.sql.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class UiConfigDataServicePostgres {

    final static Logger logger = LogManager.getLogger(UiConfigDataServicePostgres.class.getName());

    public static final String USERNAMECOLUMN = "username";
    public static final String UICONFIGCOLLECTION = "ui_config";
    public static final String UICONFIGCOLUMN = "ui_config";
    public final static String LASTREFRESHCOLUMN = "last_refresh";

    private final String userDataTTL;
    private static UiConfigDataServicePostgres instance;

    private final PostgresService pgService = new PostgresService();

    public static synchronized UiConfigDataServicePostgres getInstance() {
        if (instance == null) {
            instance = new UiConfigDataServicePostgres();
        }
        return instance;
    }

    private UiConfigDataServicePostgres() {
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
            final String sql = "SELECT " + UICONFIGCOLUMN + " FROM " + UICONFIGCOLLECTION +
                    " WHERE " + USERNAMECOLUMN + " = ?";
            try (ResultSet result = pgService.executeSelect(sql, username)) {
                if (result.next()) {
                    uiConfig = result.getString(UICONFIGCOLUMN);
                }
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
            String sql = "INSERT INTO " + UICONFIGCOLLECTION + " (" + USERNAMECOLUMN + ", " + UICONFIGCOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET " + UICONFIGCOLUMN + " = EXCLUDED." + UICONFIGCOLUMN + ", " +
                    LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;
            // TTL not native in PG, can be enforced by external script if needed
            pgService.executeUpdate(sql, username, uiConfig, new Timestamp(System.currentTimeMillis()));
        } catch (final Exception e) {
            logger.warn("Unable to insert ui config for user " + username + " : " + e.getMessage());
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
            String sql = "UPDATE " + UICONFIGCOLLECTION + " SET " +
                    UICONFIGCOLUMN + " = ?, " +
                    LASTREFRESHCOLUMN + " = ? " +
                    "WHERE " + USERNAMECOLUMN + " = ?";
            // TTL not native in PG, can be enforced by external script if needed
            pgService.executeUpdate(sql, uiConfig, new Timestamp(System.currentTimeMillis()), username);
        } catch (final Exception e) {
            logger.warn("Unable to update ui config for user " + username + " : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        return CodesReturned.ALLOK.getValue();
    }

    public void refreshUiConfig(final String username) throws DatafariServerException {
        final String userUiConfig = getUiConfig(username);
        if (userUiConfig != null) {
            updateUiConfig(username, userUiConfig);
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
            final String sql = "DELETE FROM " + UICONFIGCOLLECTION +
                    " WHERE " + USERNAMECOLUMN + " = ?";
            pgService.executeUpdate(sql, username);
        } catch (final Exception e) {
            logger.warn("Unable to delete ui config for user " + username + " : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        return CodesReturned.ALLOK.getValue();
    }
}