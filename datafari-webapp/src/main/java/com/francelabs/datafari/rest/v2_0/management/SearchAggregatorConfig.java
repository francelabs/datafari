package com.francelabs.datafari.rest.v2_0.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.francelabs.datafari.aggregator.utils.SearchAggregatorPasswordManager;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.PasswordMapper;
import com.francelabs.datafari.utils.SearchAggregatorConfiguration;

@RestController
public class SearchAggregatorConfig {

  @Autowired(required = false)
  private SearchAggregatorPasswordManager saPasswordManager;

  @RequestMapping("/rest/v2.0/management/searchAggregatorConfig")
  public String searchAggregatorConfigManagement(final HttpServletRequest request) {
    if (request.getMethod().contentEquals("GET")) {
      return doGet(request);
    } else if (request.getMethod().contentEquals("POST")) {
      return doPost(request);
    } else {
      final JSONObject jsonResponse = new JSONObject();
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Unsupported request method");
      return jsonResponse.toJSONString();
    }
  }

  protected String doGet(final HttpServletRequest request) {
    final JSONObject jsonResponse = new JSONObject();

    final SearchAggregatorConfiguration sac = SearchAggregatorConfiguration.getInstance();
    final boolean activated = Boolean.valueOf(sac.getProperty(SearchAggregatorConfiguration.ACTIVATED));
    final String externalDatafaris = sac.getProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS);
    final int timeoutPerRequest = Integer.parseInt(sac.getProperty(SearchAggregatorConfiguration.TIMEOUT_PER_REQUEST));
    final int globalTimeout = Integer.parseInt(sac.getProperty(SearchAggregatorConfiguration.GLOBAL_TIMEOUT));
    final String defaultDatafariString = sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI);
    final Boolean alwaysUseDefault = Boolean.valueOf(sac.getProperty(SearchAggregatorConfiguration.ALWAYS_USE_DEFAULT));

    String[] defaultDatafarisArray = {};
    if (defaultDatafariString != null && defaultDatafariString.trim().length() > 0) {
      defaultDatafarisArray = defaultDatafariString.split(SearchAggregatorConfiguration.SITES_SEPARATOR);
    }
    final JSONArray defaultDatafaris = new JSONArray();
    defaultDatafaris.addAll(Arrays.asList(defaultDatafarisArray));
    final JSONParser parser = new JSONParser();
    JSONArray externalDatafarisJson;
    try {
      externalDatafarisJson = (JSONArray) parser.parse(externalDatafaris);
      // Obfuscate secret of each conf
      for (int i = 0; i < externalDatafarisJson.size(); i++) {
        final JSONObject externalDatafari = (JSONObject) externalDatafarisJson.get(i);
        final String oSecret = PasswordMapper.getInstance().mapPasswordToKey(externalDatafari.get("search_aggregator_secret").toString());
        externalDatafari.put("search_aggregator_secret", oSecret);
      }
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put("activated", activated);
      jsonResponse.put("external_datafaris", externalDatafarisJson);
      jsonResponse.put("timeoutPerRequest", timeoutPerRequest);
      jsonResponse.put("globalTimeout", globalTimeout);
      jsonResponse.put("default_datafari", defaultDatafaris);
      jsonResponse.put("always_use_default", alwaysUseDefault);
      boolean renewAvailable = true;
      if (saPasswordManager == null) {
        renewAvailable = false;
      }
      jsonResponse.put("renew_available", renewAvailable);
    } catch (final ParseException e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Unable to parse search aggregator configuration. The \"external_datafaris\" parameter seems not to be a JSONArray");
    }

    return jsonResponse.toJSONString();
  }

  protected String doPost(final HttpServletRequest request) {
    final JSONObject jsonResponse = new JSONObject();

    final SearchAggregatorConfiguration sac = SearchAggregatorConfiguration.getInstance();

    if (request.getParameter("action") != null) {
      final String action = request.getParameter("action");

      try {
        if (action.contentEquals("activate")) {
          final String activated = request.getParameter("activated");
          sac.setProperty(SearchAggregatorConfiguration.ACTIVATED, activated);
          sac.saveProperties();
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        } else if (action.contentEquals("renew")) {
          final String clearPassword = saPasswordManager.renewPassword();
          jsonResponse.put("password", clearPassword);
        } else if (action.contentEquals("timeouts")) {
          final String timeoutPerRequest = request.getParameter("timeoutPerRequest");
          final String globalTimeout = request.getParameter("globalTimeout");
          sac.setProperty(SearchAggregatorConfiguration.TIMEOUT_PER_REQUEST, timeoutPerRequest);
          sac.setProperty(SearchAggregatorConfiguration.GLOBAL_TIMEOUT, globalTimeout);
          sac.saveProperties();
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        } else if (action.contentEquals("delete")) {
          final String datafariName = request.getParameter("datafariName");
          final String defaultDatafariString = sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI);
          String[] defaultDatafarisArray = {};
          if (defaultDatafariString != null && defaultDatafariString.trim().length() > 0) {
            defaultDatafarisArray = defaultDatafariString.split(SearchAggregatorConfiguration.SITES_SEPARATOR);
          }
          final ArrayList<String> defaultDatafaris = new ArrayList<>(Arrays.asList(defaultDatafarisArray));
          if (datafariName != null) {
            final String externalDatafaris = sac.getProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS);
            final JSONParser parser = new JSONParser();
            JSONArray externalDatafarisJson;
            try {
              externalDatafarisJson = (JSONArray) parser.parse(externalDatafaris);
              // Search the label to delete
              final int indexToDelete = getIndex(datafariName, externalDatafarisJson);

              // Delete if found
              if (indexToDelete != -1) {
                // If the deleted element is one of the default, remove it
                if (defaultDatafaris.contains(datafariName)) {
                  defaultDatafaris.remove(datafariName);
                  String defaultDatafariValue = "";
                  for (int i = 0; i < defaultDatafaris.size(); i++) {
                    defaultDatafariValue += defaultDatafaris.get(i);
                    if (i < defaultDatafaris.size() - 1) {
                      defaultDatafariValue += SearchAggregatorConfiguration.SITES_SEPARATOR;
                    }
                  }
                  sac.setProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI, defaultDatafariValue);
                }
                externalDatafarisJson.remove(indexToDelete);
              }
              // Update the properties file
              sac.setProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS, externalDatafarisJson.toJSONString());
              sac.saveProperties();

              final JSONArray defaultDatafarisJSON = new JSONArray();
              defaultDatafarisJSON.addAll(defaultDatafaris);
              jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
              jsonResponse.put("external_datafaris", externalDatafarisJson);
              jsonResponse.put("default_datafari", defaultDatafarisJSON);
            } catch (final ParseException e) {
              jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
              jsonResponse.put(OutputConstants.STATUS, "Unable to parse search aggregator configuration. The \"external_datafaris\" parameter seems not to be a JSONArray");
            }
          } else {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "Wrong request: Missing parameter 'datafariName'");
          }
        } else if (action.contentEquals("new") || action.contentEquals("modify")) {
          final String datafariName = request.getParameter("datafariName");
          final String searchApiUrl = request.getParameter("search_api_url");
          final String tokenRequestUrl = request.getParameter("token_request_url");
          final String searchAggregatorSecret = PasswordMapper.getInstance().mapKeyToPassword(request.getParameter("search_aggregator_secret"));
          final boolean enabled = Boolean.valueOf(request.getParameter("enabled"));
          final String externalDatafaris = sac.getProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS);
          final String defaultDatafariString = sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI);
          String[] defaultDatafarisArray = {};
          if (defaultDatafariString != null && defaultDatafariString.trim().length() > 0) {
            defaultDatafarisArray = defaultDatafariString.split(SearchAggregatorConfiguration.SITES_SEPARATOR);
          }
          final ArrayList<String> defaultDatafaris = new ArrayList<>(Arrays.asList(defaultDatafarisArray));
          final JSONObject receivedConf = new JSONObject();
          receivedConf.put("label", datafariName);
          receivedConf.put("search_api_url", searchApiUrl);
          receivedConf.put("token_request_url", tokenRequestUrl);
          receivedConf.put("search_aggregator_secret", searchAggregatorSecret);
          receivedConf.put("enabled", enabled);
          final JSONParser parser = new JSONParser();
          JSONArray externalDatafarisJson;
          try {
            externalDatafarisJson = (JSONArray) parser.parse(externalDatafaris);
            if (action.contentEquals("new")) {
              externalDatafarisJson.add(receivedConf);
            } else {
              // Search the label to replace
              final int indexToReplace = getIndex(datafariName, externalDatafarisJson);

              if (indexToReplace != -1) {
                externalDatafarisJson.set(indexToReplace, receivedConf);
                // If the modified element is one of the default datafari and it has been disabled remove it from default
                if (!enabled && defaultDatafaris.contains(datafariName)) {
                  defaultDatafaris.remove(datafariName);
                  String defaultDatafariValue = "";
                  for (int i = 0; i < defaultDatafaris.size(); i++) {
                    defaultDatafariValue += defaultDatafaris.get(i);
                    if (i < defaultDatafaris.size() - 1) {
                      defaultDatafariValue += SearchAggregatorConfiguration.SITES_SEPARATOR;
                    }
                  }
                  sac.setProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI, defaultDatafariValue);
                }
              } else {
                externalDatafarisJson.add(receivedConf);
              }
            }
            // Update the properties file
            sac.setProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS, externalDatafarisJson.toJSONString());
            sac.saveProperties();
            final JSONArray defaultDatafarisJSON = new JSONArray();
            defaultDatafarisJSON.addAll(defaultDatafaris);
            jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
            jsonResponse.put("external_datafaris", externalDatafarisJson);
            jsonResponse.put("default_datafari", defaultDatafarisJSON);
          } catch (final ParseException e) {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "Unable to parse search aggregator configuration. The \"external_datafaris\" parameter seems not to be a JSONArray");
          }
        } else if (action.contentEquals("adddefault")) {
          final String datafariName = request.getParameter("datafariName");
          if (datafariName != null) {
            final String defaultDatafariString = sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI);

            String[] defaultDatafarisArray = {};
            if (defaultDatafariString != null && defaultDatafariString.trim().length() > 0) {
              defaultDatafarisArray = defaultDatafariString.split(SearchAggregatorConfiguration.SITES_SEPARATOR);
            }
            final ArrayList<String> defaultDatafaris = new ArrayList<>(Arrays.asList(defaultDatafarisArray));
            final String externalDatafaris = sac.getProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS);
            final JSONParser parser = new JSONParser();
            JSONArray externalDatafarisJson;
            try {
              externalDatafarisJson = (JSONArray) parser.parse(externalDatafaris);

              // Add a new default Datafari to the list if it is not already there and it exists
              // as an external Datafari
              if (getIndex(datafariName, externalDatafarisJson) != -1 && defaultDatafaris.indexOf(datafariName) == -1) {
                defaultDatafaris.add(datafariName);
                String defaultDatafariValue = "";
                for (int i = 0; i < defaultDatafaris.size(); i++) {
                  defaultDatafariValue += defaultDatafaris.get(i);
                  if (i < defaultDatafaris.size() - 1) {
                    defaultDatafariValue += SearchAggregatorConfiguration.SITES_SEPARATOR;
                  }
                }
                sac.setProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI, defaultDatafariValue);
                sac.saveProperties();
              }
              final JSONArray defaultDatafarisJsonArray = new JSONArray();
              defaultDatafarisJsonArray.addAll(defaultDatafaris);
              jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
              jsonResponse.put("default_datafari", defaultDatafarisJsonArray);
            } catch (final ParseException e) {
              jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
              jsonResponse.put(OutputConstants.STATUS, "Unable to parse search aggregator configuration. The \"external_datafaris\" parameter seems not to be a JSONArray");
            }
          } else {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "Wrong request: Missing parameter 'datafariName'");
          }
        } else if (action.contentEquals("removedefault")) {
          final String datafariName = request.getParameter("datafariName");
          if (datafariName != null) {
            final String defaultDatafariString = sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI);

            String[] defaultDatafarisArray = {};
            if (defaultDatafariString != null && defaultDatafariString.trim().length() > 0) {
              defaultDatafarisArray = defaultDatafariString.split(SearchAggregatorConfiguration.SITES_SEPARATOR);
            }
            final ArrayList<String> defaultDatafaris = new ArrayList<>(Arrays.asList(defaultDatafarisArray));
            // If something is effectively removed, save the new value
            if (defaultDatafaris.remove(datafariName)) {
              String defaultDatafariValue = "";
              for (int i = 0; i < defaultDatafaris.size(); i++) {
                defaultDatafariValue += defaultDatafaris.get(i);
                if (i < defaultDatafaris.size() - 1) {
                  defaultDatafariValue += SearchAggregatorConfiguration.SITES_SEPARATOR;
                }
              }
              sac.setProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI, defaultDatafariValue);
              sac.saveProperties();
            }
            final JSONArray defaultDatafarisJsonArray = new JSONArray();
            defaultDatafarisJsonArray.addAll(defaultDatafaris);
            jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
            jsonResponse.put("default_datafari", defaultDatafarisJsonArray);
          } else {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "Wrong request: Missing parameter 'datafariName'");
          }
        } else if (action.contentEquals("setalwaysusedefault")) {
          if (request.getParameter("alwaysusedefault") != null) {
            final Boolean alwaysUseDefault = Boolean.valueOf(request.getParameter("alwaysusedefault"));
            sac.setProperty(SearchAggregatorConfiguration.ALWAYS_USE_DEFAULT, alwaysUseDefault.toString());
            sac.saveProperties();
            jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
            jsonResponse.put("always_use_default", alwaysUseDefault);
          } else {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "Wrong request: Missing parameter 'alwaysusedefault'");
          }
        }
      } catch (final IOException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Unable to save search aggregator properties");
      }
    }
    if (jsonResponse.get(OutputConstants.CODE) == null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    }

    return jsonResponse.toJSONString();
  }

  private int getIndex(final String name, final JSONArray datafarisArray) {
    int index = -1;
    // Search the label to set as default (verify that it exists in the list of datafaris)
    for (int i = 0; i < datafarisArray.size(); i++) {
      final JSONObject externalDatafari = (JSONObject) datafarisArray.get(i);
      final String label = externalDatafari.get("label").toString();
      if (label.contentEquals(name)) {
        index = i;
        break;
      }
    }
    return index;
  }
}
