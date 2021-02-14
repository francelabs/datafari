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
package com.francelabs.datafari.rest.v1_0.savedsearches;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.rest.v1_0.exceptions.BadRequestException;
import com.francelabs.datafari.rest.v1_0.exceptions.DataNotFoundException;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.exceptions.NotAuthenticatedException;
import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.service.db.SavedSearchDataService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SavedSearches {

    final static Logger logger = LogManager.getLogger(SavedSearchDataService.class.getName());

    @GetMapping(value = "/rest/v1.0/users/current/savedsearches", produces = "application/json;charset=UTF-8")
    protected String getCurrentUserSavedSearches(final HttpServletRequest request) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            final JSONObject jsonResponse = new JSONObject();
            try {
                Map<String, String> savedSearcheMap;
                savedSearcheMap = SavedSearchDataService.getInstance().getSearches(authenticatedUserName);
                List<JSONObject> savedSearchesJSONList = new ArrayList<JSONObject>();
                savedSearcheMap.forEach((key, value) -> {
                    JSONObject savedSearchObject = new JSONObject();
                    savedSearchObject.put("name", key);
                    savedSearchObject.put("search", value);
                    savedSearchesJSONList.add(savedSearchObject);
                });
                jsonResponse.put("savedsearches", savedSearchesJSONList);
                String auditString = "Accessed saved searches data assigned to his user";
                AuditLogUtil.log("cassandra", authenticatedUserName, request.getRemoteAddr(), auditString);
                return RestAPIUtils.buildOKResponse(jsonResponse);
            } catch (Exception e) {
                logger.debug("REST API encountered an exception while retrieving saved searches for a user", e);
                throw new InternalErrorException("Unexpected error while retrieving saved searches");
            }
        } else {
            throw new NotAuthenticatedException();
        }
    }

    @PostMapping(value = "/rest/v1.0/users/current/savedsearches", produces = "application/json;charset=UTF-8")
    protected String createSavedSearch(final HttpServletRequest request, @RequestBody String jsonParam) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            final JSONParser parser = new JSONParser();
            try {
                JSONObject params = (JSONObject) parser.parse(jsonParam);
                if (!checkJSONValidity(params)) {
                    throw new BadRequestException("The JSON data does not represent a proper saved search.");
                }
                int returnCode = SavedSearchDataService.getInstance().saveSearch(authenticatedUserName,
                        (String) params.get("name"), (String) params.get("search"));
                if (returnCode == CodesReturned.ALLOK.getValue()) {
                    String auditString = "created a saved search assigned to his user";
                    AuditLogUtil.log("cassandra", authenticatedUserName, request.getRemoteAddr(), auditString);
                    return RestAPIUtils.buildOKResponse(params);
                } else {
                    throw new InternalErrorException(
                            "Error while saving new saved search or saved search with this name already exists.");
                }
            } catch (ParseException e) {
                throw new BadRequestException("Couldn't parse the JSON body");
            } catch (Exception e) {
                logger.debug("REST API encountered an exception while saving a saved searches for a user", e);
                throw new InternalErrorException("Error while saving new saved search.");
            }
        } else {
            throw new NotAuthenticatedException();
        }
    }

    @PutMapping(value = "/rest/v1.0/users/current/savedsearches/{savedSearchName}", produces = "application/json;charset=UTF-8")
    protected String modifyAlertFromCurrentUser(final HttpServletRequest request, @RequestBody String jsonParam,
            @PathVariable("savedSearchName") String savedSearchName) {
        try {
            // First identify if the saved search is from the authenticated user
            final String authenticatedUserName = AuthenticatedUserName.getName(request);
            JSONObject selectedSavedSearch = getSavedSearchFromNameForUser(savedSearchName, authenticatedUserName);
            // If it is not found in the currently authenticated user saved searches, throw
            // an
            // error, else delete it
            if (selectedSavedSearch == null) {
                throw new DataNotFoundException("This saved search does not exist for the current user.");
            }
            // Before deleting, ensure that the body contains valid data for a saved search
            // creation
            final JSONParser parser = new JSONParser();
            JSONObject params = (JSONObject) parser.parse(jsonParam);
            if (!checkJSONValidity(params)) {
                throw new BadRequestException("The JSON data does not represent a proper saved search.");
            }
            // We can perform the update, to do so a delete and an insert are performed.
            // TODO: Replacing this with a proper update would be better.
            int returnCode = SavedSearchDataService.getInstance().deleteSearch(authenticatedUserName,
                    (String) selectedSavedSearch.get("name"), (String) selectedSavedSearch.get("search"));
            if (returnCode == CodesReturned.ALLOK.getValue()) {
                returnCode = SavedSearchDataService.getInstance().saveSearch(authenticatedUserName,
                        (String) params.get("name"), (String) params.get("search"));        
                if (returnCode == CodesReturned.ALLOK.getValue()) {
                    return RestAPIUtils.buildOKResponse(params);
                }
            }
            throw new InternalErrorException(
                            "Error while updating saved search.");
        } catch (ParseException e) {
            throw new BadRequestException("Couldn't parse the JSON body");
        } catch (Exception e) {
            logger.debug("REST API encountered an exception while updating saved searches for a user", e);
            throw new InternalErrorException("Error while updating a saved search.");
        } 
    }

    @DeleteMapping(value = "/rest/v1.0/users/current/savedsearches/{savedSearchName}", produces = "application/json;charset=UTF-8")
    protected String deleteSearchFromCurrentUser(final HttpServletRequest request,
            @PathVariable("savedSearchName") String savedSearchName) {
        try {
            // First identify if the saved search is from the authenticated user
            final String authenticatedUserName = AuthenticatedUserName.getName(request);
            JSONObject selectedSavedSearch = getSavedSearchFromNameForUser(savedSearchName, authenticatedUserName);
            // If it is not found in the currently authenticated user saved searches, throw
            // an
            // error, else delete it
            if (selectedSavedSearch == null) {
                throw new DataNotFoundException("This saved search does not exist for the current user.");
            }
            SavedSearchDataService.getInstance().deleteSearch(authenticatedUserName,
                    (String) selectedSavedSearch.get("name"), (String) selectedSavedSearch.get("search"));
            String auditString = "deleted a saved search assigned to his user";
            AuditLogUtil.log("cassandra", authenticatedUserName, request.getRemoteAddr(), auditString);
            return RestAPIUtils.buildOKResponse(selectedSavedSearch);
        } catch (Exception e) {
            logger.debug("REST API encountered an exception while deleting a saved searche for a user", e);
            throw new InternalErrorException("Error while deleting a saved search.");
        }
    }

    /**
     * Returns a JSON object representing the saved search with the given name from
     * the given user saved searches collection. Returns null if the saved search
     * cannot be found
     * 
     * @param name     the name of the saved search to be searched
     * @param username the name of the user to which the saved search belongs
     * @return a JSON object representing the saved search or null
     * @throws Exception If anything goes wrong while retrieving the saved searches
     *                   data from the database
     */
    private JSONObject getSavedSearchFromNameForUser(String name, String username) throws Exception {
        final Map<String, String> savedSearchesMap = SavedSearchDataService.getInstance().getSearches(username);
        Iterator<String> savedSearchesIt = savedSearchesMap.keySet().iterator();
        JSONObject selectedSavedSearch = null;
        while (savedSearchesIt.hasNext() && selectedSavedSearch == null) {
            String currentSavedSearchName = savedSearchesIt.next();
            if (currentSavedSearchName.contentEquals(name)) {
                selectedSavedSearch = new JSONObject();
                selectedSavedSearch.put("name", currentSavedSearchName);
                selectedSavedSearch.put("search", savedSearchesMap.get(currentSavedSearchName));
            }
        }
        return selectedSavedSearch;
    }

    /**
     * Returns true if the JSON object contains the properties needed for the
     * creation of a saved search, false otherwise. Saved search JSON object must
     * contain a property name and a property search that are both of type String.
     * Other properties are ignored.
     * 
     * @param savedSearchJSON The JSON object to analyze
     * @retur true is the JSON Object is valid to be used for the creation of a
     *        saved search.
     */
    private boolean checkJSONValidity(JSONObject savedSearchJSON) {
        if (savedSearchJSON.get("name") != null && savedSearchJSON.get("search") != null) {
            return true;
        }
        return false;
    }

}