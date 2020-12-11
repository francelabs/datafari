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
package com.francelabs.datafari.rest.v1_0.favorites;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rest.v1_0.exceptions.BadRequestException;
import com.francelabs.datafari.rest.v1_0.exceptions.DataNotFoundException;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.exceptions.NotAuthenticatedException;
import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.user.Favorite;
import com.francelabs.datafari.utils.AuthenticatedUserName;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@RestController
public class Favorites {

    @GetMapping(value = "/rest/v1.0/users/current/favorites", produces = "application/json;charset=UTF-8")
    public String getFavorites(final HttpServletRequest request,
            @RequestParam(name = "ids", required = false) String[] ids) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            final JSONObject jsonResponse = new JSONObject();
            if (ids != null && ids.length == 0) {
                ids = null;
            }
            try {
                List<String> favoritesList = Favorite.getFavorites(authenticatedUserName, null);
                List<JSONObject> favoritesJSONList = new ArrayList<JSONObject>();
                final JSONParser parser = new JSONParser();
                for (int i = 0; i < favoritesList.size(); i++) {
                    favoritesJSONList.add((JSONObject) parser.parse(favoritesList.get(i)));
                }
                jsonResponse.put("favorites", favoritesJSONList);
                AuditLogUtil.log("cassandra", authenticatedUserName, request.getRemoteAddr(),
                        "Accessed the list of Favorites of user " + authenticatedUserName);
                return jsonResponse.toJSONString();
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Unexpected error while retrieving favorites");
            } catch (ParseException e) {
                throw new InternalErrorException("Error parsing internal data during favorite retrieval.");
            }
        } else {
            throw new NotAuthenticatedException();
        }
    }

    @PostMapping(value = "/rest/v1.0/users/current/favorites", produces = "application/json;charset=UTF-8", consumes = "application/json;charset=UTF-8")
    public String addFavorite(final HttpServletRequest request, final HttpServletResponse response,
            @RequestBody String jsonParam) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            final JSONParser parser = new JSONParser();
            try {
                JSONObject params = (JSONObject) parser.parse(jsonParam);
                String id = (String) params.get("id");
                String title = (String) params.get("title");
                if (id == null || id.length() == 0 || title == null) {
                    throw new BadRequestException("id or title for the new favorite were not provided correctly.");
                } else {
                    Favorite.addFavorite(authenticatedUserName, id, title);
                    AuditLogUtil.log("Cassandra", authenticatedUserName, request.getRemoteAddr(),
                            "Added a favorite for user " + authenticatedUserName);
                    return RestAPIUtils.buildOKResponse(params);
                }
            } catch (ParseException e) {
                throw new BadRequestException("Couldn't parse the JSON body");
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Error while saving favorite.");
            }
        } else {
            throw new NotAuthenticatedException();
        }
    }

    @DeleteMapping(value = "/rest/v1.0/users/current/favorites/{favoriteID}", produces = "application/json;charset=UTF-8")
    public String removeFavorite(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable("favoriteID") String favoriteID) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            try {
                String[] favoriteIDArray = { favoriteID };
                List<String> favorites = Favorite.getFavorites(authenticatedUserName, favoriteIDArray);
                if (favorites.size() == 1) {
                    final JSONParser parser = new JSONParser();
                    try {
                        final JSONObject content = (JSONObject) parser.parse(favorites.get(0));
                        Favorite.deleteFavorite(authenticatedUserName, favoriteID);
                        AuditLogUtil.log("cassandra", authenticatedUserName, request.getRemoteAddr(),
                                "Removed a favorite document for user " + authenticatedUserName);
                        return RestAPIUtils.buildOKResponse(content);
                    } catch (ParseException e) {
                        throw new InternalErrorException("Error parsing internal data during favorite deletion.");
                    }
                }
                // Cannot delete something that is not there
                throw new DataNotFoundException(
                        "Couldn't find favorite with id " + favoriteID + " for the current user.");
            } catch (DatafariServerException e) {
                throw new InternalErrorException("Unexpected error while deleting favorite.");
            }
        } else {
            throw new NotAuthenticatedException();
        }
    }

}
