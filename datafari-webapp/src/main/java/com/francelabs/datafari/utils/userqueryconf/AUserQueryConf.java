package com.francelabs.datafari.utils.userqueryconf;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.SolrAPI;
import com.francelabs.datafari.utils.Timer;
import com.sun.istack.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;

public abstract class AUserQueryConf {
  public abstract Logger getLogger();

  /**
   * Related to getUserQueryConf method that returns the query configuration in json format, gets the attributes for of each pair.
   * For example: qf, pf...
   *
   * @return attributes used to set Http request attributes with the Solr query parameter.
   */
  protected abstract String[] getQueryConfKeys();


  /**
   * Get the user specific query configuration, aimed to create a Solr Query.
   *
   * @param request
   * @return if any, returns a json formatted configuration like this: {"qf":"...","pf":"...","bq":"...",...}.
   * @throws DatafariServerException
   * @throws IOException
   */
  public @NotNull String getUserQueryConf(final HttpServletRequest request) throws DatafariServerException, IOException {
    request.setCharacterEncoding("utf8");
    final Principal userPrincipal = request.getUserPrincipal();
    // checking if the user is connected
    if (userPrincipal == null) {
      return "";
    }

    return getUserQueryConf(request.getSession(), userPrincipal.getName());
  }
  protected abstract @NotNull String getUserQueryConf(final HttpSession session, final String username) throws DatafariServerException, IOException;

    /**
     * Apply user's specific query config on the request (specific boosts related to user context).
     *
     * @param request the original request
     */
  public void applyUserQueryConf(final HttpServletRequest request) {
    Timer timer = new Timer(this.getClass().getName(), "applyUserQueryConf");

    final String userConf;
    try {
      userConf = getUserQueryConf(request);

      if (userConf != null && !userConf.isEmpty()) {
        final JSONParser parser = new JSONParser();
        try {
          final JSONObject jsonConf = (JSONObject) parser.parse(userConf);

          String solrQueryParamValue;
          for (String solrQueryParamName : getQueryConfKeys()){
            solrQueryParamValue = (String) jsonConf.get(solrQueryParamName);

            if (StringUtils.isNotBlank(solrQueryParamValue)) {
              request.setAttribute(solrQueryParamName, solrQueryParamValue);
            }
          }

        } catch (final ParseException e) {
          getLogger().warn("An issue has occured while reading user query conf", e);
        }
      }

    } catch (DatafariServerException e) {
      getLogger().error("Impossible to retrieve User query configuration due to internal database error (see previous errors reported).");
    } catch (IOException e) {
      getLogger().error("Impossible to retrieve User query configuration (see previous errors reported).");
    } finally {
      timer.stop();
    }
  }
}
