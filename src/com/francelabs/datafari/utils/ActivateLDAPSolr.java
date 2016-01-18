package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
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

	private ActivateLDAPSolr() {
		final String filePath = System.getProperty("catalina.home") + File.separator + ".." + File.separator + "solr" + File.separator + "solr_home"
				+ File.separator + "FileShare" + File.separator + "conf" + File.separator + "solrconfig.xml";
		solrConfig = new File(filePath);
		// TODO : adjust code for dev mode too
		// If in development mode correct paths are :
		// String filePathJSON = System.getProperty("catalina.home") +
		// File.separator +".." + File.separator + "bin" + File.separator
		// +"config" + File.separator + "manifoldcf" + File.separator +
		// "monoinstance" + File.separator + "authorityconnections" +
		// File.separator + "authorityConnection.json";
		// String filePathGroupJSON = System.getProperty("catalina.home") +
		// File.separator +".." + File.separator + "bin" + File.separator
		// +"config" + File.separator + "manifoldcf" + File.separator +
		// "monoinstance" + File.separator + "authorityconnections" +
		// File.separator + "authorityGroups.json";
		final String filePathJSON = System.getProperty("catalina.home") + File.separator + ".." + File.separator + "bin" + File.separator + "config"
				+ File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator + "authorityconnections" + File.separator
				+ "authorityConnection.json";
		final String filePathGroupJSON = System.getProperty("catalina.home") + File.separator + ".." + File.separator + "bin" + File.separator
				+ "config" + File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator + "authorityconnections"
				+ File.separator + "authorityGroups.json";
		authorityconnectionJSON = new File(filePathJSON);
		authoritygroupJSON = new File(filePathGroupJSON);
	}

	private static ActivateLDAPSolr getInstance() {
		if (instance == null) {
			return instance = new ActivateLDAPSolr();
		}
		return instance;
	}

	private static void XMLTOFile(final File file, final Document document) throws TransformerException {
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		transformer = transformerFactory.newTransformer();
		final DOMSource source = new DOMSource(document);
		final StreamResult result = new StreamResult(file);
		transformer.transform(source, result);

	}

	public static int activate() throws Exception {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document docSchem = dBuilder.parse(getInstance().solrConfig);
		final Element root = docSchem.getDocumentElement();
		final NodeList fields = root.getElementsByTagName("requestHandler");
		for (int i = 0; i < fields.getLength(); i++) {
			final Node item = fields.item(i);
			if (item.getAttributes().getNamedItem("class") != null
					&& !item.getAttributes().getNamedItem("class").getNodeValue().equals("solr.SearchHandler"))
				continue;
			boolean isActivated = false;
			final NodeList lstList = ((Element) item).getElementsByTagName("lst");
			for (int j = 0; j < lstList.getLength(); j++) {
				if (!lstList.item(j).getAttributes().getNamedItem("name").getNodeValue().equals("appends"))
					continue;
				isActivated = true;
			}
			if (!isActivated) {
				final Element elementRoot = docSchem.createElement("lst");
				final Element elementChild = docSchem.createElement("str");
				elementRoot.setAttribute("name", "appends");
				elementChild.setAttribute("name", "fq");
				elementChild.appendChild(docSchem.createTextNode("{!manifoldCFSecurity}"));
				elementRoot.appendChild(elementChild);
				item.appendChild(elementRoot);
				XMLTOFile(solrConfig, docSchem);
				final JSONObject json = JSONUtils.readJSON(authorityconnectionJSON);
				final JSONObject jsonGroup = JSONUtils.readJSON(authoritygroupJSON);
				try {
					ManifoldAPI.deleteConfig("authorityconnections", "DatafariAD");
					ManifoldAPI.deleteConfig("authoritygroups", "DatafariAuthorityGroup");
				} catch (final Exception e) {
					logger.error("FATAL ERROR", e);
				}

				try {
					ManifoldAPI.putConfig("authoritygroups", "DatafariAuthorityGroup", jsonGroup);

				} catch (final Exception e) {

				}
				try {
					ManifoldAPI.putConfig("authorityconnections", "DatafariAD", json);
				} catch (final Exception e) {

				}
			}
		}
		return CodesReturned.ALLOK;
	}

	public static int disactivate() throws SAXException, IOException, ParserConfigurationException, TransformerException {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document docSchem = dBuilder.parse(getInstance().solrConfig);
		final Element root = docSchem.getDocumentElement();
		final NodeList fields = root.getElementsByTagName("requestHandler");
		for (int i = 0; i < fields.getLength(); i++) {
			final Node item = fields.item(i);
			if (item.getAttributes().getNamedItem("class") != null
					&& !item.getAttributes().getNamedItem("class").getNodeValue().equals("solr.SearchHandler"))
				continue;
			final boolean isActivated = false;
			final NodeList lstList = ((Element) item).getElementsByTagName("lst");
			for (int j = 0; j < lstList.getLength(); j++) {
				if (!lstList.item(j).getAttributes().getNamedItem("name").getNodeValue().equals("appends"))
					continue;
				item.removeChild(lstList.item(j));
				XMLTOFile(solrConfig, docSchem);
				try {
					ManifoldAPI.deleteConfig("authorityconnections", "DatafariAD");
					ManifoldAPI.deleteConfig("authoritygroups", "DatafariAuthorityGroup");
				} catch (final Exception e) {
					logger.error("FATAL ERROR", e);
					return CodesReturned.GENERALERROR;
				}
			}
		}
		return CodesReturned.ALLOK;
	}
}
