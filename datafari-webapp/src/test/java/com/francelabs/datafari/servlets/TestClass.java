package com.francelabs.datafari.servlets;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.userqueryconf.UserPrefQueryConf;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//FIXME to remove
public class TestClass {
  private static final Map<String, String> USER_QUERY_PREF_MAPPER = new HashMap<>();
  private static final Logger logger = LogManager.getLogger(TestClass.class);

  static {
    USER_QUERY_PREF_MAPPER.put("sources", "repo_source");
    USER_QUERY_PREF_MAPPER.put("langues", "language");
    USER_QUERY_PREF_MAPPER.put("extensions", "extension");
  }

  private static FileReader uiConfig;
  static {
    try {
      uiConfig = new FileReader("/home/guylaine/IdeaProjects/datafariee/datafari-ee/datafari-webapp-ee/src/main/java/com/francelabs/datafari/servlets/AdminUIconf.json");
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  @Test
  public void monTestJSON() throws IOException, ParseException {
    JSONParser jsonParser = new JSONParser();
    JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("/home/guylaine/IdeaProjects/datafariee/datafari-ee/datafari-webapp-ee/src/main/java/com/francelabs/datafari/servlets/AdminUIconf.json"));
    JSONArray preferenceValues = (JSONArray) jsonObject.get("sources");
    System.out.println(preferenceValues.get(0));
  }

  @Test
  public void testBuildUserQueryByPreferences() throws DatafariServerException {
    UserPrefQueryConf prefUser = UserPrefQueryConf.getInstance();
    String result = prefUser.buildUserQueryByPreferences("guylaine");
    System.out.println(result);
  }

  @Test
  public void test2() throws IOException, ParseException {
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
      logger.error("Searching for user Solr query boost by preferences, impossible to retrieve user preferences from the retrieved JSON:\n" + uiConfig, e);
    }

    if (StringUtils.isNotBlank(result.toString())){
      result.insert(0, "\"bq\":\"");
      result.append("\"");
    }
    System.out.println(result);
  }

  private static StringBuilder computeQueryBoostParameterField(String solrField, JSONArray orderedValues){
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
