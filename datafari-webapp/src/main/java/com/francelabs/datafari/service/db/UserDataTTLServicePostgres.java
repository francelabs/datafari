package com.francelabs.datafari.service.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.DatafariServerException;

public class UserDataTTLServicePostgres {

  final static Logger logger = LogManager.getLogger(UserDataTTLServicePostgres.class.getName());

  public static void refreshUserDataTTL(final String username) {
    try {
      UserDataServicePostgres.getInstance().refreshUser(username);
      LangDataServicePostgres.getInstance().refreshLang(username);
      AlertDataServicePostgres.getInstance().refreshUserAlerts(username);
      DepartmentDataServicePostgres.getInstance().refreshDepartment(username);
      DocumentDataServicePostgres.getInstance().refreshLikes(username);
      DocumentDataServicePostgres.getInstance().refreshFavorites(username);
      SavedSearchDataServicePostgres.getInstance().refreshSavedSearches(username);
      AccessTokenDataServicePostgres.getInstance().refreshAccessTokens(username);
      UiConfigDataServicePostgres.getInstance().refreshUiConfig(username);
    } catch (final DatafariServerException e) {
      logger.error("Unable to refresh user '" + username + "' data TTL", e);
    }
  }
}