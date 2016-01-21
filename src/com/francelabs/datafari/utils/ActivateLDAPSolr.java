package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class ActivateLDAPSolr {
	private static File solrConfig;
	private static File authorityconnectionJSON;
	private static File authoritygroupJSON;
	private static ActivateLDAPSolr instance;
	private final static Logger logger = Logger.getLogger(ActivateLDAPSolr.class); 
	
	private ActivateLDAPSolr(){
		String filePath = System.getProperty("catalina.home")+ File.separator +".." + File.separator +"solr"+File.separator+"solr_home"+File.separator+"FileShare"+File.separator+"conf"+File.separator+"solrconfig.xml";
		solrConfig = new File(filePath);
		//TODO : adjust code for dev mode too
		//If in development mode correct paths are  : 
		// String filePathJSON = System.getProperty("catalina.home") + File.separator +".." + File.separator + "bin"  + File.separator +"config" + File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator + "authorityconnections" + File.separator + "authorityConnection.json";
		//String filePathGroupJSON = System.getProperty("catalina.home") + File.separator +".." + File.separator + "bin"  + File.separator +"config" + File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator + "authorityconnections" + File.separator + "authorityGroups.json";
		String filePathJSON = System.getProperty("catalina.home") + File.separator +".." + File.separator + "bin" + File.separator + "common" + File.separator + "config" 
				+ File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator + "authorityconnections" + File.separator + "authorityConnection.json";
		String filePathGroupJSON = System.getProperty("catalina.home") + File.separator +".." + File.separator + "bin" + File.separator + "common" + File.separator + "config" 
				+ File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator + "authorityconnections" + File.separator + "authorityGroups.json";
		authorityconnectionJSON = new File(filePathJSON);
		authoritygroupJSON = new File(filePathGroupJSON);
	}
	private static ActivateLDAPSolr getInstance(){
		if (instance == null){
			return instance = new ActivateLDAPSolr();
		}
		return instance;
	}
	
	private static void XMLTOFile(File file , Document document) throws TransformerException{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
		
	}
	
	public static int activate() throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document docSchem = dBuilder.parse(getInstance().solrConfig);					
		Element root = docSchem.getDocumentElement();
		NodeList fields = root.getElementsByTagName("requestHandler");
		for (int i=0;i<fields.getLength();i++){
			Node item = fields.item(i);
			if ( item.getAttributes().getNamedItem("class")!=null && !item.getAttributes().getNamedItem("class").getNodeValue().equals("solr.SearchHandler") )
				continue;
			boolean isActivated = false;
			NodeList lstList = ((Element)item).getElementsByTagName("lst");
			for (int j=0 ; j<lstList.getLength() ; j++){
				if (!lstList.item(j).getAttributes().getNamedItem("name").getNodeValue().equals("appends"))
					continue;
				isActivated = true;
			}
			if (!isActivated){
				Element elementRoot = docSchem.createElement("lst");
				Element elementChild = docSchem.createElement("str");
				elementRoot.setAttribute("name","appends");
				elementChild.setAttribute("name","fq");
				elementChild.appendChild(docSchem.createTextNode("{!manifoldCFSecurity}"));
				elementRoot.appendChild(elementChild);
				item.appendChild(elementRoot);
				XMLTOFile(solrConfig,docSchem);
				JSONObject json = JSONUtils.readJSON(authorityconnectionJSON);
				JSONObject jsonGroup = JSONUtils.readJSON(authoritygroupJSON);
				try {
					ManifoldAPI.deleteConfig("authorityconnections", "DatafariAD");
					ManifoldAPI.deleteConfig("authoritygroups", "DatafariAuthorityGroup");
				} catch (Exception e) {
					logger.error("FATAL ERROR",e);
				}
				
				try {
					ManifoldAPI.putConfig("authoritygroups", "DatafariAuthorityGroup",jsonGroup);
							
				} catch (Exception e) {
					
				}
				try {
					ManifoldAPI.putConfig("authorityconnections", "DatafariAD",json);					
				} catch (Exception e) {
					
				}
			}
		}
		return CodesReturned.ALLOK;
	}
	
	public static int disactivate() throws SAXException, IOException, ParserConfigurationException, TransformerException{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document docSchem = dBuilder.parse(getInstance().solrConfig);					
		Element root = docSchem.getDocumentElement();
		NodeList fields = root.getElementsByTagName("requestHandler");
		for (int i=0;i<fields.getLength();i++){
			Node item = fields.item(i);
			if ( item.getAttributes().getNamedItem("class")!= null && !item.getAttributes().getNamedItem("class").getNodeValue().equals("solr.SearchHandler"))
				continue;
			boolean isActivated = false;
			NodeList lstList = ((Element)item).getElementsByTagName("lst");
			for (int j=0 ; j<lstList.getLength() ; j++){
				if (!lstList.item(j).getAttributes().getNamedItem("name").getNodeValue().equals("appends"))
					continue;
				item.removeChild(lstList.item(j));
				XMLTOFile(solrConfig,docSchem);		
				try {
					ManifoldAPI.deleteConfig("authorityconnections", "DatafariAD");
					ManifoldAPI.deleteConfig("authoritygroups", "DatafariAuthorityGroup");
				} catch (Exception e) {
					logger.error("FATAL ERROR",e);
					return CodesReturned.GENERALERROR;
				}
			}
		}
		return CodesReturned.ALLOK;
	}
}
