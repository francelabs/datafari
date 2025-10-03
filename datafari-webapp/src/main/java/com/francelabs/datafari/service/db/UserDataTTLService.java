package com.francelabs.datafari.service.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.DatafariServerException;

public class UserDataTTLService {

  final static Logger logger = LogManager.getLogger(UserDataTTLService.class.getName());

  public static void refreshUserDataTTL(final String username) {
    try {
      UserDataService.getInstance().refreshUser(username);
      LangDataService.getInstance().refreshLang(username);
      AlertDataService.getInstance().refreshUserAlerts(username);
      DepartmentDataService.getInstance().refreshDepartment(username);
      DocumentDataService.getInstance().refreshLikes(username);
      DocumentDataService.getInstance().refreshFavorites(username);
      SavedSearchDataService.getInstance().refreshSavedSearches(username);
      AccessTokenDataService.getInstance().refreshAccessTokens(username);
      UiConfigDataService.getInstance().refreshUiConfig(username);
    } catch (final DatafariServerException e) {
      logger.error("Unable to refresh user '" + username + "' data TTL", e);
    }
  }
}