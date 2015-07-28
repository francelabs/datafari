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
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.francelabs.datafari.solrj.SolrServers.Core;

/**Javadoc
 * 
 * This servlet is used to see the various fields or modify the weight of those fields of a Solr core
 * It is only called by the FieldWeight.html.
 * doGet is used to get the fields and the informations about the fields, also used to clean the semaphore
 * doPost is used to modify the weight of a field
 * The semaphores (one for each type of query) are created in the constructor.
 * @author Alexis Karassev
 */
@WebServlet("/admin/FieldWeight")
public class FieldWeight extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String server = Core.FILESHARE.toString();
	private SemaphoreLn semaphoreConfigPf = new SemaphoreLn("", "pf");
	private SemaphoreLn semaphoreConfigQf = new SemaphoreLn("", "qf");
	private String env;
	private NodeList childList;
	private Document doc;
	private File schema =null;
	private File config = null;
	private final static Logger LOGGER = Logger.getLogger(FieldWeight.class
			.getName());

	/**
	 * @see HttpServlet#HttpServlet()
	 * Gets the path
	 * Create the semaphore
	 * Checks if the required files exist
	 */
	public FieldWeight() {
		env = System.getenv("DATAFARI_HOME");									//Gets the directory of installation if in standard environment
		if(env==null){															//If in development environment	
			RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();	//Gets the D.solr.solr.home variable given in arguments to the VM
			List<String> arguments = runtimeMxBean.getInputArguments();
			for(String s : arguments){
				if(s.startsWith("-Dsolr.solr.home"))
					env = s.substring(s.indexOf("=")+1, s.indexOf("solr_home")-5);
			}
		}
		semaphoreConfigPf = new SemaphoreLn("", "pf");
		semaphoreConfigQf = new SemaphoreLn("", "qf");
		if(new File(env+"/solr/solr_home/"+server+"/conf/schema.xml").exists())	//Check if the files exists
			schema = new File(env+"/solr/solr_home/"+server+"/conf/schema.xml");
		if(new File(env+"/solr/solr_home/"+server+"/conf/solrconfig.xml").exists())
			config = new File(env+"/solr/solr_home/"+server+"/conf/solrconfig.xml");
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Used to free a semaphore on an other select without any confirl
	 * Checks if the files still exist
	 * Gets the list of the fields
	 * Gets the weight of a field in a type of query
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(request.getParameter("sem") != null){									//If it's called just to clean the semaphore
			if(request.getParameter("type").equals("pf") && semaphoreConfigPf.availablePermits()<1)
				semaphoreConfigPf.release(); 
			else if(semaphoreConfigQf.availablePermits()<1)
				semaphoreConfigQf.release();
			return;
		}
		if( schema == null || config == null || !new File(env+"/solr/solr_home/"+server+"/conf/schema.xml").exists() || !new File(env+"/solr/solr_home/"+server+"/conf/solrconfig.xml").exists()){//If the files did not existed when the constructor was runned
			//Checks if they exist now
			if(!new File(env+"/solr/solr_home/"+server+"/conf/schema.xml").exists() || !new File(env+"/solr/solr_home/"+server+"/conf/solrconfig.xml").exists()){
				LOGGER.error("Error while opening the configuration files");		//If not an error is printed
				PrintWriter out = response.getWriter();
				out.append("Error while opening the configuration files"); 	
				out.close();
				return;
			}else{
				schema = new File(env+"/solr/solr_home/"+server+"/conf/schema.xml");//Else they are prepared to be parsed
				config = new File(env+"/solr/solr_home/"+server+"/conf/solrconfig.xml");
			}
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		if(request.getParameter("type")==null){						//If called at the creation of the HTML
			try{
				JSONObject Superjson = new JSONObject();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document docSchem = dBuilder.parse(schema);					//Parse the schema
				NodeList fields = docSchem.getElementsByTagName("field");	//Get the "field" Nodes
				try{
					for(int i = 0 ; i < fields.getLength() ; i ++){			
						JSONObject json = new JSONObject(); 
						Element elem = (Element)fields.item(i);				//Get a field node
						NamedNodeMap map = elem.getAttributes();
						for(int j=0; j<map.getLength() ; j++){
							json.append(map.item(j).getNodeName(), map.item(j).getNodeValue());
						}
						Superjson.append("field", json);
					}
				}catch(JSONException e){
					LOGGER.error("Error while appending values into the JSONObject", e);
					PrintWriter out = response.getWriter();
					out.append("Something bad happened, please retry, if the problem persists contact your system administrator"); 	
					out.close();
					throw new RuntimeException();
				}
				response.getWriter().write(Superjson.toString());			//Answer to the request
				response.setStatus(200);
				response.setContentType("text/json;charset=UTF-8");
			}catch(SAXException | ParserConfigurationException e){
				LOGGER.error("Error while parsing the schema.xml", e);
				PrintWriter out = response.getWriter();
				out.append("Something bad happened, please retry, if the problem persists contact your system administrator"); 	
				out.close();
				throw new RuntimeException();
			}
		}else{															//If the weight of a field has been requested
			try{
				String type = request.getParameter("type");
				if(type.equals("pf")){									//Select a semaphore according to the type and acquire it because the file can be now modified in a click
					if(semaphoreConfigPf.availablePermits()>0)
						semaphoreConfigPf.acquire();
					else{												//If the selected Semaphore is already acquired return "File already in use"
						PrintWriter out = response.getWriter();
						out.append("File already in use"); 	
						out.close();
						return;
					}
				}else{
					if(semaphoreConfigQf.availablePermits()>0)
						semaphoreConfigQf.acquire();
					else{
						PrintWriter out = response.getWriter();
						out.append("File already in use"); 	
						out.close();
						return;
					}
				}
				String field = request.getParameter("field").toString();//Parse the solrconfig.xml document
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				doc = dBuilder.parse(config);
				Node n = null;
				NodeList fields = (doc.getElementsByTagName("requestHandler"));//Get the requestHandler Noes
				for(int i = 0 ; i < fields.getLength() ; i++){			//Select the "/select" one
					Element elem = (Element)fields.item(i);
					if(elem.getAttribute("name").equals("/select")){
						n = fields.item(i);
						break;
					}
				}
				childList = n.getChildNodes();					//Get his childs
				for(int i=0;i<childList.getLength();i++){				//Get the lst node
					if(childList.item(i).getNodeName().equals("lst")){
						n = childList.item(i);
						break;
					}
				}
				childList = n.getChildNodes();							//Get his childs
				for(int i=0;i<childList.getLength();i++){				//Get the str node	
					n = childList.item(i);
					if(n.getNodeName().equals("str")){
						String name ="";								//Get it's attributes
						NamedNodeMap map = n.getAttributes();
						for(int j=0; j<map.getLength() ; j++){
							if(map.item(j).getNodeName().equals("name")){//Get the name
								name = map.item(j).getNodeValue();
							}
						}
						if(name.equals(type)){							//If it's pf or qf according to what the user selected
							//Get the value of the node, search for the requested field, if it is there return the weight, if not return 0
							String elemValue = n.getTextContent();		//Get the text content of the node
							int index = elemValue.indexOf(field+"^");
							if(index!=-1){								//If the field is weighted
								index += field.length()+1;				//Gets the number of the char that is the first figure of the number
								String elemValueCut = elemValue.substring(index);//Get the text content cutting everything before the requested field
								if(elemValueCut.indexOf(" ")!=-1)		//If it's not the last field, returns what's between the "^" and the next whitespace
									response.getWriter().write(elemValue.substring(index, index+elemValueCut.indexOf(" ")));
								else									//If it is the last field, return everything that is after the "field^"
									response.getWriter().write(elemValue.substring(index));
							}else										//If the field is not present return 0
								response.getWriter().write("0");
							response.setStatus(200);
							response.setContentType("text;charset=UTF-8");
							return;
						}
					}
				}
			}catch(SAXException | ParserConfigurationException | InterruptedException e){
				LOGGER.error("Error while parsing the solrconfig.xml", e);
				PrintWriter out = response.getWriter();
				out.append("Something bad happened, please retry, if the problem persists contact your system administrator"); 	
				out.close();
				throw new RuntimeException();
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * Checks if the files still exist
	 * Used to modify the weight of a field
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(config == null || !new File(env+"/solr/solr_home/"+server+"/conf/schema.xml").exists() || !new File(env+"/solr/solr_home/"+server+"/conf/solrconfig.xml").exists()){//If the files did not existed when the constructor was runned
			//Checks if they exist now
			if(!new File(env+"/solr/solr_home/"+server+"/conf/schema.xml").exists() || !new File(env+"/solr/solr_home/"+server+"/conf/solrconfig.xml").exists()){
				LOGGER.error("Error while opening the solrconfig file");		//If not an error is printed
				PrintWriter out = response.getWriter();
				out.append("Error while opening the solrconfig file"); 	
				out.close();
				return;
			}else{
				schema = new File(env+"/solr/solr_home/"+server+"/conf/schema.xml");//Else they are prepared to be parsed
				config = new File(env+"/solr/solr_home/"+server+"/conf/solrconfig.xml");
			}
		}
		try{
			String type = request.getParameter("type");
			Node n = null;
			for(int i=0; i<childList.getLength(); i++){				//Get the str node	
				n = childList.item(i);
				if(n.getNodeName().equals("str")){
					String name ="";								//Get it's attributes
					NamedNodeMap map = n.getAttributes();
					for(int j=0; j<map.getLength() ; j++){
						if(map.item(j).getNodeName().equals("name")){//Get the name
							name = map.item(j).getNodeValue(); 
						}
					}
					if(name.equals(type)){	//If it's pf or qf according to what the user selected
						//Get the parameters
						String field = request.getParameter("field").toString();	
						String value = request.getParameter("value").toString();
						String text = n.getTextContent();			//Get the value of the node,
						//Search for the requested field, if it is there return the weight, if not return 0
						int index = text.indexOf(field+"^");
						if(index!=-1){								//If the field is already weighted
							int pas = field.length()+1;
							String textCut = text.substring(index+pas);
							if(value.equals("0")){					//If the user entered 0
								if(textCut.indexOf(" ")==-1)		//If the field is the last one then we just cut the end of the text content
									n.setTextContent((text.substring(0,index)).trim());
								else								//If it's not we get the part before the field and the part after
									n.setTextContent((text.substring(0,index)+text.substring(index+pas+textCut.indexOf(" "))).trim());
							}else{									//If the user typed any other values
								if(textCut.indexOf(" ")==-1)
									n.setTextContent(text.substring(0, index+pas)+value);
								else
									n.setTextContent(text.substring(0, index+pas)+value+text.substring(index+pas+textCut.indexOf(" ")));
							}
						}else{										//If it's not weighted
							if(!value.equals("0"))					//If the value is not 0 append the field and it's value
								n.setTextContent((n.getTextContent()+" "+field+"^"+value).trim());
						}
						break;
					}
				}
			}
			//Apply the modifications	
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(config);
			transformer.transform(source, result);
			//Release the Semaphore according to the type
			if(type.equals("pf") && semaphoreConfigPf.availablePermits()<1)
				semaphoreConfigPf.release();
			else if(semaphoreConfigQf.availablePermits()<1)
				semaphoreConfigQf.release();

		}catch(TransformerException e){
			LOGGER.error("Error while modifying the solrconfig.xml", e);
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator"); 	
			out.close();
			throw new RuntimeException();
		}
	}
}
