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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.htmlparser.jericho.Source;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.NodeVisitor;

import com.ctc.wstx.io.CharsetNames;
import com.francelabs.datafari.utils.ExecutionEnvironment;
import com.google.common.base.Strings;
/**
 * This Servlet is used to add, delete facets, and modify the printing order.
 * It is called by FacetConfig.html
 * DoGet is used to create and release semaphores, but also to get the existing facets, and their order.
 * DoPost is used to add or delete facets, and modify the printing order
 * @author Alexis Karassev
 */
@WebServlet("/admin/FacetConfig")
public class FacetConfig extends HttpServlet {
	//Rhino reader
	class SearchFacet implements NodeVisitor {
		@Override public boolean visit(AstNode node) {
			//If it's an expression
			if(node.getClass().toString().equals("class org.mozilla.javascript.ast.ExpressionStatement")){
				ExpressionStatement exprnode = (ExpressionStatement) node;
				String expression = exprnode.toSource();
				//If it's a facet creation
				if(expression.startsWith("Manager.addWidget(new AjaxFranceLabs.TableWidget({") || expression.startsWith("Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({")){
					//Get the div name of the jsp
					listDivName.add(expression.substring(expression.indexOf('#')+1, expression.indexOf(',')-2));
					expression = expression.substring(expression.indexOf("field"));
					//Get the field name
					listFieldsName.add(expression.substring(expression.indexOf("'")+1, expression.indexOf(",")-1));
				}
			}
			return true;
		}
	}
	private static final long serialVersionUID = 1L;
	private static final Semaphore sem = new Semaphore(1);
	private String env;
	private JSONObject json = new JSONObject();
	private JSONObject superJson = new JSONObject();
	private List<String> listDivName;
	private List<String> listFieldsName;
	private File jsp = null;
	private File js = null;
	private File en = null;
	private File fr = null;
	private File it = null;
	private final static Logger LOGGER = Logger.getLogger(FacetConfig.class);
	
	// TODO To be moved to a property file
	private static final String LOCALE_PATH_DEV = "/WebContent/js/AjaxFranceLabs/locale/";
	private static final String LOCALE_PATH_PROD = "js/AjaxFranceLabs/locale/";
	private static final String DATAFARI_PATH_PROD = "/tomcat/webapps/Datafari/";
	
