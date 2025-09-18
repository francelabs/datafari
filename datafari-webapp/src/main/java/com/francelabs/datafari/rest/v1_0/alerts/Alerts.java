/*******************************************************************************
 *  Copyright 2020 France Labs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.rest.v1_0.alerts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rest.v1_0.exceptions.BadRequestException;
import com.francelabs.datafari.rest.v1_0.exceptions.DataNotFoundException;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.exceptions.NotAuthenticatedException;
import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.service.db.AlertDataService;
import com.francelabs.datafari.user.Alert;
import com.francelabs.datafari.utils.AuthenticatedUserName;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Alerts {

  @GetMapping(value = "/rest/v1.0/users/current/alerts", produces = "application/json;charset=UTF-8")
  protected String getCurrentUserAlerts(final HttpServletRequest request,
                                        @RequestParam(name = "keyword", required = false) String keyword) {
    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    if (authenticatedUserName == null) {
      throw new NotAuthenticatedException();
    }
    final JSONObject jsonResponse = new JSONObject();
    try {
      // Fetch alerts for the current user
      final List<Properties> alertsList = AlertDataService.getInstance().getUserAlerts(authenticatedUserName);

      // Optional filter by keyword (exact match like before)
      final List<JSONObject> alertsJSONList = new ArrayList<>();
      for (Properties p : alertsList) {
        if (keyword == null || keyword.isEmpty() || keyword.contentEquals(p.getProperty(AlertDataService.KEYWORD_COLUMN))) {
          alertsJSONList.add(propertiesToJSONObject(p));
        }
      }

      jsonResponse.put("alerts", alertsJSONList);
      String auditString = "Accessed alerts data assigned to his user";
      if (keyword != null && !keyword.isEmpty()) {
        auditString += " and corresponding to keyword: " + keyword;
      }
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(), auditString);
      return RestAPIUtils.buildOKResponse(jsonResponse);
    } catch (DatafariServerException e) {
      throw new InternalErrorException("Unexpected error while retrieving alerts");
    }
  }

  /**
   * { "keyword": String (Optional defaults to *:*), "filters": String (Optional),
   *   "core": String, "frequency": String, "mail": String, "subject": String }
   */
  @PostMapping(value = "/rest/v1.0/users/current/alerts", produces = "application/json;charset=UTF-8")
  protected String addAlertToCurrentUser(final HttpServletRequest request, @RequestBody String jsonParam) {
    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    if (authenticatedUserName == null) {
      throw new NotAuthenticatedException();
    }
    final JSONParser parser = new JSONParser();
    try {
      final JSONObject params = (JSONObject) parser.parse(jsonParam);
      final Properties alertProp = jsonToAlertProps(params, authenticatedUserName);

      // Keep calling the existing domain API (backward compatible)
      final String id = Alert.addAlert(alertProp);

      params.put("_id", id);
      return RestAPIUtils.buildOKResponse(params);
    } catch (ParseException e) {
      throw new BadRequestException("Couldn't parse the JSON body");
    } catch (DatafariServerException | IOException e) {
      throw new InternalErrorException("Error while saving new alert.");
    }
  }

  @PutMapping(value = "/rest/v1.0/users/current/alerts/{alertID}", produces = "application/json;charset=UTF-8")
  protected String modifyAlertFromCurrentUser(final HttpServletRequest request,
                                              @RequestBody String jsonParam,
                                              @PathVariable("alertID") String alertID) {
    try {
      final String authenticatedUserName = AuthenticatedUserName.getName(request);
      final JSONObject selectedAlert = getAlertFromIdForUser(alertID, authenticatedUserName);
      if (selectedAlert == null) {
        throw new DataNotFoundException("This alert does not exist for the current user.");
      }
      final JSONParser parser = new JSONParser();
      final JSONObject params = (JSONObject) parser.parse(jsonParam);
      final Properties alertProp = jsonToAlertProps(params, authenticatedUserName);

      // Legacy semantics: delete + re-add (kept as-is)
      Alert.deleteAlert(alertID);
      final String newId = Alert.addAlert(alertProp);
      params.put("_id", newId);

      return RestAPIUtils.buildOKResponse(params);
    } catch (DatafariServerException | IOException e) {
      throw new InternalErrorException("Error while deleting alert.");
    } catch (ParseException e) {
      throw new BadRequestException("Couldn't parse the JSON body");
    }
  }

  @DeleteMapping(value = "/rest/v1.0/users/current/alerts/{alertID}", produces = "application/json;charset=UTF-8")
  protected String removeAlertFromCurrentUser(final HttpServletRequest request,
                                              @PathVariable("alertID") String alertID) {
    try {
      final String authenticatedUserName = AuthenticatedUserName.getName(request);
      final JSONObject selectedAlert = getAlertFromIdForUser(alertID, authenticatedUserName);
      if (selectedAlert == null) {
        throw new DataNotFoundException("This alert does not exist for the current user.");
      }
      Alert.deleteAlert(alertID);
      return RestAPIUtils.buildOKResponse(selectedAlert);
    } catch (DatafariServerException | IOException e) {
      throw new InternalErrorException("Error while deleting alert.");
    }
  }

  /**
   * Returns a JSON object representing the alert with the given id from the given user alerts collection.
   * Returns null if the alert cannot be found.
   */
  private JSONObject getAlertFromIdForUser(final String id, final String username) throws DatafariServerException {
    final List<Properties> alertsList = AlertDataService.getInstance().getUserAlerts(username);
    final Iterator<Properties> it = alertsList.iterator();
    JSONObject selectedAlert = null;
    while (it.hasNext() && selectedAlert == null) {
      final Properties currentAlert = it.next();
      if (id.equals(currentAlert.getProperty("_id"))) {
        selectedAlert = propertiesToJSONObject(currentAlert);
      }
    }
    return selectedAlert;
  }

  /** Converts a Properties to a JSONObject (preserves all properties). */
  private JSONObject propertiesToJSONObject(final Properties properties) {
    final JSONObject response = new JSONObject();
    final Enumeration<?> names = properties.propertyNames();
    while (names.hasMoreElements()) {
      final String name = (String) names.nextElement();
      response.put(name, properties.getProperty(name));
    }
    return response;
  }

  /** Validates and maps the JSON alert body to Properties for persistence. */
  private Properties jsonToAlertProps(final JSONObject alertJSON, final String username) {
    final Properties alertProp = new Properties();
    if (alertJSON.get(AlertDataService.CORE_COLUMN) == null
        || alertJSON.get(AlertDataService.FREQUENCY_COLUMN) == null
        || alertJSON.get(AlertDataService.MAIL_COLUMN) == null
        || alertJSON.get(AlertDataService.SUBJECT_COLUMN) == null) {
      throw new BadRequestException("Some parameters to create the alert were not provided.");
    }

    // Default keyword if missing/empty
    final Object kw = alertJSON.get(AlertDataService.KEYWORD_COLUMN);
    alertProp.put(AlertDataService.KEYWORD_COLUMN, (kw == null || ((String) kw).isEmpty()) ? "*:*" : kw);

    alertProp.put(AlertDataService.CORE_COLUMN, alertJSON.get(AlertDataService.CORE_COLUMN));
    alertProp.put(AlertDataService.FREQUENCY_COLUMN, alertJSON.get(AlertDataService.FREQUENCY_COLUMN));
    alertProp.put(AlertDataService.MAIL_COLUMN, alertJSON.get(AlertDataService.MAIL_COLUMN));
    alertProp.put(AlertDataService.SUBJECT_COLUMN, alertJSON.get(AlertDataService.SUBJECT_COLUMN));

    if (alertJSON.get(AlertDataService.FILTERS_COLUMN) != null) {
      alertProp.put(AlertDataService.FILTERS_COLUMN, alertJSON.get(AlertDataService.FILTERS_COLUMN));
    }
    alertProp.put(AlertDataService.USER_COLUMN, username);
    return alertProp;
  }
}