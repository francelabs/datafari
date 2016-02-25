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
package com.francelabs.datafari.servlets.admin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.utils.ExecutionEnvironment;

/**
 * Javadoc
 *
 * This servlet is used to see the various fields or modify the weight of those
 * fields of a Solr core It is called by the FieldWeight.html, IndexField.html
 * and FacetConfig. doGet is used to get the fields and the informations about
 * the fields, also used to clean the semaphore doPost is used to modify the
 * weight of a field The semaphores (one for each type of query) are created in
 * the constructor.
 *
 * @author Alexis Karassev
 */
@WebServlet("/admin/FieldWeight")
public class FieldWeight extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final String server = Core.FILESHARE.toString();
	private final SemaphoreLn semaphoreConfigPf;
	private final SemaphoreLn semaphoreConfigQf;
	private String env;
	private NodeList childList;
	private Document doc;
	private File schema = null;
	private File config = null;
	/** Custom fields. **/
	private File customFields = null;
	/** Custom requestHandlers. **/
	private File customSearchHandler = null;
	/** Using Custom Search Handler. **/
	private boolean usingCustom = false;
	/** Search Handler. **/
	private Node searchHandler = null;
	private final static Logger LOGGER = Logger.getLogger(FieldWeight.class.getName());

	/**
	 * @see HttpServlet#HttpServlet() Gets the path Create the semaphore Checks
	 *      if the required files exist
	 */
	public FieldWeight() {
		env = System.getenv("DATAFARI_HOME"); // Gets the directory of
												// installation if in standard
												// environment
		if (env == null) { // If in development environment
			env = ExecutionEnvironment.getDevExecutionEnvironment();
		}
		semaphoreConfigPf = new SemaphoreLn("", "pf");
		semaphoreConfigQf = new SemaphoreLn("", "qf");
		if (new File(env + "/solr/solr_home/" + server + "/conf/schema.xml").exists()) { // Check
																							// if
																							// the
																							// files
																							// exists
			schema = new File(env + "/solr/solr_home/" + server + "/conf/schema.xml");
		}
		if (new File(env + "/solr/solr_home/" + server + "/conf/solrconfig.xml").exists()) {
			config = new File(env + "/solr/solr_home/" + server + "/conf/solrconfig.xml");
		}

		if (new File(env + "/solr/solr_home/" + server + "/conf/customs_schema/custom_fields.incl").exists()) {
			customFields = new File(env + "/solr/solr_home/" + server + "/conf/customs_schema/custom_fields.incl");
		}

		if (new File(env + "/solr/solr_home/" + server + "/conf/customs_solrconfig/custom_search_handler.incl").exists()) {
			customSearchHandler = new File(env + "/solr/solr_home/" + server + "/conf/customs_solrconfig/custom_search_handler.incl");
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response) Used to free a semaphore on an other select without any
	 *      confirl Checks if the files still exist Gets the list of the fields
	 *      Gets the weight of a field in a type of query
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		usingCustom = false; // Reinit the usingCustom value to false in order
								// to check again if the
								// custom_search_handler.xml file is used

		try {
			if (request.getParameter("sem") != null) { // If it's called just to
														// clean the semaphore
				if (request.getParameter("type").equals("pf") && semaphoreConfigPf.availablePermits() < 1)
					semaphoreConfigPf.release();
				else if (semaphoreConfigQf.availablePermits() < 1)
					semaphoreConfigQf.release();
				return;
			}
			if (schema == null || config == null || !new File(env + "/solr/solr_home/" + server + "/conf/schema.xml").exists()
					|| !new File(env + "/solr/solr_home/" + server + "/conf/solrconfig.xml").exists()) {// If
																										// the
																										// files
																										// did
																										// not
																										// existed
																										// when
																										// the
																										// constructor
																										// was
																										// run
				// Checks if they exist now
				if (!new File(env + "/solr/solr_home/" + server + "/conf/schema.xml").exists()
						|| !new File(env + "/solr/solr_home/" + server + "/conf/solrconfig.xml").exists()) {
					LOGGER.error(
							"Error while opening the configuration files, solrconfig.xml and/or schema.xml, in FieldWeight doGet, please make sure those files exist at "
									+ env + "/solr/solr_home/" + server + "/conf/ . Error 69025"); // If
																									// not
																									// an
																									// error
																									// is
																									// printed
					final PrintWriter out = response.getWriter();
					out.append(
							"Error while opening the configuration files, please retry, if the problem persists contact your system administrator. Error Code : 69025");
					out.close();
					return;
				} else {
					schema = new File(env + "/solr/solr_home/" + server + "/conf/schema.xml");// Else
																								// they
																								// are
																								// prepared
																								// to
																								// be
																								// parsed
					config = new File(env + "/solr/solr_home/" + server + "/conf/solrconfig.xml");
				}
			}

			if (customFields == null) {
				if (new File(env + "/solr/solr_home/" + server + "/conf/customs_schema/custom_fields.incl").exists()) {
					customFields = new File(env + "/solr/solr_home/" + server + "/conf/customs_schema/custom_fields.incl");
				}
			}

			if (customSearchHandler == null) {
				if (new File(env + "/solr/solr_home/" + server + "/conf/customs_solrconfig/custom_search_handler.incl").exists()) {
					customSearchHandler = new File(env + "/solr/solr_home/" + server + "/conf/customs_solrconfig/custom_search_handler.incl");
				}
			}

			final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			if (request.getParameter("type") == null) { // If called at the
														// creation of the HTML
				try {
					final JSONObject Superjson = new JSONObject();
					final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					final Document docSchem = dBuilder.parse(schema); // Parse
																		// the
																		// schema
					final NodeList fields = docSchem.getElementsByTagName("field"); // Get
																					// the
																					// "field"
																					// Nodes

					final DocumentBuilder customdb = dbFactory.newDocumentBuilder();
					final Document customFieldsD = customdb.parse(customFields); // Parse
																					// the
																					// custom_fields.xml
																					// file

					try {
						// Get the list of fields in the standard schema.xml
						// file
						for (int i = 0; i < fields.getLength(); i++) {
							final JSONObject json = new JSONObject();
							final Element elem = (Element) fields.item(i); // Get
																			// a
																			// field
																			// node
							final NamedNodeMap map = elem.getAttributes();
							for (int j = 0; j < map.getLength(); j++) { // Get
																		// its
																		// attributes
								json.append(map.item(j).getNodeName(), map.item(j).getNodeValue());
							}
							Superjson.append("field", json);
						}
					} catch (final JSONException e) {
						LOGGER.error(
								"Error while putting the parameters of a field into a JSON Object in FieldWeight doGet , make sure the schema.xml is valid. Error 69026",
								e);
						final PrintWriter out = response.getWriter();
						out.append(
								"Error while retrieving the fields from the schema.xml, please retry, if the problem persists contact your system administrator. Error Code : 69026");
						out.close();
						return;
					}
					response.getWriter().write(Superjson.toString()); // Answer
																		// to
																		// the
																		// request
					response.setStatus(200);
					response.setContentType("text/json;charset=UTF-8");
				} catch (SAXException | ParserConfigurationException e) {
					LOGGER.error("Error while parsing the schema.xml, in FieldWeight doGet, make sure the file is valid. Error 69027", e);
					final PrintWriter out = response.getWriter();
					out.append(
							"Error while parsing the schema.xml, please retry, if the problem persists contact your system administrator. Error Code : 69027");
					out.close();
					return;
				}
			} else { // If the weight of a field has been requested
				try {
					final String type = request.getParameter("type");
					if (type.equals("pf")) { // Select a semaphore according to
												// the type and acquire it
												// because the file can be now
												// modified in a click
						if (semaphoreConfigPf.availablePermits() > 0)
							semaphoreConfigPf.acquire();
						else { // If the selected Semaphore is already acquired
								// return "File already in use"
							final PrintWriter out = response.getWriter();
							out.append("File already in use");
							out.close();
							return;
						}
					} else {
						if (semaphoreConfigQf.availablePermits() > 0)
							semaphoreConfigQf.acquire();
						else {
							final PrintWriter out = response.getWriter();
							out.append("File already in use");
							out.close();
							return;
						}
					}
					final String field = request.getParameter("field").toString();
					final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					doc = dBuilder.parse(config);// Parse the solrconfig.xml
													// document
					final NodeList fields = (doc.getElementsByTagName("requestHandler"));// Get
																							// the
																							// requestHandler
																							// Node

					searchHandler = getSearchHandler(fields);// Get the search
					// handler from
					// the standard
					// solrconfig.xml
					// file

					if (searchHandler == null && customSearchHandler != null && customSearchHandler.exists()) { // search
						// handler
						// not
						// found
						// in
						// the
						// standard solrconfig.xml find, try
						// to find it in the
						// custom_search_handler.xml file
						usingCustom = true;
						doc = dBuilder.parse(customSearchHandler);
						searchHandler = getSearchHandler(doc.getElementsByTagName("requestHandler"));
					}

					if (searchHandler != null) {
						final Node n = run(searchHandler.getChildNodes(), type);
						childList = n.getParentNode().getChildNodes();
						final String elemValue = n.getTextContent(); // Get the
																		// text
																		// content
																		// of
																		// the
																		// node
						int index = elemValue.indexOf(field + "^");
						if (index != -1) { // If the field is weighted
							index += field.length() + 1; // Gets the number of
															// the
															// char that is the
															// first figure of
															// the
															// number
							final String elemValueCut = elemValue.substring(index);// Get
																					// the
																					// text
																					// content
																					// cutting
																					// everything
																					// before
																					// the
																					// requested
																					// field
							if (elemValueCut.indexOf(" ") != -1) // If it's not
																	// the
																	// last
																	// field,
																	// returns
																	// what's
																	// between
																	// the
																	// "^" and
																	// the
																	// next
																	// whitespace
								response.getWriter().write(elemValue.substring(index, index + elemValueCut.indexOf(" ")));
							else // If it is the last field, return everything
									// that
									// is after the "field^"
								response.getWriter().write(elemValue.substring(index));
						} else // If the field is not present return 0
							response.getWriter().write("0");
						response.setStatus(200);
						response.setContentType("text;charset=UTF-8");
						return;
					} else {
						LOGGER.error(
								"No searchHandler found either in solrconfig.xml or in custom_search_handler.xml ! Make sure your files are valid. Error 69028");
						final PrintWriter out = response.getWriter();
						out.append(
								"Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69028");
						out.close();
						return;
					}
				} catch (SAXException | ParserConfigurationException | InterruptedException e) {
					LOGGER.error("Error while parsing the solrconfig.xml, in FieldWeight doGet, make sure the file is valid. Error 69028", e);
					final PrintWriter out = response.getWriter();
					out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69028");
					out.close();
					return;
				}
			}
		} catch (final Exception e) {
			final PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69510");
			out.close();
			LOGGER.error("Unindentified error in FieldWeight doGet. Error 69510", e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response) Checks if the files still exist Used to modify the weight
	 *      of a field
	 */
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		try {
			if (config == null || !new File(env + "/solr/solr_home/" + server + "/conf/solrconfig.xml").exists()) {// If
																													// the
																													// files
																													// did
																													// not
																													// existed
																													// when
																													// the
																													// constructor
																													// was
																													// runned
				if (!new File(env + "/solr/solr_home/" + server + "/conf/solrconfig.xml").exists()) {
					LOGGER.error(
							"Error while opening the configuration file, solrconfig.xml, in FieldWeight doPost, please make sure this file exists at "
									+ env + "/solr/solr_home/" + server + "/conf/ . Error 69029"); // If
																									// not
																									// an
																									// error
																									// is
																									// printed
					final PrintWriter out = response.getWriter();
					out.append(
							"Error while opening the configuration file, please retry, if the problem persists contact your system administrator. Error Code : 69029");
					out.close();
					return;
				} else {
					config = new File(env + "/solr/solr_home/" + server + "/conf/solrconfig.xml");
				}
			}

			if (customSearchHandler == null) {
				if (new File(env + "/solr/solr_home/" + server + "/conf/customs_solrconfig/custom_search_handler.incl").exists()) {
					customSearchHandler = new File(env + "/solr/solr_home/" + server + "/conf/customs_solrconfig/custom_search_handler.incl");
				}
			}

			try {
				final String type = request.getParameter("type");

				if (!usingCustom) { // The custom search handler is not used.
									// That means that the current config must
									// be commented in the solrconfig.xml file
									// and the modifications must be saved in
									// the custom search handler file

					// Comment the current searchHandler in the solrconfig.xml
					// file
					final Transformer tf = TransformerFactory.newInstance().newTransformer();
					tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
					tf.setOutputProperty(OutputKeys.INDENT, "yes");
					final Writer out = new StringWriter();
					tf.transform(new DOMSource(searchHandler), new StreamResult(out));
					String comment = out.toString().substring(38); // Create the
																	// string
																	// comment
																	// and
																	// removing
																	// the xml
																	// starting
																	// tag
																	// (<?xml
																	// version="1.0"
																	// encoding="UTF-8"
																	// standalone="no"?>)
																	// thanks to
																	// the
																	// substring
					comment = comment.substring(0, comment.lastIndexOf("\n")); // Remove
																				// the
																				// last
																				// carrier
																				// return
																				// to
																				// avoid
																				// useless
																				// whitespace
																				// at
																				// the
																				// end
																				// of
																				// the
																				// comment
					final Comment xmlComment = doc.createComment(comment); // Create
																			// the
																			// node
																			// comment
					searchHandler.getParentNode().insertBefore(xmlComment, searchHandler); // Insert
																							// the
																							// comment
																							// node
																							// before
																							// the
																							// searchHandler
																							// node
					searchHandler.getParentNode().removeChild(searchHandler); // Remove
																				// the
																				// searchHandler
																				// node
																				// from
																				// the
																				// solrconfig
																				// doc
					tf.transform(new DOMSource(doc), new StreamResult(config)); // Save
																				// the
																				// modifications

					// create the new custom_search_handler document
					doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					searchHandler = doc.importNode(searchHandler, true); // Import
																			// the
																			// search
																			// handler
																			// node
																			// from
																			// the
																			// solrconfig
																			// doc
																			// to
																			// the
																			// custom
																			// search
																			// handler
																			// doc
					// Make the new node an actual item in the target document
					searchHandler = doc.appendChild(searchHandler);

					final Node n = run(searchHandler.getChildNodes(), type);
					childList = n.getParentNode().getChildNodes();

					// Save the custom_search_handler.incl file
					tf.transform(new DOMSource(doc), new StreamResult(customSearchHandler));
				}

				for (int i = 0; i < childList.getLength(); i++) { // Get the str
																	// node
					final Node n = childList.item(i);
					if (n.getNodeName().equals("str")) {
						String name = ""; // Get it's attributes
						final NamedNodeMap map = n.getAttributes();
						for (int j = 0; j < map.getLength(); j++) {
							if (map.item(j).getNodeName().equals("name")) {// Get
																			// the
																			// name
								name = map.item(j).getNodeValue();
							}
						}
						if (name.equals(type)) { // If it's pf or qf according
													// to what the user selected
							// Get the parameters
							final String field = request.getParameter("field").toString();
							final String value = request.getParameter("value").toString();
							final String text = n.getTextContent(); // Get the
																	// value of
																	// the node,
							// Search for the requested field, if it is there
							// return the weight, if not return 0
							final int index = text.indexOf(field + "^");
							if (index != -1) { // If the field is already
												// weighted
								final int pas = field.length() + 1;
								final String textCut = text.substring(index + pas);
								if (value.equals("0")) { // If the user entered
															// 0
									if (textCut.indexOf(" ") == -1) // If the
																	// field is
																	// the last
																	// one then
																	// we just
																	// cut the
																	// end of
																	// the text
																	// content
										n.setTextContent((text.substring(0, index)).trim());
									else // If it's not we get the part before
											// the field and the part after
										n.setTextContent((text.substring(0, index) + text.substring(index + pas + textCut.indexOf(" "))).trim());
								} else { // If the user typed any other values
									if (textCut.indexOf(" ") == -1)
										n.setTextContent(text.substring(0, index + pas) + value);
									else
										n.setTextContent(text.substring(0, index + pas) + value + text.substring(index + pas + textCut.indexOf(" ")));
								}
							} else { // If it's not weighted
								if (!value.equals("0")) // If the value is not 0
														// append the field and
														// it's value
									n.setTextContent((n.getTextContent() + " " + field + "^" + value).trim());
							}
							break;
						}
					}
				}
				// Apply the modifications
				final TransformerFactory transformerFactory = TransformerFactory.newInstance();
				final Transformer transformer = transformerFactory.newTransformer();
				final DOMSource source = new DOMSource(doc);
				final StreamResult result = new StreamResult(customSearchHandler);
				transformer.transform(source, result);
				// Release the Semaphore according to the type
				if (type.equals("pf") && semaphoreConfigPf.availablePermits() < 1)
					semaphoreConfigPf.release();
				else if (semaphoreConfigQf.availablePermits() < 1)
					semaphoreConfigQf.release();

			} catch (final TransformerException e) {
				LOGGER.error("Error while modifying the solrconfig.xml, in FieldWeight doPost, pls make sure the file is valid. Error 69030", e);
				final PrintWriter out = response.getWriter();
				out.append(
						"Error while modifying the config file, please retry, if the problem persists contact your system administrator. Error Code : 69030");
				out.close();
				return;
			}

		} catch (final Exception e) {
			final PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69511");
			out.close();
			LOGGER.error("Unindentified error in FieldWeight doPost. Error 69511", e);
		}
	}

	/**
	 * Retrieve the default search handler '/select' from a list of request
	 * handlers
	 *
	 * @param requestHandlers
	 * @return the search handler or null if not found
	 */
	private Node getSearchHandler(final NodeList requestHandlers) {
		for (int i = 0; i < requestHandlers.getLength(); i++) {

			final Node rh = requestHandlers.item(i);
			if (rh.hasAttributes()) {
				final NamedNodeMap rhAttributes = rh.getAttributes();
				for (int j = 0; j < rhAttributes.getLength(); j++) {
					if (rhAttributes.item(j).getNodeName().equals("name") && rhAttributes.item(j).getNodeValue().equals("/select")) {
						return rh;
					}
				}
			}

		}
		return null;
	}

	private Node run(final NodeList child, final String type) {
		for (int i = 0; i < child.getLength(); i++) {
			String name = "";
			if (child.item(i).hasAttributes()) {
				final NamedNodeMap map = child.item(i).getAttributes();
				for (int j = 0; j < map.getLength(); j++) {
					if (map.item(j).getNodeName().equals("name"))
						name = map.item(j).getNodeValue();
				}
				if (name.equals(type))
					return child.item(i);
			}
			if (child.item(i).hasChildNodes())
				if (run(child.item(i).getChildNodes(), type) != null)
					return run(child.item(i).getChildNodes(), type);
		}
		return null;
	}

}