	private static final String IT_JSON = "it.json";
	private static final String EN_JSON = "en.json";
	private static final String FR_JSON = "fr.json";
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public FacetConfig() {
		//gets the files
		// TODO DATAFARI_HOME to be moved to a property file
		env = System.getenv("DATAFARI_HOME");									//Gets the directory of installation if in standard environment
		if(env==null){															//If in development environment	
			env = ExecutionEnvironment.getDevExecutionEnvironment();
			jsp = new File(env+"/WebContent/searchView.jsp");
			js = new File(env+"/WebContent/js/search.js");
			en = new File(env + LOCALE_PATH_DEV + EN_JSON);
			fr = new File(env + LOCALE_PATH_DEV + FR_JSON);
			it = new File(env + LOCALE_PATH_DEV + IT_JSON);
		}else{
			jsp = new File(env + DATAFARI_PATH_PROD + "searchView.jsp");
			js = new File(env + DATAFARI_PATH_PROD + "js/search.js");
			en = new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + EN_JSON);
			fr = new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + FR_JSON);
			it = new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + IT_JSON);
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Used to clean the semaphores (sem parameter needed)
	 * Used to get the existing facets
	 * Checks if the files exist
	 * read the js file, the the jsp. Find matches between them, put thhoses matches in a json Object
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			//If it's a request to clean the semaphore
			if(request.getParameter("sem")!=null){
				//We get the correct one and make sure it was not already released, before releasing it
				if( sem.availablePermits()<1)
					sem.release();
			}else{
				listDivName = new ArrayList<String>();
				listFieldsName = new ArrayList<String>();
				//If one of the files has not been found
				if( jsp == null || js == null || en == null || fr == null){
					//Check if it still doesn't exists
					if(System.getenv("DATAFARI_HOME")==null){
						if(!(new File(env+"/WebContent/searchView.jsp").exists() 
								|| new File(env+"/WebContent/js/search.js").exists() 
								|| new File(env + LOCALE_PATH_DEV + EN_JSON).exists() 
								|| new File(env + LOCALE_PATH_DEV + FR_JSON).exists()
								|| new File(env + LOCALE_PATH_DEV + IT_JSON).exists())){
							
							StringBuilder strBuilder = new StringBuilder();
							strBuilder.append("Error while opening searchView.jsp or search.js or")
										.append(EN_JSON)
										.append(" or ")
										.append(FR_JSON)
										.append(" or ")
										.append(IT_JSON)
										.append(", in FacetConfig doGet. Check these paths ")
										.append(env+"/WebContent/searchView.jsp")
										.append(", ")
										.append(env+"/WebContent/js/search.js")
										.append(", ")
										.append(env + LOCALE_PATH_DEV + EN_JSON)
										.append(", ")
										.append(env + LOCALE_PATH_DEV + FR_JSON)
										.append(", ")
										.append(env + LOCALE_PATH_DEV + IT_JSON)
										.append(", Error 69047");
							
							LOGGER.error(strBuilder.toString());		//If not an error is printed
							PrintWriter out = response.getWriter();
							out.append("Error while opening the configuration files, please retry, if the problem persists contact your system administrator. Error code : 69047"); 	
							out.close();
							return;
						}else{
							//If it exists now get it.			
							jsp = new File(env+"/WebContent/searchView.jsp");
							js = new File(env+"/WebContent/js/search.js");
							en = new File(env + LOCALE_PATH_DEV + EN_JSON);
							fr = new File(env + LOCALE_PATH_DEV + FR_JSON);
							it = new File(env + LOCALE_PATH_DEV + IT_JSON);
						}
					}else{
						if(!(new File(env + DATAFARI_PATH_PROD + "searchView.jsp").exists() 
								|| new File(env + DATAFARI_PATH_PROD + "js/search.js").exists() 
								|| new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + EN_JSON).exists() 
								|| new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + FR_JSON).exists()
								|| new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + IT_JSON).exists())){
							
							StringBuilder strBuilder = new StringBuilder();
							strBuilder.append("Error while opening searchView.jsp or search.js or")
										.append(EN_JSON)
										.append(" or ")
										.append(FR_JSON)
										.append(" or ")
										.append(IT_JSON)
										.append(", in FacetConfig doGet. Check these paths ")
										.append(env + DATAFARI_PATH_PROD + "searchView.jsp")
										.append(", ")
										.append(env + DATAFARI_PATH_PROD + "js/search.js")
										.append(", ")
										.append(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + EN_JSON)
										.append(", ")
										.append(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + FR_JSON)
										.append(", ")
										.append(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + IT_JSON)
										.append(", Error 69047");
							
							LOGGER.error(strBuilder.toString());		//If not an error is printed
							PrintWriter out = response.getWriter();
							out.append("Error while opening the configuration files, please retry, if the problem persists contact your system administrator. Error code : 69047"); 	
							out.close();
							return;
						}else{
							//If it exists now get it.
							jsp = new File(env + DATAFARI_PATH_PROD + "searchView.jsp");
							js = new File(env + DATAFARI_PATH_PROD + "js/search.js");
							en = new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + EN_JSON);
							fr = new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + FR_JSON);
							it = new File(env + DATAFARI_PATH_PROD + LOCALE_PATH_PROD + IT_JSON);
						}
					}
				}
				//Make sure the sem was not already acquired, before acquiring it
				if( sem.availablePermits()>0){
					try {
						sem.acquire();
					} catch (InterruptedException e) {
						LOGGER.error("Error while acquiring the Semaphore in FacetConfig doGet. Error 69048", e);		//If not an error is printed
						PrintWriter out = response.getWriter();
						out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69048"); 	
						out.close();
						return;
					}
					//Read the file
					String file = js.getAbsolutePath();
					Reader reader = new FileReader(file);
					try {
						superJson = new JSONObject();
						CompilerEnvirons env = new CompilerEnvirons();
						env.setRecordingLocalJsDocComments(true);
						env.setAllowSharpComments(true);
						env.setRecordingComments(true);
						//Get all the nodes 
						AstRoot node = new Parser(env).parse(reader, file, 1);
						node.visitAll(new SearchFacet());
					} finally {
						reader.close();
					}
					try {
						//If the facet created in the js file has a matching div in the jsp, we put it in the answer
						Document docJsp = Jsoup.parse(jsp, CharsetNames.CS_UTF8);
						Element elem = docJsp.getElementById("facets");
						//get all the childNodes of facets
						List<org.jsoup.nodes.Node> listDiv = elem.childNodes();
						for(org.jsoup.nodes.Node n : listDiv){
							for(int j = 0 ; j < listDivName.size() ; j ++){
								//If it's matching a divName found in the js
								if(n.attr("id").equals(listDivName.get(j))){
									json = new JSONObject();
									json.put("field", listFieldsName.get(j));
									json.put("div", listDivName.get(j));
									//put it in the json answer
									superJson.append("facet", json);
								}
							}
						}
						//add the length
						superJson.put("length", superJson.getJSONArray("facet").length());
					} catch (JSONException e) {
						if( sem.availablePermits()<1)
							sem.release();
						LOGGER.error("Error while building the json answer in the FacetConfig doGet. Check that the jsp and js files are valid. Error 69049", e);		//If not an error is printed
						PrintWriter out = response.getWriter();
						out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69049"); 	
						out.close();
						return;
					}
					//send the answer
					response.getWriter().write(superJson.toString());
					response.setStatus(200);
					response.setContentType("text/json;charset=UTF-8");
				}else{
					PrintWriter out = response.getWriter();
					out.append("File already in use");
					out.close();
				}
			}
		}catch(Exception e){
			if( sem.availablePermits()<1)
				sem.release();
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69524");
			out.close();
			LOGGER.error("Unindentified error in FacetConfig doGet. Error 69524", e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * Used to add, delete a facet
	 * Used to modify the printing order of the facets
	 * Reads and modify the jsp and js files, but also the i18n json files.
	 * Release the semaphore at the end
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			//If it's an add facet request
			if(request.getParameter("pagination") != null){
				//get the basics parameters
				String field = request.getParameter("field"), pagi = request.getParameter("pagination"), mult="";
				//Switch the multiselection parameter according to the checkbox
				switch(request.getParameter("selectType")){
				case "true" : 
					mult="OR";
					break;
				case "false" : 
					mult="ONE";
					break;
				default :
					mult="OR";
					break;
				}
				Source source = new Source(jsp);
				String newJsp = source.getSource().toString();
				//Modify the jsp, add after the "<div id="facets">" a new line with a name composed of "facet_" and then the name of the faceted field
				newJsp = newJsp.substring(0, newJsp.indexOf("<div id=\"facets\">")+17)+"\n\t\t\t\t<div id=\"facet_"+field+"\"></div>"+newJsp.substring(newJsp.indexOf("<div id=\"facets\">")+17);
				//Read the json files
				String jsonEnContent = readFile(en.getAbsolutePath(), StandardCharsets.UTF_8);
				String jsonFrContent = readFile(fr.getAbsolutePath(), StandardCharsets.UTF_8);
				String jsonItContent = readFile(it.getAbsolutePath(), StandardCharsets.UTF_8);
				JSONObject jsonEn;
				JSONObject jsonFr;
				JSONObject jsonIt;
				try {
					//If names have been typed by the user, we use the parameters otherwise the name of the facet will just be the name of the field
					//We put the content of the files into jsonObject to easily add the new entries
					
					jsonEn = new JSONObject(jsonEnContent);
					if(!Strings.isNullOrEmpty(request.getParameter("enName"))){
						jsonEn.put("facet"+field,request.getParameter("enName"));
					}
					else {
						jsonEn.put("facet"+field, field);
					
					}
					
					jsonFr = new JSONObject(jsonFrContent);
					if(!Strings.isNullOrEmpty(request.getParameter("frName"))){
						jsonFr.put("facet"+field,request.getParameter("frName"));
					}
					else{
						jsonFr.put("facet"+field, field);
					}
					
					jsonIt = new JSONObject(jsonItContent);
					if(!Strings.isNullOrEmpty(request.getParameter("itName"))){
						jsonIt.put("facet"+field,request.getParameter("itName"));
					}
					else{
						jsonIt.put("facet"+field, field);
					}
					
				} catch (JSONException e) {
					LOGGER.error("Error while adding the labels to the json files in the FacetConfig doPost. Check that the json files are valid, aso if the parameters passed are valid. Error 69050", e);		//If not an error is printed
					PrintWriter out = response.getWriter();
					out.append("Something bad happened, please make sure your parameters are valid and retry, if the problem persists contact your system administrator. Error code : 69050"); 	
					out.close();
					return;
				}
				String newWidget="";
				if(request.getParameter("query1")==null){
					//If it's a simple facet we form the expression statement like this
					newWidget = "\n\tManager.addWidget(new AjaxFranceLabs.TableWidget({\n"
							+"\t\telm : $('#facet_"+field+"'),\n"
							+"\t\tid : 'facet_"+field+"',\n"
							+"\t\tfield : '"+field+"',\n"
							+"\t\tname : window.i18n.msgStore['facet"+field+"'],\n"
							+"\t\tpagination : "+pagi+",\n"
							+"\t\tselectionType : '"+mult+"',\n"
							+"\t\treturnUnselectedFacetValues : true\n"
							+"\t}));\n";
				}else{
					//If it's a query facet
					List<String> listQueries = new ArrayList<String>();
					List<String> listLabelsFr = new ArrayList<String>();
					List<String> listLabelsEn = new ArrayList<String>();
					List<String> listLabelsIt = new ArrayList<String>();
					String reg1 = "^query[0-9]*\\b";
					//Get as much queries and as much labels as provided by the user
					for(Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();){
						String name = e.nextElement();
						if(name.matches(reg1) && !Strings.isNullOrEmpty(request.getParameter(name))){
							listQueries.add(request.getParameter(name));
							listLabelsFr.add(request.getParameter(name+"LabelFr"));
							listLabelsEn.add(request.getParameter(name+"LabelEn"));
							listLabelsIt.add(request.getParameter(name+"LabelIt"));
						}
					}
					if(listQueries.size()<1){
						PrintWriter out = response.getWriter();
						out.append("Not enough queries"); 	
						out.close();
						//We get the correct Semaphore and make sure it was not already released, before releasing it
						if( sem.availablePermits()<1)
							sem.release();
						return;
					}
					//Form the beginning of the expression statement
					newWidget = "\n\tManager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({\n"
							+"\t\telm : $('#facet_"+field+"'),\n"
							+"\t\tid : 'facet_"+field+"',\n"
							+"\t\tfield : '"+field+"',\n"
							+"\t\tname : window.i18n.msgStore['facet"+field+"'],\n"
							+"\t\tpagination : "+pagi+",\n"
							+"\t\tselectionType : '"+mult+"',\n"
							+"\t\tqueries : [";
					//Then add every query that there is to add
					for (int i = 0 ; i < listQueries.size() ; i++ ){
						newWidget +=  " \'["+listQueries.get(i)+"]\', ";
					}
					//Remove the last ", " and open the labels
					newWidget = newWidget.substring(0,newWidget.length()-2)+ "\n\t\t],\n\t\tlabels : [ ";
					try {
						//Put all the required labels in the json under the tag "facet_fieldNamei"  
						for (int i = 0 ; i < listQueries.size() ; i++ ){
							
							if(!Strings.isNullOrEmpty(request.getParameter("query"+(i+1)+"LabelEn"))){
								jsonEn.put("facet"+field+i,request.getParameter("query"+(i+1)+"LabelEn").replaceAll("\\s+", "%20"));
							}
							else {
								jsonEn.put("facet"+field+i, field);
							}
							
							if(!Strings.isNullOrEmpty(request.getParameter("query"+(i+1)+"LabelFr"))){
								jsonFr.put("facet"+field+i,request.getParameter("query"+(i+1)+"LabelFr").replaceAll("\\s+", "%20"));
							}
							else{
								jsonFr.put("facet"+field+i, field);
							}
							
							if(!Strings.isNullOrEmpty(request.getParameter("query"+(i+1)+"LabelIt"))){
								jsonIt.put("facet"+field+i,request.getParameter("query"+(i+1)+"LabelIt").replaceAll("\\s+", "%20"));
							}
							else{
								jsonIt.put("facet"+field+i, field);
							}
							
							//Add the reference to those labels in the expression statement
							newWidget +=  "window.i18n.msgStore[\'facet"+field+i+"\'], ";
						}
						//>Remove the last ", "
						newWidget = newWidget.substring(0,newWidget.length()-2)+"]\n\t}));";
					}catch(JSONException e){
						if( sem.availablePermits()<1)
							sem.release();
						LOGGER.error("Error while adding the labels to the json files in the FacetConfig doPost. Check that the json files are valid, aso if the parameters passed are valid. Error 69051", e);		//If not an error is printed
						PrintWriter out = response.getWriter();
						out.append("Something bad happened, please make sure your parameters are valid and retry, if the problem persists contact your system administrator. Error code : 69051"); 	
						out.close();
						return;
					}
				}
				FileOutputStream fooStream = new FileOutputStream(jsp, false); // true to append false to overwrite.
				fooStream.write(newJsp.getBytes());										//rewrite the file
				fooStream.close();
				
				//Rewrite the js file adding the new statement at the beginning.
				String jsContent = readFile(js.getAbsolutePath(), StandardCharsets.UTF_8);
				jsContent = jsContent.substring(0, jsContent.indexOf("$(function($) {")+16)+newWidget+jsContent.substring(jsContent.lastIndexOf("$(function($) {")+16, jsContent.length());
				fooStream = new FileOutputStream(js, false); // true to append false to overwrite.
				fooStream.write(jsContent.getBytes());										//rewrite the file
				fooStream.close();
				
				//rewrite both json files, replacing certain characters to keep the newlines so it can still be readable by a human.
				fooStream = new FileOutputStream(en, false); // true to append false to overwrite.
				fooStream.write(jsonEn.toString().replaceAll(",\"", ",\n\"").replaceAll("\\{", "\\{\n").replaceAll("\\}", "\n\\}").getBytes());										//rewrite the file
				fooStream.close();
				
				fooStream = new FileOutputStream(fr, false); // true to append false to overwrite.
				fooStream.write(jsonFr.toString().replaceAll(",\"", ",\n\"").replaceAll("\\{", "\\{\n").replaceAll("\\}", "\n\\}").getBytes());										//rewrite the file
				fooStream.close();
				
				fooStream = new FileOutputStream(it, false); // true to append false to overwrite.
				fooStream.write(jsonIt.toString().replaceAll(",\"", ",\n\"").replaceAll("\\{", "\\{\n").replaceAll("\\}", "\n\\}").getBytes());										//rewrite the file
				fooStream.close();
				
			}else if(request.getParameter("divName")!=null){
				//If it's a delete request
				//We get the parameter
				String field = request.getParameter("divName");
				Source source = new Source(jsp);
				String newJsp = source.getSource().toString();
				//We get the were is the matching div in the jsp file
				int pas = newJsp.indexOf("<div id=\""+field+"\"></div>");
				//We cut the content of the jsp to take everything that is located before or after
				newJsp = newJsp.substring(0, pas)+newJsp.substring(pas+("<div id=\""+field+"\"></div>").length());
				//Rewrite the jsp
				FileOutputStream fooStream = new FileOutputStream(jsp, false); // true to append false to overwrite.
				byte[] myBytes = newJsp.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();
				String jsContent = readFile(js.getAbsolutePath(), StandardCharsets.UTF_8);
				//Search for the matching expression statement in the js file, first we search for a query facet and if none is matching we search for simple facet
				int begin = jsContent.indexOf("Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({\n\t\telm : $('#"+field+"')");
				if(begin ==-1)
					begin = jsContent.indexOf("Manager.addWidget(new AjaxFranceLabs.TableWidget({\n\t\telm : $('#"+field+"')");
				int end = jsContent.substring(begin).indexOf('}')+4+begin;
				//Get the end of the statement and cut it from the rest of the file
				jsContent = jsContent.substring(0, begin)+jsContent.substring(end);
				//rewrite the file
				fooStream = new FileOutputStream(js, false); // true to append false to overwrite.
				myBytes = jsContent.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();	
			}else if(request.getParameter("0")!=null){
				//If it's a modify printing order request
				//Get all the divName passed as parameters
				String[] tab = new String[request.getParameterMap().size()];
				for(int i = 0; i< request.getParameterMap().size() ; i++){
					tab[i]=request.getParameter(""+i+"");
				}
				//Find the "facets" div in the jsp
				Source source = new Source(jsp);
				String sourceString = source.getSource().toString();
				int pas = sourceString.indexOf("<div id=\"facets\">")+("<div id=\"facets\">").length();
				//Find it's end
				int end = sourceString.substring(pas).indexOf("<div id=\"facet_signature\"></div>")+("<div id=\"facet_signature\"></div>").length();
				String newJsp = sourceString.substring(0,pas)+"\n";
				end += newJsp.length();
				//Remove everything that is after the declaration of this div
				for(int j = 0 ; j <tab.length ; j++){
					//Add the facets div in the requested order
					LOGGER.debug(tab[j]);
					newJsp += "\t\t\t\t<div id=\"facet_"+tab[j]+"\"></div>\n";
				}
				//Add the end of the file
				newJsp += "\t\t\t\t<div id=\"facet_signature\"></div>\n\t\t\t";
				newJsp += sourceString.substring(end);
				//Rewrite the file
				FileOutputStream fooStream = new FileOutputStream(jsp, false); // true to append false to overwrite.
				byte[] myBytes = newJsp.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();
			}
			//We get the correct Semaphore and make sure it was not already released, before releasing it
			if( sem.availablePermits()<1)
				sem.release();
		}catch(Exception e){
			if( sem.availablePermits()<1)
				sem.release();
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69525");
			out.close();
			LOGGER.error("Unindentified error in FacetConfig doPost. Error 69525", e);
		}
	}
	
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

}
