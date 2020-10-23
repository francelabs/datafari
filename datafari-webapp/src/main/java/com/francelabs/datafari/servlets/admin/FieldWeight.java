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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
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

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;
import com.francelabs.datafari.utils.FileUtils;
import com.francelabs.datafari.utils.SolrAPI;
import com.francelabs.datafari.utils.SolrConfiguration;
import com.francelabs.datafari.utils.XMLUtils;

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
  private final String env;
  private NodeList childList;
  private Document doc;
  private File config = null;
  /** Custom fields. **/
  private File customFields = null;
  /** Custom requestHandlers. **/
  private File customSearchHandler = null;
  /** Using Custom Search Handler. **/
  private boolean usingCustom = false;
  /** Search Handler. **/
  private Node searchHandler = null;
  private final static Logger LOGGER = LogManager.getLogger(FieldWeight.class.getName());
  private static String mainCollection="FileShare";

  /**
   * @see HttpServlet#HttpServlet() Gets the path Create the semaphore Checks if
   *      the required files exist
   */
  public FieldWeight() {

    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + File.separator + "solr" + File.separator + "solrcloud" + File.separator + "FileShare" + File.separator + "conf";

    
    if (new File(env + File.separator + "solrconfig.xml").exists()) {
      config = new File(env + File.separator + "solrconfig.xml");
    }

    if (new File(env + File.separator + "customs_schema" + File.separator + "custom_fields.incl").exists()) {
      customFields = new File(env + File.separator + "customs_schema" + File.separator + "custom_fields.incl");
    }

    if (new File(env + File.separator + "customs_solrconfig" + File.separator + "custom_search_handler.incl").exists()) {
      customSearchHandler = new File(env + File.separator + "customs_solrconfig" + File.separator + "custom_search_handler.incl");
    }
    
    if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)!= null)
      mainCollection = DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION);
    
    
    
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response) Used to free a semaphore on an other select without any
   *      confirl Checks if the files still exist Gets the list of the fields
   *      Gets the weight of a field in a type of query
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    
    

    final PrintWriter out = response.getWriter();
    usingCustom = false; // Reinit the usingCustom value to false in order
    // to check again if the
    // custom_search_handler.xml file is used

    try {
      if (request.getParameter("sem") != null) { // If it's called just to
        // clean the semaphore
        return;
      }
      if ((config == null) || !new File(env + "/solrconfig.xml").exists()) {// If
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
        if (!new File(env + "/solrconfig.xml").exists()) {
          LOGGER.error("Error while opening the configuration files, solrconfig.xml and/or schema.xml, in FieldWeight doGet, please make sure those files exist at " + env + " . Error 69025"); // If
          // not
          // an
          // error
          // is
          // printed
          out.append("Error while opening the configuration files, please retry, if the problem persists contact your system administrator. Error Code : 69025");
          out.close();
          return;
        } else {
          config = new File(env + "/solrconfig.xml");
        }
      }

      if (customFields == null) {
        if (new File(env + "/customs_schema/custom_fields.incl").exists()) {
          customFields = new File(env + "/customs_schema/custom_fields.incl");
        }
      }

      if (customSearchHandler == null) {
        if (new File(env + "/customs_solrconfig/custom_search_handler.incl").exists()) {
          customSearchHandler = new File(env + "/customs_solrconfig/custom_search_handler.incl");
        }
      }

      if (request.getParameter("type") == null) { // If called at the
        // creation of the HTML
        // ==> retrieve the
        // fields list

        try {
          // Retrieve the Solr hostname, port and protocol
          final String solrserver = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST);
          final String solrport = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT);
          final String protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);

          final HttpClientBuilder builder = HttpClientBuilder.create();

          if (protocol.toLowerCase().equals("https")) {
            try {
              // setup a Trust Strategy that allows all certificates.
              final SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
                  return true;
                }
              }).build();
              final SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
              builder.setSSLSocketFactory(sslConnectionFactory);

              final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sslConnectionFactory).build();

              final HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);

              builder.setConnectionManager(ccm);
            } catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
              LOGGER.warn("Unable to set trust all certificates startegy", e);
            }
          }

          // Use Solr Schema REST API to get the list of fields
          final HttpClient httpClient = builder.build();
          final HttpHost httpHost = new HttpHost(solrserver, Integer.parseInt(solrport), protocol);
          final HttpGet httpGet = new HttpGet("/solr/"+mainCollection+"/schema/fields");
          final HttpResponse httpResponse = httpClient.execute(httpHost, httpGet);

       // Construct the jsonResponse
          final JSONObject jsonResponse = new JSONObject();
          if (httpResponse.getStatusLine().getStatusCode() == 200) {
            // Status of the API response is OK
            final JSONParser parser = new JSONParser();
            final JSONObject json = (JSONObject) parser.parse(EntityUtils.toString(httpResponse.getEntity()));
            final JSONArray fieldsJson = (JSONArray) json.get("fields");
            jsonResponse.put("field", fieldsJson);

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(jsonResponse.toString()); // Answer
            // to
            // the
            // request
            response.setStatus(200);
          } else {
            // Status of the API response is an error
            LOGGER.error("Error while retrieving the fields from the Schema API of Solr: " + httpResponse.getStatusLine().toString());
            out.append("Error while retrieving the fields from the Schema API of Solr, please retry, if the problem persists contact your system administrator. Error Code : 69026");
          }
          out.close();
        } catch (final IOException e) {
          LOGGER.error("Error while retrieving the fields from the Schema API of Solr", e);
          out.append("Error while retrieving the fields from the Schema API of Solr, please retry, if the problem persists contact your system administrator. Error Code : 69026");
          out.close();
        }

      } else { // If the weight of a field has been requested

        try {
          final String type = request.getParameter("type");
          final String field = request.getParameter("field").toString();
          findSearchHandler();

          // if (searchHandler == null && customSearchHandler != null
          // && customSearchHandler.exists()) { // search
          // // handler
          // // not
          // // found
          // // in
          // // the
          // // standard solrconfig.xml find, try
          // // to find it in the
          // // custom_search_handler.xml file
          // usingCustom = true;
          // doc = dBuilder.parse(customSearchHandler);
          // searchHandler =
          // getSearchHandler(doc.getElementsByTagName("requestHandler"));
          // }

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
              if (elemValueCut.indexOf(" ") != -1) {
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
              } else {
                // that
                // is after the "field^"
                response.getWriter().write(elemValue.substring(index));
              }
            } else {
              response.getWriter().write("0");
            }
            response.setStatus(200);
            response.setContentType("text;charset=UTF-8");
            return;
          } else {
            LOGGER.error("No searchHandler found either in solrconfig.xml or in custom_search_handler.xml ! Make sure your files are valid. Error 69028");
            out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69028");
            out.close();
            return;
          }
        } catch (SAXException | ParserConfigurationException e) {
          LOGGER.error("Error while parsing the solrconfig.xml, in FieldWeight doGet, make sure the file is valid. Error 69028", e);
          out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69028");
          out.close();
          return;
        }
      }
    } catch (final Exception e) {
      out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69510");
      out.close();
      LOGGER.error("Unindentified error in FieldWeight doGet. Error 69510", e);
    }
  }

  private void findSearchHandler() throws ParserConfigurationException, SAXException, IOException {
    final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    if ((customSearchHandler != null) && customSearchHandler.exists()) {
      try {
        doc = dBuilder.parse(customSearchHandler);// Parse
        // the
        // solrconfig.xml
        // document
        searchHandler = getSearchHandler(doc.getElementsByTagName("requestHandler"));
        if (searchHandler != null) {
          usingCustom = true;
        }
      } catch (final Exception e) {
        // Not using custom
        usingCustom = false;
      }
    }

    if (searchHandler == null) { // Not using the custom search
      // handler so try to find it
      // in the solrconfig.xml
      // file
      // Not using custom
      usingCustom = false;
      doc = dBuilder.parse(config);// Parse the solrconfig.xml
      // document
      final NodeList fields = (doc.getElementsByTagName("requestHandler"));// Get
      // the
      // requestHandler
      // Node

      searchHandler = getSearchHandler(fields);// Get the
      // search
      // handler from
      // the standard
      // solrconfig.xml
      // file
    }
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response) Checks if the files still exist Used to modify the weight of
   *      a field
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    try {
      if ((config == null) || !new File(env + File.separator + "solrconfig.xml").exists()) {// If
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
        if (!new File(env + File.separator + "solrconfig.xml").exists()) {
          LOGGER.error("Error while opening the configuration file, solrconfig.xml, in FieldWeight doPost, please make sure this file exists at " + env + "conf/ . Error 69029"); // If
          // not
          // an
          // error
          // is
          // printed
          final PrintWriter out = response.getWriter();
          out.append("Error while opening the configuration file, please retry, if the problem persists contact your system administrator. Error Code : 69029");
          out.close();
          return;
        } else {
          config = new File(env + File.separator + "solrconfig.xml");
        }
      }

      if (customSearchHandler == null) {
        if (new File(env + File.separator + "customs_solrconfig" + File.separator + "custom_search_handler.incl").exists()) {
          customSearchHandler = new File(env + File.separator + "customs_solrconfig" + File.separator + "custom_search_handler.incl");
        }
      }

      try {
        final String type = request.getParameter("type");

        if (searchHandler == null) {
          findSearchHandler();
        }

        if (!usingCustom) { // The custom search handler is not used.
          // That means that the current config must
          // be commented in the solrconfig.xml file
          // and the modifications must be saved in
          // the custom search handler file

          // Get the content of solrconfig.xml file as a string
          String configContent = FileUtils.getFileContent(config);

          // Retrieve the searchHandler from the configContent
          final Node originalSearchHandler = XMLUtils.getSearchHandlerNode(config);
          final String strSearchHandler = XMLUtils.nodeToString(originalSearchHandler);

          // Create a commented equivalent of the strSearchHandler
          final String commentedSearchHandler = "<!--" + strSearchHandler + "-->";

          // Replace the searchHandler in the solrconfig.xml file by
          // the commented version
          configContent = configContent.replace(strSearchHandler, commentedSearchHandler);
          FileUtils.saveStringToFile(config, configContent);

          // create the new custom_search_handler document
          doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

          // Import the search handler node from the solrconfig doc to
          // the custom search handler doc
          searchHandler = doc.importNode(searchHandler, true);

          // Make the new node an actual item in the target document
          searchHandler = doc.appendChild(searchHandler);

          final Node n = run(searchHandler.getChildNodes(), type);
          childList = n.getParentNode().getChildNodes();

          // Save the custom_search_handler.incl file
          XMLUtils.docToFile(doc, customSearchHandler);
        }

        if (childList == null) {
          final Node n = run(searchHandler.getChildNodes(), type);
          childList = n.getParentNode().getChildNodes();
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
                  if (textCut.indexOf(" ") == -1) {
                    // field is
                    // the last
                    // one then
                    // we just
                    // cut the
                    // end of
                    // the text
                    // content
                    n.setTextContent((text.substring(0, index)).trim());
                  } else {
                    // the field and the part after
                    n.setTextContent((text.substring(0, index) + text.substring(index + pas + textCut.indexOf(" "))).trim());
                  }
                } else { // If the user typed any other values
                  if (textCut.indexOf(" ") == -1) {
                    n.setTextContent(text.substring(0, index + pas) + value);
                  } else {
                    n.setTextContent(text.substring(0, index + pas) + value + text.substring(index + pas + textCut.indexOf(" ")));
                  }
                }
              } else { // If it's not weighted
                if (!value.equals("0")) {
                  // append the field and
                  // it's value
                  n.setTextContent((n.getTextContent() + " " + field + "^" + value).trim());
                }
              }
              break;
            }
          }
        }
        // Apply the modifications
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        final DOMSource source = new DOMSource(searchHandler);
        final StreamResult result = new StreamResult(customSearchHandler);
        transformer.transform(source, result);

      } catch (final TransformerException e) {
        LOGGER.error("Error while modifying the solrconfig.xml, in FieldWeight doPost, pls make sure the file is valid. Error 69030", e);
        final PrintWriter out = response.getWriter();
        out.append("Error while modifying the config file, please retry, if the problem persists contact your system administrator. Error Code : 69030");
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
          if (map.item(j).getNodeName().equals("name")) {
            name = map.item(j).getNodeValue();
          }
        }
        if (name.equals(type)) {
          return child.item(i);
        }
      }
      if (child.item(i).hasChildNodes()) {
        if (run(child.item(i).getChildNodes(), type) != null) {
          return run(child.item(i).getChildNodes(), type);
        }
      }
    }
    return null;
  }

}
