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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.JSONResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RTimer;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.service.search.SolrServers;
import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.statistics.StatsProcessor;
import com.francelabs.datafari.statistics.StatsPusher;

/**
 * Servlet implementation class SearchProxy
 */
@WebServlet("/SearchProxy/*")
public class SearchProxy extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static String domain = "corp.francelabs.com";

	private static final List<String> allowedHandlers = Arrays.asList(
			"/select", "/suggest", "/stats", "/statsQuery");

	private static final Logger LOGGER = Logger.getLogger(SearchProxy.class
			.getName());

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String handler = getHandler(request);

		if (!allowedHandlers.contains(handler)) {
			log("Unauthorized handler");
			response.setStatus(401);
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();
			out.println("<HTML>");
			out.println("<HEAD><TITLE>Unauthorized Handler</TITLE></HEAD>");
			out.println("<BODY>");
			out.println("The handler is not authorized.");
			out.print("Only these handlers are authorized : ");
			for (String allowedHandler : allowedHandlers) {
				out.print(allowedHandler + " ");
			}
			out.println("</BODY></HTML>");
			return;
		}

		SolrClient solr;
		SolrClient promolinkCore = null;
		QueryResponse queryResponse = null;
		QueryResponse queryResponseBis = null;
		SolrQuery query = new SolrQuery();
		SolrQuery queryBis = new SolrQuery();

		ModifiableSolrParams params = new ModifiableSolrParams();
		try {
			switch (handler) {
			case "/stats":
			case "/statsQuery":
				solr = SolrServers.getSolrServer(Core.STATISTICS);
				break;
			default:
				solr = SolrServers.getSolrServer(Core.FILESHARE);
				promolinkCore = SolrServers.getSolrServer(Core.PROMOLINK);

				// Add authentication
				if (request.getUserPrincipal() != null) {
					String AuthenticatedUserName = request.getUserPrincipal()
							.getName().replaceAll("[^\\\\]*\\\\", "");
					if (!domain.equals("")) {
						AuthenticatedUserName += "@" + domain;
					}
					params.set("AuthenticatedUserName", AuthenticatedUserName);
				}

				String queryParam = params.get("query");
				if (queryParam != null) {
					params.set("q", queryParam);
					params.remove("query");
				}
				
				
				break;
			}

			params.add(new ModifiableSolrParams(request.getParameterMap()));
			
			// perform query
			
			
			query.add(params);
			query.setRequestHandler(handler);
			queryResponse = solr.query(query);
			if (promolinkCore != null
					&& !(params.get("q").toString().equals("*:*"))) { // launch
																		// a
																		// request
																		// in
																		// the
																		// promolink
																		// core
																		// only
																		// if
																		// it's
																		// a
																		// request
																		// on
																		// the
																		// FileShare
																		// core
				if (params.get("q").startsWith("\"")) { // and if it's not an
														// empty request
					queryBis.setQuery(params.get("q"));
				} else {
					queryBis.setQuery("\"" + params.get("q") + "\"");
					queryBis.set("q.op", "AND"); // Ensure that all queries on
													// the promolink core will
													// be in exact expression
				}
				queryResponseBis = promolinkCore.query(queryBis);
			}
			switch (handler) {
			case "/select":
				// index
				long numFound = queryResponse.getResults().getNumFound();
				int QTime = queryResponse.getQTime();
				ModifiableSolrParams statsParams = new ModifiableSolrParams(
						params);
				statsParams.add("numFound", Long.toString(numFound));
				if (numFound == 0) {
					statsParams.add("noHits", "1");
				}
				statsParams.add("QTime", Integer.toString(QTime));
				StatsPusher.pushQuery(statsParams);
				break;
			case "/stats":
				StatsProcessor.processStatsResponse(queryResponse);
				break;
			}

			if (promolinkCore != null) {
				writeSolrJResponse(request, response, query, queryResponse,
						queryBis, queryResponseBis);
			} else {
				writeSolrJResponse(request, response, query, queryResponse,
						null, null);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void writeSolrJResponse(HttpServletRequest request,
			HttpServletResponse response, final SolrQuery query,
			QueryResponse queryResponse, final SolrQuery queryBis,
			QueryResponse queryResponseBis) throws IOException, JSONException,
			ParseException {
		SolrQueryRequest req = new SolrQueryRequest() {
			@Override
			public SolrParams getParams() {
				return query;
			}

			@Override
			public void setParams(SolrParams params) {
			}

			@Override
			public Iterable<ContentStream> getContentStreams() {
				return null;
			}

			@Override
			public SolrParams getOriginalParams() {
				return null;
			}

			@Override
			public Map<Object, Object> getContext() {
				return null;
			}

			@Override
			public void close() {
			}

			@Override
			public long getStartTime() {
				return 0;
			}

			@Override
			public SolrIndexSearcher getSearcher() {
				return null;
			}

			@Override
			public SolrCore getCore() {
				return null;
			}

			@Override
			public IndexSchema getSchema() {
				return null;
			}

			@Override
			public String getParamString() {
				return null;
			}

			@Override
			public void updateSchemaToLatest() {

			}

			@Override
			public Map<String, Object> getJSON() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public RTimer getRequestTimer() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setJSON(Map<String, Object> arg0) {
				// TODO Auto-generated method stub

			}

		};
		if (queryResponseBis != null) { // If it was a request on FileShare
										// therefore on promolink
			SolrQueryResponse res = new SolrQueryResponse();
			res.setAllValues(queryResponse.getResponse());
			JSONResponseWriter jsonWriter = new JSONResponseWriter();
			StringWriter s = new StringWriter();

			jsonWriter.write(s, req, res);
			JSONObject json = new JSONObject(s.toString().substring(
					s.toString().indexOf("{"))); // Creating a valid json object
													// from the results

			res.setAllValues(queryResponseBis.getResponse());
			s = new StringWriter();
			jsonWriter.write(s, req, res); // Write the result of the query on
											// promolink

			if ((s.toString().charAt(10 + s.toString().indexOf("numFound"))) != '0') { // If
																						// there
																						// are
																						// a
																						// result
																						// for
																						// the
																						// promolink
				JSONObject jsonTmp = new JSONObject(s.toString().substring(
						7 + s.toString().indexOf("docs"),
						s.toString().length() - 3)); // Taking just the results
														// without the header
				if (jsonTmp.toString().indexOf("dateBeginning") == -1
						&& jsonTmp.toString().indexOf("dateEnd") == -1) // If
																		// there
																		// is
																		// not a
																		// single
																		// date
																		// then
																		// we
																		// put
																		// the
																		// promolink
																		// in
																		// the
																		// results
					json.put("promolinkSearchComponent", jsonTmp); // Put the
																	// promolink
																	// into the
																	// results
																	// of the
																	// first
																	// query
				else if (jsonTmp.toString().indexOf("dateBeginning") != -1) { // If
																				// there
																				// is
																				// a
																				// starting
																				// date
					DateFormat dateFormat = new SimpleDateFormat(
							"yyyy-MM-dd'T'HH:mm:ss");
					String d1 = jsonTmp.get("dateBeginning").toString();
					d1 = d1.substring(2, d1.length() - 3);
					Date date = new Date(); // Get the current date
					Date date1 = dateFormat.parse(d1); // Parse the starting
														// date to a valid
														// format
					if (jsonTmp.toString().indexOf("dateEnd") != -1) { // If
																		// there
																		// is an
																		// ending
																		// date
						String d2 = jsonTmp.get("dateEnd").toString();
						d2 = d2.substring(2, d2.length() - 3);
						Date date2 = dateFormat.parse(d2); // Parse it to a
															// valid format
						if (date.compareTo(date1) > 0
								&& date.compareTo(date2) < 0) // If the starting
																// date is prior
																// to the
																// current date
							json.put("promolinkSearchComponent", jsonTmp); // And
																			// the
																			// ending
																			// date
																			// is
																			// after
																			// the
																			// current
																			// date
					} else { // If there is no ending date
						if (date.compareTo(date1) > 0) // If the starting date
														// is prior to the
														// current date
							json.put("promolinkSearchComponent", jsonTmp);
					}
				} else { // If there is no starting date
					DateFormat dateFormat = new SimpleDateFormat(
							"yyyy-MM-dd'T'HH:mm:ss");
					String d1 = jsonTmp.get("dateEnd").toString();
					d1 = d1.substring(2, d1.length() - 3);
					Date date = new Date(); // Get the current date
					Date date1 = dateFormat.parse(d1); // Parse the ending date
														// to a valid format
					if (date.compareTo(date1) < 0) // If the ending date is
													// after the current date
						json.put("promolinkSearchComponent", jsonTmp);
				}
			}
			String wrapperFunction = request.getParameter("json.wrf");
			String finalString = wrapperFunction + "(" + json.toString() + ")";
			response.getWriter().write(finalString); // Send the answer to the
														// jsp page
			response.setStatus(200);
			response.setContentType("text/json;charset=UTF-8");
		} else {
			SolrQueryResponse res = new SolrQueryResponse();
			JSONResponseWriter json = new JSONResponseWriter();
			res.setAllValues(queryResponse.getResponse());
			json.write(response.getWriter(), req, res);
			response.setStatus(200);
			response.setContentType("text/json;charset=UTF-8");
		}
	}

	private String getHandler(HttpServletRequest servletRequest) {
		String pathInfo = servletRequest.getPathInfo();
		return pathInfo.substring(pathInfo.lastIndexOf("/"), pathInfo.length());
	}

}