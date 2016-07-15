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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.JSONResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RTimer;
import org.apache.solr.util.RTimerTree;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.service.search.SolrServers;
import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.statistics.StatsProcessor;
import com.francelabs.datafari.statistics.StatsPusher;
import com.francelabs.datafari.utils.RealmLdapConfiguration;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Servlet implementation class SearchProxy
 */
@WebServlet("/SearchProxy/*")
public class SearchProxy extends HttpServlet implements SolrQueryRequest {
	private static final long serialVersionUID = 1L;

	private static String domain = "corp.francelabs.com";

	private static final List<String> allowedHandlers = Arrays.asList("/select", "/suggest", "/stats", "/statsQuery");

	private static final Logger LOGGER = Logger.getLogger(SearchProxy.class.getName());

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		final String handler = getHandler(request);

		if (!allowedHandlers.contains(handler)) {
			log("Unauthorized handler");
			response.setStatus(401);
			response.setContentType("text/html");
			final PrintWriter out = response.getWriter();
			out.println("<HTML>");
			out.println("<HEAD><TITLE>Unauthorized Handler</TITLE></HEAD>");
			out.println("<BODY>");
			out.println("The handler is not authorized.");
			out.print("Only these handlers are authorized : ");
			for (final String allowedHandler : allowedHandlers) {
				out.print(allowedHandler + " ");
			}
			out.println("</BODY></HTML>");
			return;
		}

		SolrClient solr;
		SolrClient promolinkCore = null;
		QueryResponse queryResponse = null;
		QueryResponse queryResponsePromolink = null;
		final SolrQuery query = new SolrQuery();
		final SolrQuery queryPromolink = new SolrQuery();

		final ModifiableSolrParams params = new ModifiableSolrParams();
		try {
			// get the AD domain
			final HashMap<String, String> h = RealmLdapConfiguration.getConfig(request);
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
					String AuthenticatedUserName = request.getUserPrincipal().getName().replaceAll("[^\\\\]*\\\\", "");
					if (!domain.equals("")) {
						AuthenticatedUserName += "@" + domain;
					}
					params.set("AuthenticatedUserName", AuthenticatedUserName);
				}

				final String queryParam = params.get("query");
				if (queryParam != null) {
					params.set("q", queryParam);
					params.remove("query");
				}

