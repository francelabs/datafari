package com.francelabs.datafari.servlets.admin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

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
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.Environment;

@WebServlet("/admin/CheckMCFConfiguration")
public class CheckMCFConfiguration extends HttpServlet {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private static final String CONF_PARAM = "configuration";
  private static final String FILER_CONF = "filer";
  private static final String FILER_EXPRESSION = "/connectors/repositoryconnector[@class='com.francelabs.datafari.connectors.share.SharedDriveConnector']";
  private static final String REGISTER_PARAM = "registered";

  private static final Logger logger = LogManager.getLogger(CheckMCFConfiguration.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public CheckMCFConfiguration() {

  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final String confToCheck = request.getParameter(CONF_PARAM);

    jsonResponse.put(REGISTER_PARAM, false);

    if (confToCheck != null) {

      String envPath = Environment.getEnvironmentVariable("MCF_HOME");
      if (envPath == null) {
        envPath = "/opt/datafari/mcf/mcf_home";
      }
      final File connectors = new File(envPath + "/connectors.xml");
      final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      try {
        final DocumentBuilder builder = builderFactory.newDocumentBuilder();
        final Document xmlDocument = builder.parse(connectors);
        final XPath xPath = XPathFactory.newInstance().newXPath();

        if (confToCheck.equals(FILER_CONF)) {
          final NodeList nodeList = (NodeList) xPath.compile(FILER_EXPRESSION).evaluate(xmlDocument, XPathConstants.NODESET);
          if (nodeList.getLength() > 0) {
            jsonResponse.put(REGISTER_PARAM, true);
          }
        }
      } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Unable to read the MCF connectors.xml file");
        logger.error("Unable to read the MCF config", e);
      }
    }

    jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

}
