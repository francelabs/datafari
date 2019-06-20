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
package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XMLUtils {

	/**
	 * Convert XML Node to String
	 *
	 * @param node
	 *            the node to convert
	 * @return the String equivalent of the node
	 * @throws TransformerException
	 */
	public static String nodeToString(final Node node) throws TransformerException {
		final Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		final Writer out = new StringWriter();
		tf.transform(new DOMSource(node), new StreamResult(out));
		return out.toString();
	}

	/**
	 * Save an XML document into a file
	 *
	 * @param document
	 *            the XML document to save
	 * @param file
	 *            the file
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	public static void docToFile(final Document document, final File file)
			throws TransformerFactoryConfigurationError, TransformerException {
		final Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		tf.transform(new DOMSource(document), new StreamResult(file));
	}

	/**
	 * Get the searchHandler Node from the solrconfig.xml file
	 *
	 * @param solrconfig
	 *            the solrconfig.xml File
	 *
	 * @return searchHandler XML Node or null if not found
	 *
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public static Node getSearchHandlerNode(final File solrconfig)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document docSchem = dBuilder.parse(solrconfig);
		final XPathFactory xPathfactory = XPathFactory.newInstance();
		final XPath xpath = xPathfactory.newXPath();
		final XPathExpression expr = xpath
				.compile("//requestHandler[@class=\"solr.SearchHandler\" and @name=\"/select\"]");
		final Node requestHandler = (Node) expr.evaluate(docSchem, XPathConstants.NODE);
		return requestHandler;
	}

}
