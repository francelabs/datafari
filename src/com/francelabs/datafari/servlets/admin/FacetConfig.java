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
import java.util.Iterator;
import java.util.List;

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
/**
 * Servlet implementation class FacetConfig
 */
@WebServlet("/admin/FacetConfig")
public class FacetConfig extends HttpServlet {
	class SearchFacet implements NodeVisitor {
		@Override public boolean visit(AstNode node) {
			if(node.getClass().toString().equals("class org.mozilla.javascript.ast.ExpressionStatement")){
				ExpressionStatement exprnode = (ExpressionStatement) node;
				String expression = exprnode.toSource();
				if(expression.startsWith("Manager.addWidget(new AjaxFranceLabs.TableWidget({") || expression.startsWith("Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({")){
					listDivName.add(expression.substring(expression.indexOf('#')+1, expression.indexOf(',')-2));
					expression = expression.substring(expression.indexOf("field"));
					listFieldsName.add(expression.substring(expression.indexOf("'")+1, expression.indexOf(",")-1));
				}
			}
			return true;
		}
	}
	private static final long serialVersionUID = 1L;
	private static final List<SemaphoreLn> listMutex = new ArrayList<SemaphoreLn>();
	private String env;
	private JSONObject json = new JSONObject();
	private JSONObject superJson = new JSONObject();
	private List<String> listDivName;
	private List<String> listFieldsName;
	private File jsp = null;
	private File js = null;
	private File en = null;
	private File fr = null;
	private final static Logger LOGGER = Logger.getLogger(ModifyNodeContent.class
			.getName());
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public FacetConfig() {
		env = System.getenv("DATAFARI_HOME");									//Gets the directory of installation if in standard environment
		if(env==null){															//If in development environment	
			RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();	//Gets the D.solr.solr.home variable given in arguments to the VM
			List<String> arguments = runtimeMxBean.getInputArguments();
			for(String s : arguments){
				if(s.startsWith("-Dsolr.solr.home"))
					env = s.substring(s.indexOf("=")+1, s.indexOf("solr_home")-5);
			}
		}
		listMutex.add(new SemaphoreLn("", "facetConfig"));
		if(new File(env+"/WebContent/searchView.jsp").exists())	//Check if the files exists
			jsp = new File(env+"/WebContent/searchView.jsp");
		if(new File(env+"/WebContent/js/search.js").exists())
			js = new File(env+"/WebContent/js/search.js");
		if(new File(env+"/WebContent/js/AjaxFranceLabs/locale/en.json").exists())	//Check if the files exists
			en = new File(env+"/WebContent/js/AjaxFranceLabs/locale/en.json");
		if(new File(env+"/WebContent/js/AjaxFranceLabs/locale/fr.json").exists())
			fr = new File(env+"/WebContent/js/AjaxFranceLabs/locale/fr.json");
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			if(request.getParameter("sem")!=null){
				for(int i = 0 ; i < listMutex.size() ; i++){
					if(listMutex.get(i).getType().equals("facetConfig")){
						if( listMutex.get(i).availablePermits()<1){
							listMutex.get(i).release();
						}
					}
				}
			}else{
				listDivName = new ArrayList<String>();
				listFieldsName = new ArrayList<String>();
				if( jsp == null || js == null || en == null || fr == null){
					if(!(new File(env+"/WebContent/searchView.jsp").exists() || new File(env+"/WebContent/js/search.js").exists() || new File(env+"/WebContent/js/AjaxFranceLabs/locale/en.json").exists() || new File(env+"/WebContent/js/AjaxFranceLabs/locale/fr.json").exists())){
						LOGGER.error("Error while opening searchView.jsp or search.js or en.json or fr.json, in FacetConfig doGet. Check those paths "+jsp.getAbsolutePath()+", "+js.getAbsolutePath()+", "+en.getAbsolutePath()+", "+fr.getAbsolutePath()+", Error 69047");		//If not an error is printed
						PrintWriter out = response.getWriter();
						out.append("Error while opening the configuration files, please retry, if the problem persists contact your system administrator. Error Code : 69047"); 	
						out.close();
						return;
					}else{
						jsp = new File(env+"/WebContent/searchView.jsp");
						js = new File(env+"/WebContent/js/search.js");
						en = new File(env+"/WebContent/js/AjaxFranceLabs/locale/en.json");
						fr = new File(env+"/WebContent/js/AjaxFranceLabs/locale/fr.json");
					}
				}
				for(int i = 0 ; i < listMutex.size() ; i++){
					if(listMutex.get(i).getType().equals("facetConfig")){
						if( listMutex.get(i).availablePermits()>0){
							try {
								listMutex.get(i).acquire();
							} catch (InterruptedException e) {
								LOGGER.error("Error while acquiring the Semaphore in FacetConfig doGet. Error 69048");		//If not an error is printed
								PrintWriter out = response.getWriter();
								out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69048"); 	
								out.close();
								return;
							}
							String file = env+"/WebContent/js/search.js";
							Reader reader = new FileReader(file);
							try {
								superJson = new JSONObject();
								CompilerEnvirons env = new CompilerEnvirons();
								env.setRecordingLocalJsDocComments(true);
								env.setAllowSharpComments(true);
								env.setRecordingComments(true);
								AstRoot node = new Parser(env).parse(reader, file, 1);
								node.visitAll(new SearchFacet());
							} finally {
								reader.close();
							}
							try {
								Document docJsp = Jsoup.parse(jsp, CharsetNames.CS_UTF8);
								Element elem = docJsp.getElementById("facets");
								List<org.jsoup.nodes.Node> listDiv = elem.childNodes();
								for(org.jsoup.nodes.Node n : listDiv){
									for(int j = 0 ; j < listDivName.size() ; j ++){
										if(n.attr("id").equals(listDivName.get(j))){
											json = new JSONObject();
											json.put("field", listFieldsName.get(j));
											json.put("div", listDivName.get(j));
											superJson.append("facet", json);
										}
									}
								}
								superJson.put("length", superJson.getJSONArray("facet").length());
							} catch (JSONException e) {
								LOGGER.error("Error while building the json answer in the FacetConfig doGet. Check that the jsp and js files are valid. Error 69049");		//If not an error is printed
								PrintWriter out = response.getWriter();
								out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69049"); 	
								out.close();
								return;
							}
							response.getWriter().write(superJson.toString());
							response.setStatus(200);
							response.setContentType("text/json;charset=UTF-8");
						}else{
							PrintWriter out = response.getWriter();
							out.append("File already in use");
							out.close();
						}
					}
				}
			}
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69524");
			out.close();
			LOGGER.error("Unindentified error in FacetConfig doGet. Error 69524", e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			if(request.getParameter("pagination") != null){
				String field = request.getParameter("field"), pagi = request.getParameter("pagination"), mult="";
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
				newJsp = newJsp.substring(0, newJsp.indexOf("<div id=\"facets\">")+17)+"\n\t\t\t\t<div id=\"facet_"+field+"\"></div>"+newJsp.substring(newJsp.indexOf("<div id=\"facets\">")+17);
				FileOutputStream fooStream = new FileOutputStream(jsp, false); // true to append false to overwrite.
				byte[] myBytes = newJsp.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();
				String jsonEnContent = readFile(en.getAbsolutePath(), StandardCharsets.UTF_8);
				String jsonFrContent = readFile(fr.getAbsolutePath(), StandardCharsets.UTF_8);
				try {
					JSONObject jsonEn = new JSONObject(jsonEnContent);
					if(request.getParameter("enName")!="")
						jsonEn.put("facet"+field,request.getParameter("enName"));
					else
						jsonEn.put("facet"+field, field);
					JSONObject jsonFr = new JSONObject(jsonFrContent);
					if(request.getParameter("frName")!="")
						jsonFr.put("facet"+field,request.getParameter("frName"));
					else
						jsonFr.put("facet"+field, field);
					fooStream = new FileOutputStream(en, false); // true to append false to overwrite.
					myBytes = jsonEn.toString().replaceAll(",\"", ",\n\"").replaceAll("\\{", "\\{\n").replaceAll("\\}", "\n\\}").getBytes();
					fooStream.write(myBytes);										//rewrite the file
					fooStream.close();
					fooStream = new FileOutputStream(fr, false); // true to append false to overwrite.
					myBytes = jsonFr.toString().replaceAll(",\"", ",\n\"").replaceAll("\\{", "\\{\n").replaceAll("\\}", "\n\\}").getBytes();
					fooStream.write(myBytes);										//rewrite the file
					fooStream.close();
				} catch (JSONException e) {
					LOGGER.error("Error while building the json answer in the FacetConfig doGet. Check that the json files are valid, aso if the parameters passed are valid. Error 69050");		//If not an error is printed
					PrintWriter out = response.getWriter();
					out.append("Something bad happened, please make sure your parameters are valid and retry, if the problem persists contact your system administrator. Error code : 69050"); 	
					out.close();
					return;
				}
				String newWidget="";
				if(request.getParameter("query1")==null){
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
					List<String> listQueries = new ArrayList<String>();
					List<String> listLabelsFr = new ArrayList<String>();
					List<String> listLabelsEn = new ArrayList<String>();
					String reg1 = "^query[0-9]*\\b";
					for(Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();){
						String name = e.nextElement();
						if(name.matches(reg1)){
							listQueries.add(request.getParameter(name));
							listLabelsFr.add(request.getParameter(name+"LabelFr"));
							listLabelsEn.add(request.getParameter(name+"LabelEn"));
						}
					}
					newWidget = "\n\tManager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({\n"
							+"\t\telm : $('#facet_"+field+"'),\n"
							+"\t\tid : 'facet_"+field+"',\n"
							+"\t\tfield : '"+field+"',\n"
							+"\t\tname : window.i18n.msgStore['facet"+field+"'],\n"
							+"\t\tpagination : "+pagi+",\n"
							+"\t\tselectionType : '"+mult+"',\n"
							+"\t\tqueries : [";
					for (int i = 0 ; i < listQueries.size() ; i++ ){
						newWidget +=  " \'["+listQueries.get(i)+"]\', ";
					}
					newWidget = newWidget.substring(0,newWidget.length()-2)+ "\n\t\t],\n\t\tlabels : [ ";
					try {
						jsonEnContent = readFile(en.getAbsolutePath(), StandardCharsets.UTF_8);
						jsonFrContent = readFile(fr.getAbsolutePath(), StandardCharsets.UTF_8);
						JSONObject jsonEn = new JSONObject(jsonEnContent);
						JSONObject jsonFr = new JSONObject(jsonFrContent);
						for (int i = 0 ; i < listQueries.size() ; i++ ){
							if(request.getParameter("query"+(i+1)+"LabelEn")!="")
								jsonEn.put("facet"+field+i,request.getParameter("query"+(i+1)+"LabelEn").replaceAll("\\s+", "%20"));
							else
								jsonEn.put("facet"+field+i, field);
							if(request.getParameter("query"+(i+1)+"LabelFr")!="")
								jsonFr.put("facet"+field+i,request.getParameter("query"+(i+1)+"LabelFr").replaceAll("\\s+", "%20"));
							else
								jsonFr.put("facet"+field+i, field);
							newWidget +=  "window.i18n.msgStore[\'facet"+field+i+"\'], ";
						}
						newWidget = newWidget.substring(0,newWidget.length()-2)+"]\n\t}));";
						fooStream = new FileOutputStream(en, false); // true to append false to overwrite.
						myBytes = jsonEn.toString().replaceAll(",\"", ",\n\"").replaceAll("\\{", "\\{\n").replaceAll("\\}", "\n\\}").getBytes();
						fooStream.write(myBytes);										//rewrite the file
						fooStream.close();
						fooStream = new FileOutputStream(fr, false); // true to append false to overwrite.
						myBytes = jsonFr.toString().replaceAll(",\"", ",\n\"").replaceAll("\\{", "\\{\n").replaceAll("\\}", "\n\\}").getBytes();
						fooStream.write(myBytes);										//rewrite the file
						fooStream.close();
					}catch(JSONException e){
						LOGGER.error("Error while building the json answer in the FacetConfig doGet. Check that the json files are valid, aso if the parameters passed are valid. Error 69051");		//If not an error is printed
						PrintWriter out = response.getWriter();
						out.append("Something bad happened, please make sure your parameters are valid and retry, if the problem persists contact your system administrator. Error code : 69051"); 	
						out.close();
						return;
					}
				}
				String jsContent = readFile(js.getAbsolutePath(), StandardCharsets.UTF_8);
				jsContent = jsContent.substring(0, jsContent.indexOf("$(function($) {")+16)+newWidget+jsContent.substring(jsContent.lastIndexOf("$(function($) {")+16, jsContent.length());
				fooStream = new FileOutputStream(js, false); // true to append false to overwrite.
				myBytes = jsContent.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();
			}else if(request.getParameter("divName")!=null){
				String field = request.getParameter("divName");
				Source source = new Source(jsp);
				String newJsp = source.getSource().toString();
				int pas = newJsp.indexOf("<div id=\""+field+"\"></div>");
				newJsp = newJsp.substring(0, pas)+newJsp.substring(pas+("<div id=\""+field+"\"></div>").length());
				FileOutputStream fooStream = new FileOutputStream(jsp, false); // true to append false to overwrite.
				byte[] myBytes = newJsp.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();
				String jsContent = readFile(js.getAbsolutePath(), StandardCharsets.UTF_8);
				int begin = jsContent.indexOf("Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({\n\t\telm : $('#"+field+"')");
				if(begin ==-1)
					begin = jsContent.indexOf("Manager.addWidget(new AjaxFranceLabs.TableWidget({\n\t\telm : $('#"+field+"')");
				int end = jsContent.substring(begin).indexOf('}')+4+begin;
				jsContent = jsContent.substring(0, begin)+jsContent.substring(end);
				fooStream = new FileOutputStream(js, false); // true to append false to overwrite.
				myBytes = jsContent.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();	
			}else{
				int i = 0;
				String[] tab = new String[request.getParameterMap().size()];
				for(Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();){
					String next = e.nextElement();
					if(next.matches("^[0-9]\\b"))
						tab[i]=request.getParameter(next);
					i++;
				}
				Source source = new Source(jsp);
				String sourceString = source.getSource().toString();
				int pas = sourceString.indexOf("<div id=\"facets\">")+("<div id=\"facets\">").length();
				int end = sourceString.substring(pas).indexOf("\t</div>\n");
				String newJsp = sourceString.substring(0,pas)+"\n";
				end += newJsp.length();
				for(int j = tab.length-1 ; j >-1 ; j--){
					newJsp += "\t\t\t\t<div id=\"facet_"+tab[j]+"\"></div>\n";
				}
				newJsp += "\t\t\t\t<div id=\"facet_signature\"></div>\n\t\t\t";
				newJsp += sourceString.substring(end);
				FileOutputStream fooStream = new FileOutputStream(jsp, false); // true to append false to overwrite.
				byte[] myBytes = newJsp.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();
			}
			for(int i = 0 ; i < listMutex.size() ; i++){
				if(listMutex.get(i).getType().equals("facetConfig")){
					if( listMutex.get(i).availablePermits()<1)
						listMutex.get(i).release();
				}
			}
		}catch(Exception e){
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
