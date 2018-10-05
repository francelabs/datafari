package com.francelabs.datafari.utils;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.francelabs.datafari.exception.CodesReturned;

public class ActivateLDAPSolr {
  private static File solrConfig;
  private static File customSearchHandler;
  private static ActivateLDAPSolr instance;
  private final static Logger logger = LogManager.getLogger(ActivateLDAPSolr.class);

  private ActivateLDAPSolr() {
    final String solrconfigPath = Environment.getEnvironmentVariable("SOLR_INSTALL_DIR") + File.separator + "solrcloud" + File.separator + "FileShare" + File.separator + "conf" + File.separator + "solrconfig.xml";
    solrConfig = new File(solrconfigPath);
    final String authorityConnectionJSONPath = Environment.getEnvironmentVariable("DATAFARI_HOME") + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator
        + "authorityconnections" + File.separator + "authorityConnection.json";
    final String authorityGroupsJSONPath = Environment.getEnvironmentVariable("DATAFARI_HOME") + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "monoinstance"
        + File.separator + "authorityconnections" + File.separator + "authorityGroups.json";
    new File(authorityConnectionJSONPath);
    new File(authorityGroupsJSONPath);

    // Custom searchHandler
    final String customSearchHandlerPath = Environment.getEnvironmentVariable("SOLR_INSTALL_DIR") + File.separator + "solrcloud" + File.separator + "FileShare" + File.separator + "conf" + File.separator + "customs_solrconfig" + File.separator
        + "custom_search_handler.incl";
    customSearchHandler = new File(customSearchHandlerPath);
  }

  private static ActivateLDAPSolr getInstance() {
    if (instance == null) {
      return instance = new ActivateLDAPSolr();
    }
    return instance;
  }

  public static int activate() throws Exception {
    final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    File fileToParse = getInstance().customSearchHandler;
    Document docSchem;
    try {
      docSchem = dBuilder.parse(fileToParse);
    } catch (final Exception e) {
      fileToParse = getInstance().solrConfig;
      docSchem = dBuilder.parse(fileToParse);
    }

    final XPathFactory xPathfactory = XPathFactory.newInstance();
    final XPath xpath = xPathfactory.newXPath();
    XPathExpression expr = xpath.compile("//requestHandler[@class=\"solr.SearchHandler\" and @name=\"/select\"]");
    final Node requestHandler = (Node) expr.evaluate(docSchem, XPathConstants.NODE);

    if (requestHandler != null) {
      expr = xpath.compile("//lst[@name=\"appends\"]");
      final Node appends = (Node) expr.evaluate(requestHandler, XPathConstants.NODE);
      if (appends == null) {
        // Save the current searchHandler version
        final String oldSearchHandler = XMLUtils.nodeToString(requestHandler);

        // Add the manifoldCFSecurity to the searchHandler
        final Element elementRoot = docSchem.createElement("lst");
        final Element elementChild = docSchem.createElement("str");
        elementRoot.setAttribute("name", "appends");
        elementChild.setAttribute("name", "fq");
        elementChild.appendChild(docSchem.createTextNode("{!manifoldCFSecurity}"));
        elementRoot.appendChild(elementChild);
        requestHandler.appendChild(elementRoot);

        // Replace the old searchHandler by the new one
        String configContent = FileUtils.getFileContent(fileToParse);
        final String newSearchHandler = XMLUtils.nodeToString(requestHandler);
        configContent = configContent.replace(oldSearchHandler, newSearchHandler);

        // Save the file
        FileUtils.saveStringToFile(fileToParse, configContent);
      }
    }

    return CodesReturned.ALLOK.getValue();
  }

  public static int disactivate() throws Exception {
    final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    File fileToParse = getInstance().customSearchHandler;
    Document docSchem;
    try {
      docSchem = dBuilder.parse(fileToParse);
    } catch (final Exception e) {
      fileToParse = getInstance().solrConfig;
      docSchem = dBuilder.parse(fileToParse);
    }

    final XPathFactory xPathfactory = XPathFactory.newInstance();
    final XPath xpath = xPathfactory.newXPath();
    XPathExpression expr = xpath.compile("//requestHandler[@class=\"solr.SearchHandler\" and @name=\"/select\"]");
    final Node requestHandler = (Node) expr.evaluate(docSchem, XPathConstants.NODE);

    if (requestHandler != null) {
      expr = xpath.compile("//lst[@name=\"appends\"]");
      final Node appends = (Node) expr.evaluate(requestHandler, XPathConstants.NODE);
      if (appends != null) {
        // Save the current searchHandler version
        final String oldSearchHandler = XMLUtils.nodeToString(requestHandler);

        // Remove the manifoldCFSecurity
        requestHandler.removeChild(appends);

        // Replace the old searchHandler by the new one
        String configContent = FileUtils.getFileContent(fileToParse);
        final String newSearchHandler = XMLUtils.nodeToString(requestHandler);
        configContent = configContent.replace(oldSearchHandler, newSearchHandler);

        // Save the file
        FileUtils.saveStringToFile(fileToParse, configContent);
      }
    }

    return CodesReturned.ALLOK.getValue();
  }
}
