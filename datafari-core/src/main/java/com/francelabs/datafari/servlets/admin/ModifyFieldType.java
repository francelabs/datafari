package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.FieldTypes;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.ReplaceFieldType;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Update;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.FieldTypesResponse;
import org.apache.solr.common.SolrException;

import com.francelabs.datafari.service.search.SolrServers;
import com.francelabs.datafari.service.search.SolrServers.Core;

/**
 * Servlet implementation class AddTokenLimit
 */
@WebServlet("/admin/ModifyFieldType")
public class ModifyFieldType extends HttpServlet {

	private final static Logger LOGGER = Logger.getLogger(ModifyFieldType.class.getName());

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ModifyFieldType() {
		super();
		// TODO Auto-generated constructor stub
	}


	/**
	 * Get current value for a filter of an analyzer type for text_* fields
	 * 
	 * class = name of the filter
	 * type = attr in filter
	 * 
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String value = "";

		try {

			// example : maxTokenCount
			String type = request.getParameter("type");
			// class : solr.LimitTokenCountFilterFactory
			String clazz = request.getParameter("class");

			
			// use schema API to get field type
			SolrClient solrClient = SolrServers.getSolrServer(Core.FILESHARE);
			FieldTypes solrRequest = new FieldTypes();
			FieldTypesResponse solrResponse = new FieldTypesResponse();
			solrResponse.setResponse(solrClient.request(solrRequest));

			for (FieldTypeRepresentation fieldType : solrResponse.getFieldTypes()) {
				String name = (String) fieldType.getAttributes().get("name");
				// get all analyzers for text_* field
				if (name != null && name.startsWith("text_")) {
					List<AnalyzerDefinition> analyzers = new ArrayList<AnalyzerDefinition>();
					analyzers.add(fieldType.getAnalyzer());
					analyzers.add(fieldType.getIndexAnalyzer());
					analyzers.add(fieldType.getQueryAnalyzer());
					for (AnalyzerDefinition analyzer : analyzers) {
						if (analyzer != null) {
							List<Map<String, Object>> filters = analyzer.getFilters();
							if (filters != null) {
								for (Map<String, Object> filter : filters) {
									if (filter != null) {
										String clazzAttr = (String) filter.get("class");
										if (clazzAttr != null && clazzAttr.equals(clazz)) {
											// get last value 
											value = (String) filter.get(type);
										}
									}
								}
							}
						}
					}
				}
			}

			// keep compatibility with some other admin servlets but we
			// should definitely change that with a JSON response
			PrintWriter out = response.getWriter();
			out.append(value); // Return it's content
			out.close();

		} catch (SolrException e) {
			// keep compatibility with some other admin servlets but we
			// should definitely change that with a JSON response
			LOGGER.error("Error while ModifyFiedType doGet, make sure the file is valid. Error 69012", e);
			PrintWriter out = response.getWriter();
			out.append(
					"Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69034");
			out.close();
			return;
		} catch (

		Exception e) {
			e.printStackTrace();
			PrintWriter out = response.getWriter();
			// keep compatibility with some other admin servlets but we should
			// definitely change that with a JSON response

			out.append(
					"Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69514");
			out.close();
			LOGGER.error("Unindentified error in ModifyFiedType doGet. Error 69212", e);
		}

	}


	/**
	 * Set the current value for a filter of an analyzer type
	 * 
	 * class = name of the filter
	 * type = attribute of the filter
	 * value = value to change
	 * 
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			// example : maxTokenCount
			String type = request.getParameter("type");
			// example : solr.LimitTokenCountFilterFactory
			String clazz = request.getParameter("class");
			String value = request.getParameter("value");

			SolrClient solrClient = SolrServers.getSolrServer(Core.FILESHARE);
			FieldTypes solrRequest = new FieldTypes();
			FieldTypesResponse solrResponse = new FieldTypesResponse();
			solrResponse.setResponse(solrClient.request(solrRequest));
			List<Update> updates = new ArrayList<Update>();
			for (FieldTypeRepresentation fieldType : solrResponse.getFieldTypes()) {
				String name = (String) fieldType.getAttributes().get("name");
				if (name != null && name.startsWith("text_")) {
					List<AnalyzerDefinition> analyzers = new ArrayList<AnalyzerDefinition>();
					analyzers.add(fieldType.getAnalyzer());
					analyzers.add(fieldType.getIndexAnalyzer());
					analyzers.add(fieldType.getQueryAnalyzer());
					for (AnalyzerDefinition analyzer : analyzers) {
						if (analyzer != null) {
							List<Map<String, Object>> filters = analyzer.getFilters();
							if (filters != null) {
								for (Map<String, Object> filter : filters) {
									if (filter != null) {
										String clazzAttr = (String) filter.get("class");
										if (clazzAttr != null && clazzAttr.equals(clazz)) {
											filter.put(type, value);
											// keep each modified fieldType definition 
											updates.add(new ReplaceFieldType(fieldType));
										}
									}
								}
							}
						}
					}
				}
			}
			

			// send a bulk update of each modified fieldType definition
			MultiUpdate multiUpdateRequest = new MultiUpdate(updates);
			solrClient.request(multiUpdateRequest);

			// keep compatibility with some other admin servlets but we
			// should definitely change that with a JSON response
			PrintWriter out = response.getWriter();
			out.append(value); // Return it's content
			out.close();

		} catch (SolrException e) {
			LOGGER.error("Error while modifying the solrconfig.xml, in ModifyNodeContent doPost. Error 69036", e);
			PrintWriter out = response.getWriter();
			out.append(
					"Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69036");
			out.close();
			return;
		} catch (Exception e) {
			PrintWriter out = response.getWriter();
			out.append(
					"Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69515");
			out.close();
			LOGGER.error("Unindentified error in ModifyNodeContent doPost. Error 69515", e);
		}
	}
}
