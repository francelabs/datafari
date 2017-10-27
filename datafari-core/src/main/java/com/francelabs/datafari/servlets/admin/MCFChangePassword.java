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

import org.apache.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

@WebServlet("/SearchAdministrator/MCFChangePassword")
public class MCFChangePassword extends HttpServlet {
	private final String env;
	private static final String confname = "FileShare";
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger(MCFChangePassword.class.getName());

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
		} catch (ManifoldCFException e) {
			LOGGER.error("Exception during MCf change password ", e);
			jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
		}
		modifyPropertiesMCF(newMCFPassword);
		
		String datafari_home = Environment.getEnvironmentVariable("DATAFARI_HOME"); // Gets
		
		if (datafari_home == null) { // If in development environment
				datafari_home = ExecutionEnvironment.getDevExecutionEnvironment();
		}
		String mcf_path = datafari_home + "/mcf/mcf_home/";
		String scriptname = "setglobalproperties.sh";
		
		String[] command = { "/bin/bash","-c", "cd "+mcf_path+" && bash "+scriptname};
		ProcessBuilder p = new ProcessBuilder(command);
		Process p2 = p.start();
		
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));

		BufferedReader stdError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));

		// read the output from the command
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			LOGGER.info(s);
		}

		// read any errors from the attempted command
		while ((s = stdError.readLine()) != null) {
			LOGGER.warn(s);
		}
		
		// script
	
		final PrintWriter out = response.getWriter();
		jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
		out.print(jsonResponse);

		

	}

	protected void modifyPropertiesMCF(String password) {
		
		try {

			File file = new File(env+"/properties-global.xml");

			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();

			Document doc = dBuilder.parse(file);

			Element element = doc.getDocumentElement();
			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
			NodeList nodeList = element.getElementsByTagName("property");
			System.out.println(nodeList.getLength());
			if (nodeList.getLength() > 0) {
				for (int i=0; i< nodeList.getLength(); i++){
					//System.out.println("test");
					Element elementAttribute = (Element) nodeList.item(i);
					NamedNodeMap nodeMap = elementAttribute.getAttributes();


					if (nodeMap.getLength() >= 1) {
						Node node = nodeMap.item(0);
						Node node2 = nodeMap.item(1);
						if ((node.getNodeValue().toString().equals("org.apache.manifoldcf.login.password.obfuscated")) ||(node.getNodeValue().toString().equals("org.apache.manifoldcf.apilogin.password.obfuscated"))) {
							node2.setTextContent(password);
				
						}
					}

				}
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(env+"/properties-global.xml"));
			transformer.transform(source, result);

			System.out.println("Done");


		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
