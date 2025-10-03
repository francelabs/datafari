package com.francelabs.datafari.servlets;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.service.db.AlertDataService;
import com.francelabs.datafari.user.Alert;
import com.francelabs.datafari.utils.AuthenticatedUserName;

/**
 * Javadoc
 *
 * This servlet is used to add new alerts and print/edit/delete the existing
 * alerts in the database. It is only called by the Alerts.html. doGet is used
 * to print the Alerts. doPost is used to add/edit/delete Alerts. The connection
 * with the database is made in the constructor.
 *
 * @author Alexis Karassev
 *
 */
@WebServlet("/Alerts")
public class Alerts extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private final static Logger LOGGER = LogManager.getLogger(Alerts.class.getName());

  /**
   * @throws IOException
   * @see HttpServlet#HttpServlet() Connect with the database
   */
  public Alerts() throws IOException {
    super();
  }

  /**
   * @throws IOException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response) Used to print the existing alerts. Makes a request and put the
   *      results into a JSON file.
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    boolean allOK = true;
    try {
      final PrintWriter pw = response.getWriter();

      int i = 0;
      final JSONObject superJson = new JSONObject();
      final JSONArray alertsArray = new JSONArray();
      try {
        final List<Properties> alerts = AlertDataService.getInstance().getAlerts();
        // Get all the existing Alerts
        for (final Properties alert : alerts) { // Get the next Alert
          if (!request.getParameter("keyword").equals("")) {
            // If the user have typed something in the search field
            if (alert.get("keyword").equals(request.getParameter("keyword"))) {
              // then only the Alerts with a corresponding keyword are put into the Json
              if (request.getRemoteUser().equals(alert.get("user")) || request.isUserInRole("SearchAdministrator")) {
                // Only the Alerts with the correct user, except if it's the admin
                // put the jsonObject in an other so that this superJSON will contain all the
                // Alerts
                alertsArray.add(put(alert, request.isUserInRole("SearchAdministrator")));

                i++; // count the number of alerts

              }
            }
          } else { // If nothing was typed in the search field

            if (request.getRemoteUser().equals(alert.get("user")) || request.isUserInRole("SearchAdministrator")) {
              // Only the Alerts with the correct user, except if it's the admin
              alertsArray.add(put(alert, request.isUserInRole("SearchAdministrator")));
              i++;
            }

          }
        }
        superJson.put("alerts", alertsArray);

        superJson.put("length", i);
        // Put the number of alerts at the end of the JSON object (handy to print the
        // alerts back in the HTML)

        pw.write(superJson.toString()); // Send the JSON back to the HTML page
        response.setStatus(200);
        response.setContentType("text/json;charset=UTF-8");
      } catch (final Exception e) {
        pw.append(
            "Error connecting to the database, please retry, if the problem persists contact your system administrator. Error code : 69010");
        pw.close();
        LOGGER.error("Error connecting to the database in Alerts Servlet's doGet. Error 69010", e);
        allOK = false;
        return;
      }
    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append(
          "Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69502");
      out.close();
      LOGGER.error("Unindentified error in Alerts doGet. Error 69502", e);
      allOK = false;
    } finally {
      // Finally should get executed even in cases of return within the try or catch
      String authenticatedUserName = AuthenticatedUserName.getName(request);
      String keyword = (request.getParameter("keyword") == null || request.getParameter("keyword").length() == 0) ? "*"
          : request.getParameter("keyword");
      if (allOK) {
        if (request.isUserInRole("SearchAdministrator")) {
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "As SearcHAdministrator accessed all alerts data corresponding to keyword: " + keyword);
        } else {
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "Accessed alerts data assigned to his user and corresponding to keyword: " + keyword);
        }
      } else {
        if (request.isUserInRole("SearchAdministrator")) {
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "As SearcHAdministrator, got an error trying to access all alerts data corresponding to keyword: "
                  + keyword);
        } else {
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "Got an error trying to access alerts data assigned to his user and corresponding to keyword: "
                  + keyword);
        }
      }
    }
  }

  private Object put(final Properties alert, final boolean admin) {
    final JSONObject json = new JSONObject(); // Creates a json object
    json.put("_id", alert.get("_id")); // gets the id
    json.put("keyword", alert.get("keyword")); // gets the keyword
    json.put("subject", alert.get("subject")); // gets the subject
    json.put("core", alert.get("core")); // gets the core
    json.put("frequency", alert.get("frequency")); // gets the frequency
    json.put("mail", alert.get("mail")); // gets the mail
    if (admin) {
      json.put("user", alert.get("user")); // gets the user
    }
    return json;

  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response) Used to delete/add/edit an Alert Directly change the database
   *      and returns nothing
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    boolean allOK = true;
    try {
      final PrintWriter pw = response.getWriter();
      final JSONObject jsonResponse = new JSONObject();
      try {
        if (request.getParameter("_id") != null) {
           // Deleting part: executes the query in the collection
          Alert.deleteAlert(request.getParameter("_id")); 
        }
        if (request.getParameter("keyword") != null) {
          final Properties alert = new Properties();// Adding part
          for (final Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            // For all the parameters passed, we put the parameter name as the key and the
            // content as the value
            final String elem = e.nextElement();
            if (!elem.equals("_id")) {
              // Do not put the _id manually so if the parameter is "_id" we do not put it in,
              // otherwise there will be an exception at the 2nd modification or at a removal
              // after a modification.
              alert.put(elem, request.getParameter(elem));
            }
            // This loop can only be triggered by an edit.
          }
          if (!alert.containsKey("filters")) {
            alert.put("filters", "");
          }
          alert.put("user", request.getRemoteUser());
          jsonResponse.put("uuid", Alert.addAlert(alert));
          // insert the object composed of all the parameters
        }
        // If this is an edit the two parts (Delete and Add) will be executed
        // successively
        pw.write(jsonResponse.toString());
      } catch (final Exception e) {
        pw.append(
            "Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69011");
        pw.close();
        LOGGER.error("Error connecting to the database in Alerts Servlet's doPost. Error 69011", e);
        allOK = false;
      }
    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append(
          "Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69503");
      out.close();
      LOGGER.error("Unindentified error in Alerts doPost. Error 69503", e);
      allOK = false;
    } finally {
      String authenticatedUserName = AuthenticatedUserName.getName(request);
      String keyword = (request.getParameter("keyword") == null || request.getParameter("keyword").length() == 0) ? "*"
          : request.getParameter("keyword");
      if (allOK) {
        if (request.getParameter("_id") != null) {
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "Deleted an alert from the alerts collection.");
        } 
        if (request.getParameter("keyword") != null) {
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "Added an alert to the alerts collection");
        }
      } else {
        if (request.getParameter("_id") != null) {
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "Error deleting an alert from the alerts collection.");
        } 
        if (request.getParameter("keyword") != null) {
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "Error adding an alert to the alerts collection");
        }
      }
    }
  }

  static String readFile(final String path, final Charset encoding)
      // Read the file
      throws IOException {
    final byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }
}