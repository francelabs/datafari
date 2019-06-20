/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

@WebServlet("/SearchAdministrator/MCFChangePassword")
public class MCFChangePassword extends HttpServlet {
  private final String env;
  private static final long serialVersionUID = 1L;
  private final static Logger LOGGER = LogManager.getLogger(MCFChangePassword.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public MCFChangePassword() {
    super();

    String environnement = System.getenv("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + "/mcf/mcf_home";

  }

  /**
   * @throws IOException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    String newMCFPassword = request.getParameter("password");
    try {
      newMCFPassword = ManifoldCF.obfuscate(newMCFPassword);
    } catch (final ManifoldCFException e) {
      LOGGER.error("Exception during MCF change password ", e);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
    }
    modifyPropertiesMCF(newMCFPassword);

    String datafari_home = Environment.getEnvironmentVariable("DATAFARI_HOME"); // Gets

    if (datafari_home == null) { // If in development environment
      datafari_home = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    final String mcf_path = datafari_home + "/mcf/mcf_home/";
    final String scriptname = "setglobalproperties.sh";

    final String[] command = { "/bin/bash", "-c", "cd " + mcf_path + " && bash " + scriptname };
    final ProcessBuilder p = new ProcessBuilder(command);
    final Process p2 = p.start();

    final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));

    final BufferedReader stdError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));

    // read the output from the command
    String s = null;
    String errorCode = null;
    while ((s = stdInput.readLine()) != null) {
      LOGGER.info(s);
    }

    // read any errors from the attempted command
    // TODO too verbose, display messages that are not errors
    while ((s = stdError.readLine()) != null) {
      LOGGER.warn(s);
      errorCode = s;
    }

    // TODO handle exception in shell script better
    final PrintWriter out = response.getWriter();
    /*
     * if (errorCode == null) jsonResponse.put(OutputConstants.CODE,
     * CodesReturned.ALLOK.getValue()); else
     * jsonResponse.put(OutputConstants.CODE,
     * CodesReturned.GENERALERROR.getValue());
     */
    jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    out.print(jsonResponse);

  }

  protected void modifyPropertiesMCF(final String password) {

    try {

      final File file = new File(env + "/properties-global.xml");

      final DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

      final Document doc = dBuilder.parse(file);

      final Element element = doc.getDocumentElement();
      System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
      final NodeList nodeList = element.getElementsByTagName("property");
      System.out.println(nodeList.getLength());
      if (nodeList.getLength() > 0) {
        for (int i = 0; i < nodeList.getLength(); i++) {
          // System.out.println("test");
          final Element elementAttribute = (Element) nodeList.item(i);
          final NamedNodeMap nodeMap = elementAttribute.getAttributes();

          if (nodeMap.getLength() >= 1) {
            final Node node = nodeMap.item(0);
            final Node node2 = nodeMap.item(1);
            if (node.getNodeValue().toString().equals("org.apache.manifoldcf.login.password.obfuscated") || node.getNodeValue().toString().equals("org.apache.manifoldcf.apilogin.password.obfuscated")) {
              node2.setTextContent(password);
              node2.setTextContent(password);

            }
          }

        }
      }
      final TransformerFactory transformerFactory = TransformerFactory.newInstance();
      final Transformer transformer = transformerFactory.newTransformer();
      final DOMSource source = new DOMSource(doc);
      final StreamResult result = new StreamResult(new File(env + "/properties-global.xml"));
      transformer.transform(source, result);

    } catch (final Exception e) {
      LOGGER.error("Unable to modify MCF properties", e);
    }
  }
}
