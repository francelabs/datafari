package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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

public class RealmLdapConfiguration {
	private String configFilePath;
	public static final String fileName = "context.xml" ;
	 private static final Logger logger = Logger.getLogger(ModifyRealmLdap.class.getName());  
	private static File fileContext;
	private static RealmLdapConfiguration instance; 
	public final static String attributeConnectionURL = "connectionURL";
	public final static String attributeConnectionName = "connectionName";
	public final static String attributeConnectionPassword = "connectionPassword";
	public final static String attributeDomainName = "userBase";
	
	
	private RealmLdapConfiguration(){
		String env = System.getProperty("catalina.home");	
		configFilePath = env+ File.separator + ".." + File.separator + "WebContent" + File.separator + "META-INF" + File.separator + fileName;
		fileContext = new File(configFilePath); 
	}
	
	private static RealmLdapConfiguration getInstance(){
		if (instance == null){
			instance = new RealmLdapConfiguration(); 
		}
		return instance;
	}
	
	public static HashMap<String,String> getConfig() throws SAXException, IOException, ParserConfigurationException{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document docSchem = dBuilder.parse(getInstance().fileContext);					//Parse the schema
		NodeList fields = docSchem.getElementsByTagName("Realm");
		HashMap <String,String> hashMap = new HashMap <String,String>();
		for (int i=0;i<fields.getLength();i++){
			NamedNodeMap attributes = fields.item(i).getAttributes();
			if (!attributes.getNamedItem("className").getNodeValue().equals("org.apache.catalina.realm.JNDIRealm"))
				continue;
			hashMap.put(attributeConnectionURL , attributes.getNamedItem(attributeConnectionURL).getNodeValue());
			hashMap.put(attributeConnectionName , attributes.getNamedItem(attributeConnectionName).getNodeValue());
			hashMap.put(attributeConnectionPassword , attributes.getNamedItem(attributeConnectionPassword).getNodeValue());
			hashMap.put(attributeDomainName, attributes.getNamedItem(attributeDomainName).getNodeValue());
		}
		return hashMap;
	}
	
	public static int setConfig(HashMap<String,String> h) throws SAXException, IOException, ParserConfigurationException{
		if (h.containsKey(attributeConnectionURL) && h.containsKey(attributeConnectionName) && h.containsKey(attributeConnectionPassword) && h.containsKey(attributeDomainName)){
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document docSchem = dBuilder.parse(getInstance().fileContext);					//Parse the schema
		Element root = docSchem.getDocumentElement();
		NodeList fields = root.getElementsByTagName("Realm");
		for (int i=0;i<fields.getLength();i++){
			Node item = fields.item(i);
			if (!item.getAttributes().getNamedItem("className").getNodeValue().equals("org.apache.catalina.realm.JNDIRealm"))
				continue;
			if (item.getNodeType() == Node.ELEMENT_NODE ){
				Element realmConfiguration = (Element) item;
				realmConfiguration.setAttribute(attributeConnectionURL, h.get(attributeConnectionURL));
				realmConfiguration.setAttribute(attributeConnectionName, h.get(attributeConnectionName));
				realmConfiguration.setAttribute(attributeConnectionPassword, h.get(attributeConnectionPassword));
				realmConfiguration.setAttribute(attributeDomainName, h.get(attributeDomainName));
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
