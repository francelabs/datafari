package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrException;

import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;

/**
 * Servlet implementation class AddTokenLimit
 */
@WebServlet("/admin/ModifyFieldType")
public class ModifyFieldType extends HttpServlet {

  private final static Logger LOGGER = LogManager.getLogger(ModifyFieldType.class.getName());

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
   * class = name of the filter type = attr in filter
   *
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    String value = "";

    try {

      // example : maxTokenCount
      final String type = request.getParameter("type");
      // class : solr.LimitTokenCountFilterFactory
      final String clazz = request.getParameter("class");

      // use schema API to get field type
      final IndexerServer server = IndexerServerManager.getIndexerServer(Core.FILESHARE);

      value = server.getAnalyzerFilterValue(clazz, type);

      // keep compatibility with some other admin servlets but we
      // should definitely change that with a JSON response
      final PrintWriter out = response.getWriter();
      out.append(value); // Return it's content
      out.close();

    } catch (final SolrException e) {
      // keep compatibility with some other admin servlets but we
      // should definitely change that with a JSON response
      LOGGER.error("Error while ModifyFiedType doGet, make sure the file is valid. Error 69012", e);
      final PrintWriter out = response.getWriter();
      out.append(
          "Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69034");
      out.close();
      return;
    } catch (

    final Exception e) {
      e.printStackTrace();
      final PrintWriter out = response.getWriter();
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
   * class = name of the filter type = attribute of the filter value = value to
   * change
   *
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    try {
      // example : maxTokenCount
      final String type = request.getParameter("type");
      // example : solr.LimitTokenCountFilterFactory
      final String clazz = request.getParameter("class");
      final String value = request.getParameter("value");

      final IndexerServer server = IndexerServerManager.getIndexerServer(Core.FILESHARE);
      server.updateAnalyzerFilterValue(clazz, type, value);

      // keep compatibility with some other admin servlets but we
      // should definitely change that with a JSON response
      final PrintWriter out = response.getWriter();
      out.append(value); // Return it's content
      out.close();

    } catch (final SolrException e) {
      LOGGER.error("Error while modifying the solrconfig.xml, in ModifyNodeContent doPost. Error 69036", e);
      final PrintWriter out = response.getWriter();
      out.append(
          "Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69036");
      out.close();
      return;
    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append(
          "Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69515");
      out.close();
      LOGGER.error("Unindentified error in ModifyNodeContent doPost. Error 69515", e);
    }
  }
}
