package com.francelabs.datafari.utils.userqueryconf;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UiConfigDataService;
import com.francelabs.datafari.utils.SolrAPI;
import com.sun.istack.NotNull;
import org.apache.commons.lang3.StringUtils;
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
    return new String[]{SolrAPI.QUERY_BOOST};
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
   * Create a "boost" query parameter. Example: "boost":"product( product( if( eq(repo_source, 'source2'),300,1 ), if( eq(repo_source, 'source3'),200,1 )), if( eq(repo_source, 'enron'),100,1 )"
   * Apply product operation as many times as necessary to combine conditions on fields.
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
        // Must have more than 1 preference value to require boosting.
        if (preferenceValues != null && preferenceValues.size()>1){
          solrField = userPrefMapEntry.getValue();

          // Compute query parameter for this solrField: product( product( if( eq(field,value1), weight1,1), if( eq(field,value2), weight2, 1) ) ), if(...) )
          queryFieldPart = computeQueryBoostParameterField(solrField, preferenceValues);
          // Wrap into "product" function when 2 values are available.
          result = wrapProduct(result, queryFieldPart);
        }
      }
    } catch (ParseException e) {
      logger.error("Searching for user Solr query boost by preferences, impossible to retrieve user preferences " +
          "from the retrieved JSON:\n" + uiConfig, e);
    }

    if (StringUtils.isNotBlank(result.toString())){
      // Complete configuration syntax:
      //   - result comes with functions_conditions.
      //   - we must wrap functions_conditions with: "boost":"functions_conditions"
      result.insert(0, "\""+ SolrAPI.QUERY_BOOST +"\":\"");
      result.append("\"");
    }
    return result.toString();
  }

  /**
   * Creates one "boost" query field parameter, with "if" function to give a weight to the value of the field, and "product" function to wrap "if" functions and "product"
   * to combine like this : product( product( if( eq(repo_source, 'source2'),30,1 ), if( eq(repo_source, 'source3'),20,1 )), if( eq(repo_source, 'enron'),10,1 )
   *
   * @param solrField the solr field to compute the configuration for. In the example above solrField=repo_source
   * @param orderedValues the field values ordered to have the biggest weight first and then decrease. In the example above
   *                      orderedValues=[source_3, source_2, source_1]. The weights 3*X, 2*X and 1*X will be assigned.
   * @return a nested sequence of IFs and Products for the given solrField. In another word, combination of "product" and "if" functions for a field and its values/weight.
   */
  private StringBuilder computeQueryBoostParameterField(String solrField, JSONArray orderedValues){
    StringBuilder result = new StringBuilder();
    int nbValues = orderedValues.size();
    StringBuilder ifFunction;
    for (Object value : orderedValues) {
      // create "if" function
      ifFunction = createIF(solrField, value.toString(), nbValues);
      // Wrap into "product" function when 2 values are available.
      result = wrapProduct(result, ifFunction);

      nbValues--;
    }
    return result;
  }

  /**
   * Create the "if" function
   *
   * @param solrField
   * @param value
   * @param initialWeight
   * @return
   */
  private StringBuilder createIF(String solrField, String value, int initialWeight){
    StringBuilder ifFunction = new StringBuilder("if(eq(");
    ifFunction.append(solrField).append(",'").append(value).append("'),").append(initialWeight*10).append(",1)");
    return ifFunction;
  }

  /**
   * Wrap into a "product" function the 2 given values. Resulting in a multiplication between first value and second value in a query boost.
   *
   * @param firstValue
   * @param secondValue
   * @return the String "product(firstValue, secondValue)" if the 2 values are filled. Otherwise, returns the second value if filled.
   * Otherwise, returns the firstValue.
   */
  private StringBuilder wrapProduct(StringBuilder firstValue, StringBuilder secondValue){
    if (StringUtils.isNotBlank(firstValue) && StringUtils.isNotBlank(secondValue)) {
      firstValue.insert(0, "product( ").append(", ").append(secondValue).append(" )");
    } else if (StringUtils.isNotBlank(secondValue)) {
      firstValue = secondValue;
    }

    return firstValue;
  }

}
