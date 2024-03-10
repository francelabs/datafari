package com.francelabs.datafari.utils.userqueryconf;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UiConfigDataService;
import com.francelabs.datafari.utils.SolrAPI;
import com.sun.istack.NotNull;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

public class UserPrefQueryConf extends AUserQueryConf {
  private static UserPrefQueryConf instance = null;
  private static final Logger logger = LogManager.getLogger(UserPrefQueryConf.class);


  public static final String SESSION_USER_QUERY_PREFERENCES = "query_preferences";

  /**
   * Used to extract in user preferences, the Solr fields to be boosted.
   * The Map key is the UI name of the field. It's like an alias in the UI for the Solr field.
   * The Map value is the Solr field name associated.
   */
  private static final Map<String, String> USER_QUERY_PREF_MAPPER = new HashMap<>();

  static {
    USER_QUERY_PREF_MAPPER.put("sources", "repo_source");
    USER_QUERY_PREF_MAPPER.put("langues", "language");
    USER_QUERY_PREF_MAPPER.put("extension", "extension");
  }

  private UserPrefQueryConf(){}
  public static UserPrefQueryConf getInstance(){
    if (instance == null){
      instance = new UserPrefQueryConf();
    }
    return instance;
  }


  /**
   * Call this method to trigger the user's query configuration related to its preferences.
   *
   * @param request the current http request
   */
  public static void triggerUpdateUserQueryPreferences(final HttpServletRequest request){
    request.getSession().removeAttribute(SESSION_USER_QUERY_PREFERENCES);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected String[] getQueryConfKeys() {
    return new String[]{SolrAPI.QUERY_BQ};
  }

  /**
   * Get the user specific query configuration, aimed to create a Solr Query.
   * Set a session attribute pour the query configuration if not yet done.
   *
   * @param session
   * @param username
   * @return
   * @throws DatafariServerException
   * @throws IOException
   */
  @Override
  protected @NotNull String getUserQueryConf(final HttpSession session, final String username) throws DatafariServerException, IOException {

    String queryPreferences = (String) session.getAttribute(SESSION_USER_QUERY_PREFERENCES);
    if (queryPreferences == null) {
      queryPreferences = buildUserQueryByPreferences(username);
      session.setAttribute(SESSION_USER_QUERY_PREFERENCES, queryPreferences);
    }

    return "{" + queryPreferences + "}";

  }

  /**
   * Build the Solr query parameter related to the user preferences (chosen in Datafari's UI).
   * Create a "bq" query parameter. Example: "bq":"repo_source:(source_3^1 OR source_2^2 OR source_1^3) langue:(fr^2 OR en^1)"
   *
   * @param username
   * @return the json formatted Solr query parameter for the given user. Return an empty string if no configuration found
   * @throws DatafariServerException if an error occur while getting {@link UiConfigDataService} instance.
   */
  private @NotNull String buildUserQueryByPreferences(String username) throws DatafariServerException {
    String uiConfig = UiConfigDataService.getInstance().getUiConfig(username);
    if (org.apache.commons.lang.StringUtils.isBlank(uiConfig)){
      return "";
    }

    JSONParser jsonParser = new JSONParser();
    StringBuilder result = new StringBuilder();
    try {
      JSONObject jsonUiConfig = (JSONObject) jsonParser.parse(uiConfig);
      JSONArray preferenceValues;
      String solrField;
      StringBuilder queryFieldPart;
      for(Map.Entry<String, String> userPrefMapEntry: USER_QUERY_PREF_MAPPER.entrySet()) {
        preferenceValues = (JSONArray) jsonUiConfig.get(userPrefMapEntry.getKey());
        if (preferenceValues != null && preferenceValues.size()>0){
          solrField = userPrefMapEntry.getValue();

          // Compute query parameter for this solrField: field:(value1^weight1 OR value2^weight2 OR...)
          queryFieldPart = computeQueryBoostParameterField(solrField, preferenceValues);
          result.append(queryFieldPart).append(" ");
        }
      }
    } catch (ParseException e) {
      logger.error("Searching for user Solr query boost by preferences, impossible to retrieve user preferences " +
          "from the retrieved JSON:\n" + uiConfig, e);
    }

    if (StringUtils.isNotBlank(result.toString())){
      // Complete configuration syntax: {"bq":"field:(value1^weight1...)"}
      result.insert(0, "\""+ SolrAPI.QUERY_BQ +"\":\"");
      result.append("\"");
    }
    return result.toString();
  }

  /**
   * Creates one "bq" field parameter, for example: repo_source:(source_3^1 OR source_2^2 OR source_1^3)
   * Conforms to Solr syntax for "bq" Boost Query parameter.
   *
   * @param solrField the solr field to compute the configuration for. In the example above solrField=repo_source
   * @param orderedValues the field values ordered to have the biggest weight first and then decrease. In the example above
   *                      orderedValues=[source_3, source_2, source_1]. The weights 3, 2 and 1 will be assigned.
   * @return the field configuration for the "bq" parameter.
   */
  private StringBuilder computeQueryBoostParameterField(String solrField, JSONArray orderedValues){
    StringBuilder result = new StringBuilder(solrField).append(":(");
    int nbValues = orderedValues.size();
    for (Object value : orderedValues) {
      result.append(value).append("^").append(nbValues);
      nbValues--;
      if (nbValues > 0){
        result.append(" OR ");
      }
    }
    result.append(")");
    return result;
  }

}
