/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.user.Lang;

/**
 * Servlet implementation class addFavorite
 */
@WebServlet("/applyLang")
public class ApplyUserLang extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(ApplyUserLang.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ApplyUserLang() {
    super();
    BasicConfigurator.configure();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    final Principal userPrincipal = request.getUserPrincipal();
    String redirectUrl = "";
    String lang = request.getParameter("lang");
    final String reqLang = request.getParameter("lang");
    // checking if the user is connected
    if (userPrincipal == null) {
      jsonResponse.put("code", CodesReturned.NOTCONNECTED);
      if ((request.getParameter("urlRedirect") != null) && !request.getParameter("urlRedirect").isEmpty()) {
        response.sendRedirect(request.getParameter("urlRedirect"));
      }
    } else {
      final String username = request.getUserPrincipal().getName();
      try {
        lang = Lang.getLang(username);
        if (lang == null) {
          if ((reqLang != null) && !reqLang.isEmpty() && (reqLang.length() <= 5)) {
            lang = reqLang;
          } else {
            lang = "fr";
          }
          Lang.setLang(username, lang);
        }
        jsonResponse.put("code", CodesReturned.ALLOK);
        if ((request.getParameter("urlRedirect") != null) && !request.getParameter("urlRedirect").isEmpty()) {
          redirectUrl = request.getParameter("urlRedirect");
          Pattern p = Pattern.compile("lang=[^\\s\\&]*");
          Matcher m = p.matcher(redirectUrl);
          if (m.find()) {
            redirectUrl = redirectUrl.replace(m.group(), "lang=" + lang);
          } else {
            p = Pattern.compile("\\?[^\\s\\&]+");
            m = p.matcher(redirectUrl);
            if (!m.find()) {
              if (redirectUrl.contains("?")) {
                redirectUrl += "lang=" + lang;
              } else {
                redirectUrl += "?lang=" + lang;
              }
            } else {
              redirectUrl += "&lang=" + lang;
            }
          }
          response.sendRedirect(redirectUrl);
        }
      } catch (final Exception e) {
        logger.error(e);
        jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONDATABASE);
        if ((request.getParameter("urlRedirect") != null) && !request.getParameter("urlRedirect").isEmpty()) {
          response.sendRedirect(request.getParameter("urlRedirect"));
        }
      }
    }
    jsonResponse.put("lang", lang);
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    final Principal userPrincipal = request.getUserPrincipal();
    if (userPrincipal != null) {
      final String reqLang = request.getParameter("lang");
      if ((reqLang != null) && !reqLang.isEmpty() && (reqLang.length() <= 5)) {
        final String username = request.getUserPrincipal().getName();
        try {
          Lang.updateLang(username, reqLang);
          jsonResponse.put("code", CodesReturned.ALLOK);
        } catch (final Exception e) {
          logger.error(e);
          jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONDATABASE);
        }
      }
    } else {
      jsonResponse.put("code", CodesReturned.NOTCONNECTED);
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
