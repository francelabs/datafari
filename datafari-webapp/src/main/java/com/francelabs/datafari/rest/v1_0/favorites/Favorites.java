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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

@WebServlet("/rest/v1.0/favorites/*")
public class Favorites extends HttpServlet {

    /**
     * Automatically generated serial ID
     */
    private static final long serialVersionUID = 4964433201454339581L;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("error", "Not Implemented");
            PrintWriter out;
            out = response.getWriter();
            out.print(jsonResponse);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("error", "Not Implemented");
            PrintWriter out;
            out = response.getWriter();
            out.print(jsonResponse);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }    }

    @Override
    protected void doDelete(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("error", "Not Implemented");
            PrintWriter out;
            out = response.getWriter();
            out.print(jsonResponse);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }    }

}
