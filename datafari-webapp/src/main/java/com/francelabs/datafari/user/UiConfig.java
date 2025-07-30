package com.francelabs.datafari.user;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UiConfigDataServicePostgres;

public class UiConfig {
    /**
     * Get the user specific ui config
     *
     * @param username
     *                 of the user
     * @return the prefered language.
     * @throws DatafariServerException
     */
    public static String getUiConfig(final String username) throws DatafariServerException {
        return UiConfigDataServicePostgres.getInstance().getUiConfig(username);
    }

    /**
     * Set user's specific ui config
     *
     * @param username
     * @param uiConfig
     * @return CodesReturned.ALLOK if all was ok
     * @throws DatafariServerException
     */
    public static int setUiConfig(final String username, final String uiConfig) throws DatafariServerException {
        return UiConfigDataServicePostgres.getInstance().setUiConfig(username, uiConfig);
    }

    /**
     * Update user specific ui config
     *
     * @param username
     * @param uiConfig
     * @return CodesReturned.ALLOK if all was ok
     * @throws DatafariServerException
     */
    public static int updateUiConfig(final String username, final String uiConfig) throws DatafariServerException {
        return UiConfigDataServicePostgres.getInstance().updateUiConfig(username, uiConfig);
    }

    /**
     * Delete user specific ui config
     *
     * @param username
     * @return CodesReturned.ALLOK if all was ok
     * @throws DatafariServerException
     */
    public static int deleteUiConfig(final String username) throws DatafariServerException {
        return UiConfigDataServicePostgres.getInstance().deleteUiConfig(username);
    }
}
