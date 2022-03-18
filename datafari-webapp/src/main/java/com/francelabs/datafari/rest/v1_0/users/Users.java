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
package com.francelabs.datafari.rest.v1_0.users;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rest.v1_0.exceptions.BadRequestException;
import com.francelabs.datafari.rest.v1_0.exceptions.DataNotFoundException;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.exceptions.NotAuthenticatedException;
import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.user.Lang;
import com.francelabs.datafari.user.UiConfig;
import com.francelabs.datafari.utils.AuthenticatedUserName;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@RestController
public class Users {

    @GetMapping(value = "/rest/v1.0/users/current", produces = "application/json;charset=UTF-8")
    protected String getUser(final HttpServletRequest request) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        JSONObject responseContent = new JSONObject();
        if (authenticatedUserName != null) {
            responseContent.put("name", authenticatedUserName);
            ArrayList<String> roles = new ArrayList<>();
            if (request.isUserInRole("SearchAdministrator")) {
                roles.add("SearchAdministrator");
            }
            if (request.isUserInRole("SearchExpert")) {
                roles.add("SearchExpert");
            }
            responseContent.put("roles", roles);
            try {
                String lang = Lang.getLang(authenticatedUserName);
                AuditLogUtil.log("cassandra", authenticatedUserName, request.getRemoteAddr(),
                        "Accessed saved language for user " + authenticatedUserName);
                responseContent.put("lang", lang);
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Database conenction error while retrieving user language.");
            }
            return RestAPIUtils.buildOKResponse(responseContent);
        } else {
            throw new DataNotFoundException("No user currently connected.");
        }
    }

    @PutMapping(value = "rest/v1.0/users/current", produces = "application/json;charset=UTF-8")
    protected String modifyUser(final HttpServletRequest request, @RequestBody String jsonParam) {
        final JSONParser parser = new JSONParser();
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            try {
                final JSONObject body = (JSONObject) parser.parse(jsonParam);
                String bodyLang = (String) body.get("lang");
                Lang.setLang(authenticatedUserName, bodyLang);
                AuditLogUtil.log("cassandra", "system", request.getRemoteAddr(),
                        "Initialized saved language for user " + authenticatedUserName);
                return RestAPIUtils.buildOKResponse(body);
            } catch (ParseException e1) {
                throw new BadRequestException("Couldn't parse the JSON body");
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Error while saving the new lang.");
            }
        } else {
            throw new NotAuthenticatedException("User must be authenticated to perform this action.");
        }
    }

    @GetMapping(value = "rest/v1.0/users/current/history", produces = "application/json;charset=UTF-8")
    protected String getUserHistory(final HttpServletRequest request) {
        final JSONParser parser = new JSONParser();
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            // TODO Get the user history instead of hardcoded data
            JSONObject responseContent = new JSONObject();
            ArrayList<String> history = new ArrayList<>();
            history.add("france labs");
            history.add("datafari");
            history.add("energy");
            responseContent.put("history", history);
            responseContent.put("__note", "Current history data are hard coded and do not represent actual user queries.");
            AuditLogUtil.log("cassandra", "system", request.getRemoteAddr(),
                    "User " + authenticatedUserName + " accessed his request history");
            return RestAPIUtils.buildOKResponse(responseContent);
        } else {
            throw new NotAuthenticatedException("User must be authenticated to perform this action.");
        }
    }

    @GetMapping(value = "rest/v1.0/users/current/uiconfig", produces = "application/json;charset=UTF-8")
    protected String getUserUiConfig(final HttpServletRequest request) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        JSONObject responseContent = new JSONObject();
        if (authenticatedUserName != null) {
            try {
                String uiConfig = UiConfig.getUiConfig(authenticatedUserName);
                AuditLogUtil.log("cassandra", authenticatedUserName, request.getRemoteAddr(),
                        "Accessed saved ui config for user " + authenticatedUserName);
                responseContent.put("uiConfig", uiConfig);
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Database conenction error while retrieving user language.");
            }
            return RestAPIUtils.buildOKResponse(responseContent);
        } else {
            throw new DataNotFoundException("No user currently connected.");
        }
    }

    @PutMapping(value = "rest/v1.0/users/current/uiconfig", produces = "application/json;charset=UTF-8")
    protected String setUserUiConfig(final HttpServletRequest request, @RequestBody String jsonParam) {
        final JSONParser parser = new JSONParser();
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            try {
                final JSONObject body = (JSONObject) parser.parse(jsonParam);
                String bodyUiConfig = (String) body.get("uiConfig");
                UiConfig.setUiConfig(authenticatedUserName, bodyUiConfig);
                AuditLogUtil.log("cassandra", "system", request.getRemoteAddr(),
                        "Modified saved ui config for user " + authenticatedUserName);
                return RestAPIUtils.buildOKResponse(body);
            } catch (ParseException e1) {
                throw new BadRequestException("Couldn't parse the JSON body");
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Error while saving the new ui config.");
            }
        } else {
            throw new NotAuthenticatedException("User must be authenticated to perform this action.");
        }
    }
}