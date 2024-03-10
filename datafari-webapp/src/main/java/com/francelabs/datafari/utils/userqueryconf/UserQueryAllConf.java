package com.francelabs.datafari.utils.userqueryconf;

import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * For Solr search query user configurations.
 * Manages all types of user configuration.
 */
public class UserQueryAllConf {
  private static UserPrefQueryConf prefConf = UserPrefQueryConf.getInstance();



  /**
   * Apply user's specific query config (specific boosts related to user context) on the request.
   * Set Http request with Solr Query Parser Parameters using API request syntax.
   *
   * @param request the original request
   */
  public static void apply(final HttpServletRequest request) {
    prefConf.applyUserQueryConf(request);
  }

}
