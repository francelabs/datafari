/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * 
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This Servlet is used to print and modify mail.txt file
 * It is called by alertsAdmin.html
 * DoGet is used to get the value of the fields
 * DoPost is used to modify the value of the fields
 * @author Alexis Karassev
 */
@WebServlet("/admin/MailConf")
public class MailConf extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String env;
	private String content;
	private final static Logger LOGGER = Logger.getLogger(alertsAdmin.class
			.getName());       
	/**
	 * @see HttpServlet#HttpServlet()
	 * Gets the path
	 */
	public MailConf() {
		env = System.getenv("DATAFARI_HOME");									//Gets the directory of installation if in standard environment
		if(env==null){															//If in development environment	
			RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();	//Gets the D.solr.solr.home variable given in arguments to the VM
			List<String> arguments = runtimeMxBean.getInputArguments();
			for(String s : arguments){
				if(s.startsWith("-Dsolr.solr.home"))
					env = s.substring(s.indexOf("=")+1, s.indexOf("solr_home")-5);
			}
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
 	 * Checks if the required file exist
 	 * Checks if it's the administrator that went on alertsAdmin.html
 	 * Read the file and return the values after the "="
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			content="";
			try {																		//Read the file if it exists
				content = readFile(env+"/bin/common/mail.txt", StandardCharsets.UTF_8);
			} catch (NoSuchFileException e1) {
				PrintWriter out = response.getWriter();				
				LOGGER.error("Error while reading the mail.txt in the doGet of the MailConf Servlet. Error 69031 ", e1);
				if(request.isUserInRole("SearchAdministrator")){
					out.append("Error while reading the mail.txt, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69031");
				}
				out.close();
				return;
			}
			try{
				if(request.isUserInRole("SearchAdministrator")){							//If the user is an Admin
					JSONObject json = new JSONObject();										//Put in a JSON all the values
					String[] lines = content.split(System.getProperty("line.separator"));
					for(int i=0; i<lines.length ; i++){
						if(lines[i].startsWith("smtp"))										//Get the host
							json.put("smtp", lines[i].substring(lines[i].indexOf("=")+1,lines[i].length()).trim());
						else if(lines[i].startsWith("from"))								//Get the address			
							json.put("from", lines[i].substring(lines[i].indexOf("=")+1,lines[i].length()).trim());
						else if(lines[i].startsWith("user"))								//Get the user name
							json.put("user", lines[i].substring(lines[i].indexOf("=")+1,lines[i].length()).trim());
						else if(lines[i].startsWith("pass"))								//Get the password
							json.put("pass", lines[i].substring(lines[i].indexOf("=")+1,lines[i].length()).trim());
					}
					response.getWriter().write(json.toString());							//Send the answer
					response.setStatus(200);
					response.setContentType("text/json;charset=UTF-8");
				}else{																		//Else send insufficiant permission
					PrintWriter out = response.getWriter();
					out.append("Insufficiant permission to print mail configuration"); 	
					out.close();
					return;
				}
			}catch (JSONException e){
				PrintWriter out = response.getWriter();				
				LOGGER.error("Error while creating the JSON answer inthe doGet of the MailConf Servlet. Error 69032 ", e);
				out.append("Error while reading the mail.txt, please make sure the file is correctly filled and retry, if the problem persists contact your system administrator. Error code : 69032");
				out.close();
				return;
			}
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69512");
			out.close();
			LOGGER.error("Unindentified error in MailConf doGet. Error 69512", e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * Modify the values according to the parameters
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			String[] lines = content.split(System.getProperty("line.separator"));	//Get the content
			String linesBis = "";
			for(int i=0; i<lines.length ; i++){
				if(lines[i].startsWith("smtp"))										//Set the host
					linesBis += lines[i].substring(0 ,lines[i].indexOf("=")+1)+request.getParameter("SMTP").trim()+"\n";
				else if(lines[i].startsWith("from"))								//Set the address			
					linesBis += lines[i].substring(0 ,lines[i].indexOf("=")+1)+request.getParameter("address").trim()+"\n";
				else if(lines[i].startsWith("user"))								//Set the user name
					linesBis += lines[i].substring(0 ,lines[i].indexOf("=")+1)+request.getParameter("user").trim()+"\n";
				else if(lines[i].startsWith("pass"))								//Set the password
					linesBis += lines[i].substring(0 ,lines[i].indexOf("=")+1)+request.getParameter("pass").trim()+"\n";
				else
					linesBis += lines[i]+"\n";
			}
			FileOutputStream fooStream = new FileOutputStream(new File(env+"/bin/common/mail.txt"), false); 
			byte[] myBytes = linesBis.getBytes();
			fooStream.write(myBytes);										//rewrite the file
			fooStream.close();
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69513");
			out.close();
			LOGGER.error("Unindentified error in MailConf doPost. Error 69513", e);
		}
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

}
