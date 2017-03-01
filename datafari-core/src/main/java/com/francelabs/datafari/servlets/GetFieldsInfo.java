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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.francelabs.datafari.utils.AdvancedSearchConfiguration;

/**
 * Servlet implementation class GetFieldsInfo
 *
 *
 * Has two return behaviors: - No fieldName param provided : return the list of
 * fields found in the schema.xml of Solr with their attributes (type, indexed,
 * etc) - fieldName param provided : return the field with his attributes if
 * found in the schema.xml
 *
 * The return format is a json containing the return code and the expected
 * result
 *
 */
@WebServlet("/GetFieldsInfo")
public class GetFieldsInfo extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(GetFieldsInfo.class.getName());

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetFieldsInfo() {

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("utf8");
		response.setContentType("application/json");
		final PrintWriter out = response.getWriter();

		try {
			// Define the Solr hostname, port and protocol
			final String solrserver = "localhost";
			final String solrport = "8983";
			final String protocol = "http";

			// Use Solr Schema REST API to get the list of fields
			final HttpClient httpClient = HttpClientBuilder.create().build();
			final HttpHost httpHost = new HttpHost(solrserver, Integer.parseInt(solrport), protocol);
			final HttpGet httpGet = new HttpGet("/solr/FileShare/schema/fields");
			final HttpResponse httpResponse = httpClient.execute(httpHost, httpGet);

			// Construct the jsonResponse
			final JSONObject jsonResponse = new JSONObject();
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				// Status of the API response is OK
				final JSONObject json = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
				final JSONArray fieldsJson = json.getJSONArray("fields");
				for (int i = 0; i < fieldsJson.length(); i++) {
					final JSONObject field = (JSONObject) fieldsJson.get(i);
					// If a fieldname has been provided, it means that this
					// servlet
					// only
					// needs to return infos on this specific field
					if (request.getParameter("fieldName") != null) {
						final String fieldName = request.getParameter("fieldName");
						if (field.getString("name").equals(fieldName)) {
							jsonResponse.append("field", field);
							break;
						}
					} else {
						// Load the list of denied fields
						final String strDeniedFieldsList = AdvancedSearchConfiguration.getInstance()
								.getProperty(AdvancedSearchConfiguration.DENIED_FIELD_LIST);
						final Set<String> deniedFieldsSet = new HashSet<>(
								Arrays.asList(strDeniedFieldsList.split(",")));
						if (!deniedFieldsSet.contains(field.getString("name")) && field.getBoolean("indexed")
								&& !field.getString("name").startsWith("allow_")
								&& !field.getString("name").startsWith("deny_")
								&& !field.getString("name").startsWith("_")) {
							jsonResponse.append("field", field);
						}
					}
				}

				out.print(jsonResponse);
			} else {
				// Status of the API response is an error
				logger.error("Error while retrieving the fields from the Schema API of Solr: "
						+ httpResponse.getStatusLine().toString());
				out.append(
						"Error while retrieving the fields from the Schema API of Solr, please retry, if the problem persists contact your system administrator. Error Code : 69026");
			}
			out.close();
		} catch (final IOException e) {
			logger.error("Error while retrieving the fields from the Schema API of Solr", e);
			out.append(
					"Error while retrieving the fields from the Schema API of Solr, please retry, if the problem persists contact your system administrator. Error Code : 69026");
			out.close();
		}

	}

}
