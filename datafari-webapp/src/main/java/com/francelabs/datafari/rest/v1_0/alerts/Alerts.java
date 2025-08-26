/*******************************************************************************
 *  * Copyright 2020 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
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
        if (authenticatedUserName != null) {
            final JSONObject jsonResponse = new JSONObject();
            try {
                final List<Properties> alertsList = AlertDataService.getInstance().getUserAlerts(authenticatedUserName);
                List<JSONObject> alertsJSONList = new ArrayList<JSONObject>();
                for (int i = 0; i < alertsList.size(); i++) {
                    // If no keyword is provided or if the keyword from the alert corresponds to the
                    // one provided
                    if (keyword == null || keyword.length() == 0
                            || keyword.contentEquals((String) alertsList.get(i).get("keyword"))) {
                        alertsJSONList.add(propertiesToJSONObject(alertsList.get(i)));
                    }
                }
                jsonResponse.put("alerts", alertsJSONList);
                String auditString = "Accessed alerts data assigned to his user";
                if (keyword != null && keyword.length() != 0) {
                    auditString += "and corresponding to keyword: " + keyword;
                }
                AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(), auditString);
                return RestAPIUtils.buildOKResponse(jsonResponse);
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Unexpected error while retrieving alerts");
            }
        } else {
            throw new NotAuthenticatedException();
        }
    }

    /**
     * { "keyword": String (Optional defaults to *:*), "filters": String (Optional),
     * "core": String, "frequency": String, "mail": String, "subject": String }
     */
    @PostMapping(value = "/rest/v1.0/users/current/alerts", produces = "application/json;charset=UTF-8")
    protected String addAlertToCurrentUser(final HttpServletRequest request, @RequestBody String jsonParam) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            final JSONParser parser = new JSONParser();
            try {
                JSONObject params = (JSONObject) parser.parse(jsonParam);
                final Properties alertProp = jsonToAlertProps(params, authenticatedUserName);
                // TODO: The id returned by addAlert is not the registered id, this must be changed
                String id = Alert.addAlert(alertProp);
                params.put("_id", id);
                return RestAPIUtils.buildOKResponse(params);
            } catch (ParseException e) {
                throw new BadRequestException("Couldn't parse the JSON body");
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Error while saving new alert.");
            } catch (IOException e) {
                throw new InternalErrorException("Error while saving new alert.");
            }
        } else {
            throw new NotAuthenticatedException();
        }
    }

    @PutMapping(value = "/rest/v1.0/users/current/alerts/{alertID}", produces = "application/json;charset=UTF-8")
    protected String modifyAlertFromCurrentUser(final HttpServletRequest request, @RequestBody String jsonParam,
            @PathVariable("alertID") String alertID) {
        try {
            final String authenticatedUserName = AuthenticatedUserName.getName(request);
            // Check if this alert ID is in the currently authenticated list of alerts, if
            // not throw an exception.
            final List<Properties> alertsList = AlertDataService.getInstance().getUserAlerts(authenticatedUserName);
            JSONObject selectedAlert = getAlertFromIdForUser(alertID, authenticatedUserName);
            if (selectedAlert == null) {
                throw new DataNotFoundException("This alert does not exist for the current user.");
            }
            // Before deleting, ensure that the body contains valid data for an alert
            // creation
            final JSONParser parser = new JSONParser();
            JSONObject params = (JSONObject) parser.parse(jsonParam);
            final Properties alertProp = jsonToAlertProps(params, authenticatedUserName);
            // We can perform the update, to do so a delete and an insert are performed.
            // TODO: This changes the id of the alert which is not ideal, find a way to
            // perform a cleaner update.
            Alert.deleteAlert(alertID);
            String id = Alert.addAlert(alertProp);
            params.put("_id", id);
            return RestAPIUtils.buildOKResponse(params);
        } catch (DatafariServerException e) {
            throw new InternalErrorException("Error while deleting alert.");
        } catch (IOException e) {
            throw new InternalErrorException("Error while deleting alert.");
        } catch (ParseException e) {
            throw new BadRequestException("Couldn't parse the JSON body");
        }
    }

    @DeleteMapping(value = "/rest/v1.0/users/current/alerts/{alertID}", produces = "application/json;charset=UTF-8")
    protected String removeAlertFromCurrentUser(final HttpServletRequest request,
            @PathVariable("alertID") String alertID) {
        try {
            // First identify if the alert is from the authenticated user
            final String authenticatedUserName = AuthenticatedUserName.getName(request);
            JSONObject selectedAlert = getAlertFromIdForUser(alertID, authenticatedUserName);
            // If it is not found in the currently authenticated user alerts, throw an
            // error, else delete it
            if (selectedAlert == null) {
                throw new DataNotFoundException("This alert does not exist for the current user.");
            }
            Alert.deleteAlert(alertID);
            return RestAPIUtils.buildOKResponse(selectedAlert);
        } catch (DatafariServerException e) {
            throw new InternalErrorException("Error while deleting alert.");
        } catch (IOException e) {
            throw new InternalErrorException("Error while deleting alert.");
        }
    }

    /**
     * Returns a JSON object representing the alert with the given id from the given
     * user alerts collection. Returns null if the alert cannot be found
     * 
     * @param id       the id of the alert to be searched
     * @param username the name of the user to which the alert belongs
     * @return a JSON object representing the alert or null
     * @throws DatafariServerException If anything goes wrong while retrieving the
     *                                 alerts data from the database
     */
    private JSONObject getAlertFromIdForUser(String id, String username) throws DatafariServerException {
        final List<Properties> alertsList = AlertDataService.getInstance().getUserAlerts(username);
        Iterator<Properties> alertsListIt = alertsList.iterator();
        JSONObject selectedAlert = null;
        while (alertsListIt.hasNext() && selectedAlert == null) {
            Properties currentAlert = alertsListIt.next();
            if (currentAlert.getProperty("_id").contentEquals(id)) {
                selectedAlert = propertiesToJSONObject(currentAlert);
            }
        }
        return selectedAlert;
    }

    /**
     * Transforms a Properties object into a JSONObject keeping all the properties.
     * @param properties The properties to translate to JSON
     * @return  a JSONObject containing all the properties of the Properties object
     */
    private JSONObject propertiesToJSONObject(Properties properties) {
        final JSONObject response = new JSONObject();
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String currentName = (String) propertyNames.nextElement();
            response.put(currentName, properties.getProperty(currentName));
        }
        return response;
    }

    /**
     * Transforms a JSONObject representing an alert into a Properties object
     * representing that alert for the given user suitable to be transfered 
     * to the alert data service.
     * @param alertJSON The JSONObject to transform
     * @param username The user to which the alert must belong
     * @return  a Properties representing an alert object to be saved
     */
    private Properties jsonToAlertProps(JSONObject alertJSON, String username) {
        final Properties alertProp = new Properties();
        if (alertJSON.get(AlertDataService.CORE_COLUMN) == null
                || alertJSON.get(AlertDataService.FREQUENCY_COLUMN) == null
                || alertJSON.get(AlertDataService.MAIL_COLUMN) == null
                || alertJSON.get(AlertDataService.SUBJECT_COLUMN) == null) {
            throw new BadRequestException("Some parameters to create the alert were not provided.");
        }
        if (alertJSON.get(AlertDataService.KEYWORD_COLUMN) == null
                || ((String) alertJSON.get(AlertDataService.KEYWORD_COLUMN)).length() == 0) {
            alertProp.put("keyword", "*:*");
        }
        alertProp.put(AlertDataService.KEYWORD_COLUMN, alertJSON.get(AlertDataService.KEYWORD_COLUMN));
        alertProp.put(AlertDataService.CORE_COLUMN, alertJSON.get(AlertDataService.CORE_COLUMN));
        alertProp.put(AlertDataService.FREQUENCY_COLUMN, alertJSON.get(AlertDataService.FREQUENCY_COLUMN));
        alertProp.put(AlertDataService.MAIL_COLUMN, alertJSON.get(AlertDataService.MAIL_COLUMN));
        alertProp.put(AlertDataService.SUBJECT_COLUMN, alertJSON.get(AlertDataService.SUBJECT_COLUMN));
        if (alertJSON.get(AlertDataService.FILTERS_COLUMN) != null) {
            alertProp.put("filters", alertJSON.get(AlertDataService.FILTERS_COLUMN));
        }
        alertProp.put(AlertDataService.USER_COLUMN, username);
        return alertProp;
    }
}