				break;
			}

			params.add(new ModifiableSolrParams(request.getParameterMap()));

			try {
				if (ScriptConfiguration.getProperty("ontologyEnabled").toLowerCase().equals("true")
						&& ScriptConfiguration.getProperty("ontologyEnabled").toLowerCase().equals("true")
						&& handler.equals("/select")) {
					final boolean languageSelection = Boolean
							.valueOf(ScriptConfiguration.getProperty("ontologyLanguageSelection"));
					String parentsLabels = ScriptConfiguration.getProperty("ontologyParentsLabels");
					String childrenLabels = ScriptConfiguration.getProperty("ontologyChildrenLabels");
					if (languageSelection) {
						parentsLabels += "_fr";
						childrenLabels += "_fr";
					}
					final int facetFieldLength = params.getParams("facet.field").length;
					final String[] facetFields = Arrays.copyOf(params.getParams("facet.field"), facetFieldLength + 2);
					facetFields[facetFieldLength] = "{!ex=" + parentsLabels + "}" + parentsLabels;
					facetFields[facetFieldLength + 1] = "{!ex=" + childrenLabels + "}" + childrenLabels;
					params.set("facet.field", facetFields);
				}
			} catch (final IOException e) {
				LOGGER.warn("Ignored ontology facets because of error: " + e.toString());
			}

			// perform query

			query.add(params);
			query.setRequestHandler(handler);
			queryResponse = solr.query(query);
			if (promolinkCore != null && !(params.get("q").toString().equals("*:*"))) { // launch
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
																						// onZ
																						// the
																						// FileShare
																						// core

				queryPromolink.setQuery(params.get("q"));
				queryPromolink.setFilterQueries("-dateBeginning:[NOW/DAY+1DAY TO *]", "-dateEnd:[* TO NOW/DAY]");
				queryResponsePromolink = promolinkCore.query(queryPromolink);
			}
			switch (handler) {
			case "/select":
				// index
				final long numFound = queryResponse.getResults().getNumFound();
				final int QTime = queryResponse.getQTime();
				final ModifiableSolrParams statsParams = new ModifiableSolrParams(params);
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
				writeSolrJResponse(request, response, query, queryResponse, queryPromolink, queryResponsePromolink);
			} else {
				writeSolrJResponse(request, response, query, queryResponse, null, null);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	private void writeSolrJResponse(final HttpServletRequest request, final HttpServletResponse response,
			final SolrQuery query, final QueryResponse queryResponse, final SolrQuery queryPromolink,
			final QueryResponse queryResponsePromolink) throws IOException, JSONException, ParseException {
		final SolrQueryRequest req = new SolrQueryRequest() {

			@Override
			public void close() {
				// TODO Auto-generated method stub

			}

			@Override
			public Iterable<ContentStream> getContentStreams() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<Object, Object> getContext() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SolrCore getCore() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, Object> getJSON() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SolrParams getOriginalParams() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getParamString() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SolrParams getParams() {
				// TODO Auto-generated method stub
				return query;
			}

			@Override
			public RTimerTree getRequestTimer() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IndexSchema getSchema() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SolrIndexSearcher getSearcher() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public long getStartTime() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public Principal getUserPrincipal() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setJSON(Map<String, Object> arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setParams(SolrParams arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void updateSchemaToLatest() {
				// TODO Auto-generated method stub

			}

		};
		if (queryResponsePromolink != null) { // If it was a request on
												// FileShare
			// therefore on promolink
			final SolrQueryResponse res = new SolrQueryResponse();
			res.setAllValues(queryResponse.getResponse());
			final JSONResponseWriter jsonWriter = new JSONResponseWriter();
			StringWriter s = new StringWriter();

			jsonWriter.write(s, req, res);
			final JSONObject json = new JSONObject(s.toString().substring(s.toString().indexOf("{"))); // Creating
																										// a
																										// valid
																										// json
																										// object
																										// from
																										// the
																										// results

			res.setAllValues(queryResponsePromolink.getResponse());
			s = new StringWriter();
			jsonWriter.write(s, req, res); // Write the result of the query on
											// promolink

			if (queryResponsePromolink.getResults().getNumFound() != 0) { // If
																			// there
																			// are
																			// a
																			// result
																			// for
																			// the
																			// promolink

				JSONObject promoResponseJSON = new JSONObject();
				SolrDocument promoLinkDocument = queryResponsePromolink.getResults().get(0);
				for (String fieldName : queryResponsePromolink.getResults().get(0).getFieldNames()) {
					promoResponseJSON.put(fieldName, promoLinkDocument.get(fieldName));
				}
				// Taking
				// just
				// the
				// results
				// without
				// the
				// header

				json.put("promolinkSearchComponent", promoResponseJSON);

			}
			final String wrapperFunction = request.getParameter("json.wrf");
			final String finalString = wrapperFunction + "(" + json.toString() + ")";
			response.setStatus(200);
			response.setCharacterEncoding("utf-8");
			response.setContentType("text/json;charset=utf-8");
			response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
			response.getWriter().write(finalString); // Send the answer to the
														// jsp page

		} else {
			final SolrQueryResponse res = new SolrQueryResponse();
			final JSONResponseWriter json = new JSONResponseWriter();
			response.setStatus(200);
			response.setCharacterEncoding("utf-8");
			response.setContentType("text/json;charset=utf-8");
			response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
			res.setAllValues(queryResponse.getResponse());
			json.write(response.getWriter(), req, res);

		}
	}

	private String getHandler(final HttpServletRequest servletRequest) {
		final String pathInfo = servletRequest.getPathInfo();
		return pathInfo.substring(pathInfo.lastIndexOf("/"), pathInfo.length());
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public Iterable<ContentStream> getContentStreams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Object, Object> getContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SolrCore getCore() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getJSON() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SolrParams getOriginalParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getParamString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SolrParams getParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RTimerTree getRequestTimer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexSchema getSchema() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SolrIndexSearcher getSearcher() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getStartTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setJSON(Map<String, Object> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setParams(SolrParams arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSchemaToLatest() {
		// TODO Auto-generated method stub

	}

}