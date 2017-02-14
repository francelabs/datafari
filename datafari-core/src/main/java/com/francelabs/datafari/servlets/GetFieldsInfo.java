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

import java.io.File;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.francelabs.datafari.utils.AdvancedSearchConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

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
	private final String env;
	private File schema = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetFieldsInfo() {
		String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

		if (environnement == null) { // If in development environment
			environnement = ExecutionEnvironment.getDevExecutionEnvironment();
		}
		env = environnement + File.separator + "solr" + File.separator + "solrcloud" + File.separator + "FileShare"
				+ File.separator + "conf";

		if (new File(env + File.separator + "schema.xml").exists()) { // Check
			// if
			// the
			// files
			// exists
			schema = new File(env + File.separator + "schema.xml");
		}
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

		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			final JSONObject Superjson = new JSONObject();
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			final Document docSchem = dBuilder.parse(schema); // Parse
																// the
																// schema

			// If a fieldname has been provided, it means that this servlet only
			// needs to return infos on this specific field
			if (request.getParameter("fieldName") != null) {
				final String fieldName = request.getParameter("fieldName");
				final XPathFactory xPathfactory = XPathFactory.newInstance();
				final XPath xpath = xPathfactory.newXPath();
				XPathExpression expr;

				// search for the provided field with Xpath
				expr = xpath.compile("//field[@name=\"" + fieldName + "\"]");
				final Node fieldNode = (Node) expr.evaluate(docSchem, XPathConstants.NODE);
				if (fieldNode != null) {
					// Field has been found, transform it into a json and return
					// it
					final JSONObject json = new JSONObject();
					final Element elem = (Element) fieldNode; // Get
					// a
					// field
					// node
					final NamedNodeMap map = elem.getAttributes();
					for (int j = 0; j < map.getLength(); j++) { // Get
						// its
						// attributes
						json.append(map.item(j).getNodeName(), map.item(j).getNodeValue());
					}
					if (json != null) {
						Superjson.append("field", json);
					}
				}
				final PrintWriter out = response.getWriter();
				out.print(Superjson);

			} else { // otherwise return the list of all fields found in the
						// schema.xml file

				// Load the list of denied fields
				final String strDeniedFieldsList = AdvancedSearchConfiguration.getInstance()
						.getProperty(AdvancedSearchConfiguration.DENIED_FIELD_LIST);
				final Set<String> deniedFieldsSet = new HashSet<>(Arrays.asList(strDeniedFieldsList.split(",")));

				final NodeList fields = docSchem.getElementsByTagName("field"); // Get
																				// the
																				// "field"
																				// Nodes

				try {
					// Get the list of fields in the standard schema.xml
					// file
					for (int i = 0; i < fields.getLength(); i++) {
						JSONObject json = new JSONObject();
						final boolean notIndexed = false;
						final Element elem = (Element) fields.item(i); // Get
																		// a
																		// field
																		// node
						final NamedNodeMap map = elem.getAttributes();
						for (int j = 0; j < map.getLength(); j++) { // Get
																	// its
																	// attributes

							// If the current field is in the denied list or is
							// not indexed or its name starts with '_' or
							// 'allow_' or 'deny_' then ignore it, otherwise add
							// it to the super json
							if ((map.item(j).getNodeName().equals("name")
									&& deniedFieldsSet.contains(map.item(j).getNodeValue()))
									|| ((map.item(j).getNodeName().equals("indexed")
											&& map.item(j).getNodeValue().equals("false"))
											|| (map.item(j).getNodeName().equals("name")
													&& (map.item(j).getNodeValue().startsWith("_")
															|| map.item(j).getNodeValue().startsWith("allow_")
															|| map.item(j).getNodeValue().startsWith("deny_"))))) {
								json = null;
								break;
							} else {
								json.append(map.item(j).getNodeName(), map.item(j).getNodeValue());
							}
						}
						if (json != null) {
							Superjson.append("field", json);
						}
					}
					final PrintWriter out = response.getWriter();
					out.print(Superjson);
				} catch (final JSONException e) {
					logger.error(
							"Error while putting the parameters of a field into a JSON Object in GetFieldsInfo doGet , make sure the schema.xml is valid. Error 69026",
							e);
					final PrintWriter out = response.getWriter();
					out.append(
							"Error while retrieving the fields from the schema.xml, please retry, if the problem persists contact your system administrator. Error Code : 69026");
					out.close();
					return;
				}
			}
		} catch (SAXException | ParserConfigurationException | XPathExpressionException e) {
			logger.error(
					"Error while parsing the schema.xml, in FieldWeight doGet, make sure the file is valid. Error 69027",
					e);
			final PrintWriter out = response.getWriter();
			out.append(
					"Error while parsing the schema.xml, please retry, if the problem persists contact your system administrator. Error Code : 69027");
			out.close();
			return;
		}

	}

}
