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
package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.utils.AlertsConfiguration;

/**
 * This Servlet is used to print and modify mail.txt file It is called by
 * alertsAdmin.html DoGet is used to get the value of the fields DoPost is used
 * to modify the value of the fields
 *
 * @author Alexis Karassev
 */
@WebServlet("/admin/MailConf")
public class MailConf extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger(alertsAdmin.class.getName());

	/**
	 * @see HttpServlet#HttpServlet() Gets the path
	 */
	public MailConf() {

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response) Checks if the required file exist Checks if it's the
	 *      administrator that went on alertsAdmin.html Read the file and return
	 *      the values after the "="
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("application/json");
		final JSONObject json = new JSONObject();

		try {
			if (request.isUserInRole("SearchAdministrator")) { // If the
																// user is
																// an Admin

				json.put("smtp", AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_ADRESS));
				json.put("from", AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_FROM));
				json.put("user", AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_USER));
				json.put("pass", AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_PASSWORD));

				json.put("code", 0);

			} else { // Else send insufficiant permission
				LOGGER.error("Insufficiant permission to print mail configuration");
				json.put("message", "Insufficiant permission to print mail configuration");
				json.put("code", "-1");
			}
		} catch (final JSONException e) {
			LOGGER.error("Error while creating the JSON answer inthe doGet of the MailConf Servlet. Error 69032 ", e);
			json.put("message",
					"Error while reading the mail.txt, please make sure the file is correctly filled and retry, if the problem persists contact your system administrator. Error code : 69032");
			json.put("code", 69032);
		}

		final PrintWriter out = response.getWriter();
		out.print(json);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response) Modify the values according to the parameters
	 */
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("application/json");
		final JSONObject json = new JSONObject();

		try {

			AlertsConfiguration.setProperty(AlertsConfiguration.SMTP_ADRESS, request.getParameter("SMTP"));
			AlertsConfiguration.setProperty(AlertsConfiguration.SMTP_FROM, request.getParameter("address"));
			AlertsConfiguration.setProperty(AlertsConfiguration.SMTP_USER, request.getParameter("user"));
			AlertsConfiguration.setProperty(AlertsConfiguration.SMTP_PASSWORD, request.getParameter("pass"));

			json.put("code", 0);
		} catch (final Exception e) {
			json.put("message",
					"Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69513");
			json.put("code", "69513");
			LOGGER.error("Unindentified error in MailConf doPost. Error 69513", e);
		}

		final PrintWriter out = response.getWriter();
		out.print(json);
	}

}
