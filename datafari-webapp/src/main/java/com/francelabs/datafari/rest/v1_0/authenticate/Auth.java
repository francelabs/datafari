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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Auth {

  @GetMapping("/rest/v1.0/auth")
  /**
   * Protected endpoint that triggers the authentication and then simply redirect to the URL specified by the 'callback' request parameter
   *
   * @param request
   * @param response
   * @param callbackURL the URL to redirect to
   */
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response, @RequestParam(name = "callback") final String callbackURL) {
    if (callbackURL != null && callbackURL.length() > 0) {
      try {
        response.sendRedirect(callbackURL);
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } else {
      // No callback URL provided, so we redirect to the main context of the request !
      try {
        final String mainContext = request.getContextPath();
        response.sendRedirect(mainContext);
      } catch (final IOException e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
  }
}