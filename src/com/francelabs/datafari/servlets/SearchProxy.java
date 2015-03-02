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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import com.francelabs.datafari.solrj.SolrServers;
import com.francelabs.datafari.solrj.SolrServers.Core;
import com.francelabs.datafari.statistics.StatsProcessor;
import com.francelabs.datafari.statistics.StatsPusher;

/**
 * Servlet implementation class SearchProxy
 */
@WebServlet("/SearchProxy/*")
public class SearchProxy extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final List<String> allowedHandlers = Arrays.asList(
			"/select", "/suggest", "/stats", "/statsQuery");

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

		SolrServer solr;
		QueryResponse queryResponse = null;
		SolrQuery query = new SolrQuery();

		ModifiableSolrParams params = new ModifiableSolrParams(
				request.getParameterMap());

		try {
			switch (handler) {
			case "/stats":
			case "/statsQuery":
				solr = SolrServers.getSolrServer(Core.STATISTICS);
				break;
			default:
				solr = SolrServers.getSolrServer(Core.FILESHARE);
				/*
				 * if (request.getUserPrincipal() != null) { String
				 * AuthenticatedUserName = request.getUserPrincipal()
				 * .getName().replaceAll("[^\\\\]*\\\\", ""); if
				 * (!domain.equals("")) { AuthenticatedUserName += "@" + domain;
				 * } params.set("AuthenticatedUserName", AuthenticatedUserName);
				 * }
				 */

				String queryParam = params.get("query");
				if (queryParam != null) {
					params.set("q", queryParam);
					params.remove("query");
				}
				break;
			}

			// perform query
			query.add(params);
			query.setRequestHandler(handler);
			queryResponse = solr.query(query);

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

			writeSolrJResponse(request, response, query, queryResponse);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void writeSolrJResponse(HttpServletRequest request,
			HttpServletResponse response, final SolrQuery query,
			QueryResponse queryResponse) throws IOException {
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

		};

		SolrQueryResponse res = new SolrQueryResponse();
		res.setAllValues(queryResponse.getResponse());

		JSONResponseWriter json = new JSONResponseWriter();
		json.write(response.getWriter(), req, res);

		response.setStatus(200);
		response.setContentType("text/json");

	}

	private String getHandler(HttpServletRequest servletRequest) {
		String pathInfo = servletRequest.getPathInfo();
		return pathInfo.substring(pathInfo.lastIndexOf("/"), pathInfo.length());
	}

}
