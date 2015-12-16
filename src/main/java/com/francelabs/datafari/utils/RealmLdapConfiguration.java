package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.servlets.admin.ModifyRealmLdap;
import com.google.common.base.Strings;

public class RealmLdapConfiguration {	
	 
	private File fileContext;
	
	private static RealmLdapConfiguration instance;
	
	private static final Logger logger = Logger.getLogger(ModifyRealmLdap.class); 
	
	public static final String ATTR_CONNECTION_URL = "connectionURL";
	public static final String ATTR_CONNECTION_NAME = "connectionName";
	public static final String ATTR_CONNECTION_PW = "connectionPassword";
	public static final String ATTR_DOMAIN_NAME = "userBase";
	private static final String FILE_NAME = "context.xml" ;
	
	
	private RealmLdapConfiguration(String contextPath){
		
		String webAppName;
		
		if (!Strings.isNullOrEmpty(contextPath)){
			if (contextPath.startsWith(File.separator)){
				// Remove the path separator (\ or //)
				webAppName = contextPath.substring(1);
			} else {
				webAppName = contextPath;
			}
		} else {
			// Set the default value
			webAppName = "Datafari";
		}	
		
		String env = System.getenv("DATAFARI_HOME");		//Gets the directory of installation if in standard environment
		if(env==null){															//If in development environment	(Eclipse WTP)
			env = ExecutionEnvironment.getDevExecutionEnvironment();
			// Should return something like PATH_TO_WORKSPACE/datafari/ 
			// Reference WebContent folder as already done for FacetConfig class

			fileContext = new File(env + "WebContent" + File.separator + "META-INF" + File.separator + FILE_NAME);
		}else{
			fileContext = new File(env + File.separator + "tomcat" + File.separator + "webapps" + File.separator + webAppName  + File.separator + "META-INF" + File.separator + FILE_NAME);
		}
		 
	}
	
	private static RealmLdapConfiguration getInstance(String contextPath){
		if (instance == null){
			instance = new RealmLdapConfiguration(contextPath); 
		}					
		return instance;
	}
	
	public static HashMap<String,String> getConfig(HttpServletRequest request) throws SAXException, IOException, ParserConfigurationException{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document docSchem = dBuilder.parse(getInstance(request.getContextPath()).fileContext);					//Parse the schema
		NodeList fields = docSchem.getElementsByTagName("Realm");
		HashMap <String,String> hashMap = new HashMap <String,String>();
		for (int i=0;i<fields.getLength();i++){
			NamedNodeMap attributes = fields.item(i).getAttributes();
			if (!attributes.getNamedItem("className").getNodeValue().equals("org.apache.catalina.realm.JNDIRealm"))
				continue;
			hashMap.put(ATTR_CONNECTION_URL , attributes.getNamedItem(ATTR_CONNECTION_URL).getNodeValue());
			hashMap.put(ATTR_CONNECTION_NAME , attributes.getNamedItem(ATTR_CONNECTION_NAME).getNodeValue());
			hashMap.put(ATTR_CONNECTION_PW , attributes.getNamedItem(ATTR_CONNECTION_PW).getNodeValue());
			hashMap.put(ATTR_DOMAIN_NAME, attributes.getNamedItem(ATTR_DOMAIN_NAME).getNodeValue());
		}
		return hashMap;
	}
	
	public static int setConfig(HashMap<String,String> h, HttpServletRequest request) throws SAXException, IOException, ParserConfigurationException{
		if (h.containsKey(ATTR_CONNECTION_URL) && h.containsKey(ATTR_CONNECTION_NAME) && h.containsKey(ATTR_CONNECTION_PW) && h.containsKey(ATTR_DOMAIN_NAME)){
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		File fileContext = getInstance(request.getContextPath()).fileContext;
		Document docSchem = dBuilder.parse(fileContext);					//Parse the schema
		Element root = docSchem.getDocumentElement();
		NodeList fields = root.getElementsByTagName("Realm");
		for (int i=0;i<fields.getLength();i++){
			Node item = fields.item(i);
			if (!item.getAttributes().getNamedItem("className").getNodeValue().equals("org.apache.catalina.realm.JNDIRealm"))
				continue;
			if (item.getNodeType() == Node.ELEMENT_NODE ){
				Element realmConfiguration = (Element) item;
				realmConfiguration.setAttribute(ATTR_CONNECTION_URL, h.get(ATTR_CONNECTION_URL));
				realmConfiguration.setAttribute(ATTR_CONNECTION_NAME, h.get(ATTR_CONNECTION_NAME));
				realmConfiguration.setAttribute(ATTR_CONNECTION_PW, h.get(ATTR_CONNECTION_PW));
				realmConfiguration.setAttribute(ATTR_DOMAIN_NAME, h.get(ATTR_DOMAIN_NAME));
			}
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;
			try {
				transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(docSchem);
				StreamResult result = new StreamResult(fileContext);
				transformer.transform(source, result);
			} catch (TransformerException e) {
				logger.error(e);
				return CodesReturned.GENERALERROR;
			}
			
		}
			return CodesReturned.ALLOK;
		}else{
			return CodesReturned.PARAMETERNOTWELLSET;
		}
		
	}
}
