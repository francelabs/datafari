/*******************************************************************************
 *  * Copyright 2015 France Labs
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
package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;

/**
 * Servlet implementation class GetFavorites
 */
@WebServlet("/RefreshSession")
public class RefreshSession extends HttpServlet {
  private static final long serialVersionUID = 1L;

  String keycloakEnabled = null;
  String samlEnabled = null;
  String casEnabled = null;
  String kerberosEnabled = null;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public RefreshSession() {
    super();
    try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties");) {
      final Properties appProps = new Properties();
      appProps.load(is);
      keycloakEnabled = appProps.getProperty("keycloak.enabled", "false");
      samlEnabled = appProps.getProperty("saml.enabled", "false");
      casEnabled = appProps.getProperty("cas.enabled", "false");
      kerberosEnabled = appProps.getProperty("kerberos.enabled", "false");
    } catch (final Exception e) {
      keycloakEnabled = "false";
      samlEnabled = "false";
      kerberosEnabled = "false";
    }
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    jsonResponse.put("keycloakEnabled", Boolean.valueOf(keycloakEnabled));
    jsonResponse.put("samlEnabled", Boolean.valueOf(samlEnabled));
    jsonResponse.put("casEnabled", Boolean.valueOf(casEnabled));
    jsonResponse.put("kerberosEnabled", Boolean.valueOf(kerberosEnabled));

    final Principal userPrincipal = request.getUserPrincipal();
    // checking if the user is connected
    if (userPrincipal == null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.NOTCONNECTED.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Not logged");
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Logged");
      String AuthenticatedUserName = "";
      if (request.getUserPrincipal() instanceof KeycloakAuthenticationToken) {
        jsonResponse.put("keycloakUser", true);
        final KeycloakAuthenticationToken keycloakToken = (KeycloakAuthenticationToken) request.getUserPrincipal();
        if (keycloakToken.getDetails() instanceof SimpleKeycloakAccount) {
          final SimpleKeycloakAccount keycloakAccount = (SimpleKeycloakAccount) keycloakToken.getDetails();
          AuthenticatedUserName = keycloakAccount.getKeycloakSecurityContext().getToken().getPreferredUsername();
        } else {
          AuthenticatedUserName = request.getUserPrincipal().getName().replaceAll("[^\\\\]*\\\\", "");
        }
      } else {
        jsonResponse.put("keycloakUser", false);
        AuthenticatedUserName = request.getUserPrincipal().getName();
      }
      jsonResponse.put("user", AuthenticatedUserName);
      if (request.isUserInRole("SearchAdministrator") || request.isUserInRole("SearchExpert")) {
        jsonResponse.put("isAdmin", true);
      } else {
        jsonResponse.put("isAdmin", false);
      }
    }
    final HttpSession session = request.getSession(false);
    if (session != null) {
      jsonResponse.put("sessionTimeout", session.getMaxInactiveInterval());
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
