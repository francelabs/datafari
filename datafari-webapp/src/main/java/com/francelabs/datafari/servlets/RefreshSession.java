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

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.SpringSecurityConfiguration;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.simple.JSONObject;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

/**
 * Servlet implementation class GetFavorites
 */
@WebServlet("/RefreshSession")
public class RefreshSession extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private String keycloakEnabled = "false"; // legacy flag kept for frontend compat (always false now)
  private String samlEnabled = "false";
  private String casEnabled = "false";
  private String kerberosEnabled = "false";
  private String oidcEnabled = "false";

  /**
   * @see HttpServlet#HttpServlet()
   */
  public RefreshSession() {
    super();
    try {
      final SpringSecurityConfiguration appProps = SpringSecurityConfiguration.getInstance();
      keycloakEnabled = appProps.getProperty("keycloak.enabled", "false");
      samlEnabled = appProps.getProperty("saml.enabled", "false");
      casEnabled = appProps.getProperty("cas.enabled", "false");
      kerberosEnabled = appProps.getProperty("kerberos.enabled", "false");
      oidcEnabled = appProps.getProperty("oidc.enabled", "false");
    } catch (final Exception e) {
      // keep flags default value
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

    jsonResponse.put("keycloakEnabled", Boolean.valueOf(keycloakEnabled));  // Always false now
    jsonResponse.put("samlEnabled", Boolean.valueOf(samlEnabled));
    jsonResponse.put("casEnabled", Boolean.valueOf(casEnabled));
    jsonResponse.put("kerberosEnabled", Boolean.valueOf(kerberosEnabled));
    jsonResponse.put("oidcEnabled", Boolean.valueOf(oidcEnabled));

    final Principal userPrincipal = request.getUserPrincipal();
    // checking if the user is connected
    if (userPrincipal == null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.NOTCONNECTED.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Not logged");
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Logged");


      final String authenticatedUserName = AuthenticatedUserName.getName(request);
      jsonResponse.put("user", authenticatedUserName);

      // Additional information on the effective authentication type (informative, useful for debugging/UI)
      boolean isJwt  = userPrincipal instanceof JwtAuthenticationToken;
      boolean isOAuth2 = userPrincipal instanceof OAuth2AuthenticationToken;
      boolean isOidc = (userPrincipal instanceof OAuth2AuthenticationToken o) && (o.getPrincipal() instanceof OidcUser);

      jsonResponse.put("jwtUser", Boolean.valueOf(isJwt));
      jsonResponse.put("oauth2User", Boolean.valueOf(isOAuth2));
      jsonResponse.put("oidcUser", Boolean.valueOf(isOidc));
      jsonResponse.put("keycloakUser", Boolean.FALSE); // Compatible with older models: no Keycloak adapter required


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
