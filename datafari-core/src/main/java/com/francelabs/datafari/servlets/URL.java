/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.statistics.StatsPusher;
import com.francelabs.datafari.utils.RealmLdapConfiguration;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Servlet implementation class URL
 */
@WebServlet("/URL")
public class URL extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(URL.class.getName());

  private static final String redirectUrl = "/url.jsp";

  /**
   * @see HttpServlet#HttpServlet()
   */
  public URL() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    request.setCharacterEncoding("UTF-8");

    final String protocol = request.getScheme() + ":";

    final Map<String, String[]> requestMap = new HashMap<>();
    requestMap.putAll(request.getParameterMap());
    final IndexerQuery query = IndexerServerManager.createQuery();
    query.addParams(requestMap);
    // get the AD domain
    String domain = "";
    HashMap<String, String> h;
    try {
      h = RealmLdapConfiguration.getConfig(request);

      if (h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME) != null) {
        final String userBase = h.get(RealmLdapConfiguration.ATTR_DOMAIN_NAME).toLowerCase();
        final String[] parts = userBase.split(",");
        domain = "";
        for (int i = 0; i < parts.length; i++) {
          if (parts[i].indexOf("dc=") != -1) { // Check if the current
            // part is a domain
            // component
            if (!domain.isEmpty()) {
              domain += ".";
            }
            domain += parts[i].substring(parts[i].indexOf('=') + 1);
          }
        }
      }

      // Add authentication
      if (request.getUserPrincipal() != null) {
        String AuthenticatedUserName = request.getUserPrincipal().getName().replaceAll("[^\\\\]*\\\\", "");
        if (AuthenticatedUserName.contains("@")) {
          AuthenticatedUserName = AuthenticatedUserName.substring(0, AuthenticatedUserName.indexOf("@"));
        }
        if (!domain.equals("")) {
          AuthenticatedUserName += "@" + domain;
        }
        query.setParam("AuthenticatedUserName", AuthenticatedUserName);
      }
    } catch (final Exception e) {
      logger.error("Unable to add AuthenticatedUserName to query", e);
    }
    StatsPusher.pushDocument(query, protocol);

    // String surl = URLDecoder.decode(request.getParameter("url"),
    // "ISO-8859-1");
    final String surl = request.getParameter("url");

    if (ScriptConfiguration.getProperty("ALLOWLOCALFILEREADING").equals("true") && !surl.startsWith("file://///")) {

      final int BUFSIZE = 4096;
      String fileName = null;

      /**
       * File Display/Download --> <!-- Written by Rick Garcia -->
       */
      if (SystemUtils.IS_OS_LINUX) {
        // try to open the file locally
        final String fileNameA[] = surl.split(":");
        fileName = URLDecoder.decode(fileNameA[1], "UTF-8");

      } else if (SystemUtils.IS_OS_WINDOWS) {
        fileName = URLDecoder.decode(surl, "UTF-8").replaceFirst("file:/", "");
      }

      final File file = new File(fileName);
      int length = 0;
      final ServletOutputStream outStream = response.getOutputStream();
      final ServletContext context = getServletConfig().getServletContext();
      String mimetype = context.getMimeType(fileName);

      // sets response content type
      if (mimetype == null) {
        mimetype = "application/octet-stream";

      }
      response.setContentType(mimetype);
      response.setContentLength((int) file.length());

      // sets HTTP header
      response.setHeader("Content-Disposition", "inline; fileName=\"" + fileName + "\"");

      final byte[] byteBuffer = new byte[BUFSIZE];
      final DataInputStream in = new DataInputStream(new FileInputStream(file));

      // reads the file's bytes and writes them to the response stream
      while (in != null && (length = in.read(byteBuffer)) != -1) {
        outStream.write(byteBuffer, 0, length);
      }

      in.close();
      outStream.close();
    } else {

      final RequestDispatcher rd = request.getRequestDispatcher(redirectUrl);
      rd.forward(request, response);
    }
  }
}
