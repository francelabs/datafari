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
package com.francelabs.datafari.rest.v1_0.authenticate;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.utils.AuthenticatedUserName;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Auth {

    @GetMapping("/rest/v1.0/auth")
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam(name = "callback") String callbackURL) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            if (callbackURL != null && callbackURL.length() > 0) {
                try {
                    response.sendRedirect(callbackURL);
                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                try {
                    response.sendRedirect("/");
                } catch (IOException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } else {
            try {
                String currentURI = request.getRequestURI();
                String[] splitURI = currentURI.split("/");
                String newURI = "";
                // remove the /api/auth part
                for (int i = 0; i < splitURI.length - 3; i++) {
                    if (!splitURI[i].contentEquals("")) {
                        newURI += splitURI[i] + "/";
                    }
                }
                response.sendRedirect("/" + newURI + "login?redirect=" + callbackURL);
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        try {
            response.sendRedirect(callbackURL);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}