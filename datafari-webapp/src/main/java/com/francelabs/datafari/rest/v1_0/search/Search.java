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
package com.francelabs.datafari.rest.v1_0.search;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.aggregator.servlet.SearchAggregator;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.users.Users;
import com.francelabs.datafari.service.db.UserHistoryDataService;
import com.francelabs.datafari.servlets.GetUserQueryConf;
import com.francelabs.datafari.utils.AuthenticatedUserName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Search extends HttpServlet {

    /**
     * Automatically generated serial ID
     */
    private static final long serialVersionUID = -7963279533577712482L;

    private static final Logger logger = LogManager.getLogger(Users.class.getName());

    private void saveToUserHistory(final HttpServletRequest request) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            try {
                if (request.getParameter("action") == null || request.getParameter("action").contentEquals("search")) {
                    UserHistoryDataService historyService = UserHistoryDataService.getInstance();
                    List<String> history = historyService.getHistory(authenticatedUserName);
                    String currentQuery = request.getParameter("q");
                    if (history == null) {
                        // History does not exist, create it and set it
                        ArrayList<String> newHistory = new ArrayList<>();
                        newHistory.add(currentQuery);
                        historyService.setHistory(authenticatedUserName, newHistory);
                    } else {
                        if (history.contains(currentQuery)) {
                            // If the current query is in the history, first remove it
                            int index = history.indexOf(currentQuery);
                            history.remove(index);
                        }
                        // Add the current query to the top of the history
                        history.add(0, currentQuery);
                        // Remove the last query from the history if it gets too large
                        if (history.size() > UserHistoryDataService.MAX_HISTORY_LENGTH) {
                            history.remove(history.size() - 1);
                        }
                        historyService.updateHistory(authenticatedUserName, history);
                    }
                }
            } catch (DatafariServerException e) {
                logger.warn("Couldn't save query to user history", e);
            }
        }
    }

    @GetMapping("/rest/v1.0/search/*")
    protected void performSearch(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            if (request.getParameter("id") == null) {
                UUID id = UUID.randomUUID();
                request.setAttribute("id", id.toString());
            }
            
            String userConf = GetUserQueryConf.getUserQueryConf(request);
            if (userConf != null && userConf.length() > 0) {
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonConf = (JSONObject) parser.parse(userConf);
                    String qf = (String) jsonConf.get("qf");
                    String pf = (String) jsonConf.get("pf");
                    if (qf != null && qf.length() > 0) {
                        request.setAttribute("qf", qf);
                    }
                    
                    if (pf != null && pf.length() > 0) {
                        request.setAttribute("pf", pf);
                    }
                } catch (ParseException e) {
                    logger.warn("An issue has occured while reading user query conf", e);
                }
            }
            saveToUserHistory(request);
            SearchAggregator.doGetSearch(request, response);
        } catch (ServletException | IOException e) {
            throw new InternalErrorException("Error while performing the search request.");
        }
    }

    @PostMapping("/rest/v1.0/search/*")
    protected void stopSearch(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            SearchAggregator.doPostSearch(request, response);
        } catch (ServletException | IOException e) {
            throw new InternalErrorException("Error while stopping the search request.");
        }
    }
}