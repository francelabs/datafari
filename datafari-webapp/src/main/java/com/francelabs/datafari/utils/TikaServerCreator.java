package com.francelabs.datafari.utils;

import java.io.File;
import java.io.FileWriter;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TikaServerCreator {

  private final File tikaConfOCRFile;
  private final File tikaConfSimpleFile;
  private final File tikalog4j2File;
  private final File tikalog4j2ChildFile;
  private final File originalTikaBinFolderFile;
  private final File tikaServerJarFile;
  private final XPath xPath = XPathFactory.newInstance().newXPath();
  final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  final DocumentBuilder db;
  final TransformerFactory tf = TransformerFactory.newInstance();
  final Transformer transformer;
  private static TikaServerCreator instance = null;
  private final static Logger logger = LogManager.getLogger(TikaServerCreator.class);

  private TikaServerCreator() throws ParserConfigurationException, TransformerConfigurationException {
    String datafariHomePath = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHomePath == null) {
      // if no variable is set, use the default installation path
      datafariHomePath = "/opt/datafari";
    }
    final String tikaConfOCRFilePath = datafariHomePath + File.separator + "tika-server" + File.separator + "original-files" + File.separator + "conf" + File.separator + "tika-config-ocr.xml";
    final String tikaConfSimpleFilePath = datafariHomePath + File.separator + "tika-server" + File.separator + "original-files" + File.separator + "conf" + File.separator + "tika-config-simple.xml";
    final String tikalog4j2FilePath = datafariHomePath + File.separator + "tika-server" + File.separator + "original-files" + File.separator + "conf" + File.separator + "log4j2.properties.xml";
    final String tikalog4j2ChildFilePath = datafariHomePath + File.separator + "tika-server" + File.separator + "original-files" + File.separator + "conf" + File.separator
        + "log4j2child.properties.xml";
    final String originalTikaBinFolderFilePath = datafariHomePath + File.separator + "tika-server" + File.separator + "original-files" + File.separator + "bin";
    final String tikaServerJarFilePath = datafariHomePath + File.separator + "tika-server" + File.separator + "bin" + File.separator + "tika-server.jar";
    tikaConfOCRFile = new File(tikaConfOCRFilePath);
    tikaConfSimpleFile = new File(tikaConfSimpleFilePath);
    tikalog4j2File = new File(tikalog4j2FilePath);
    tikalog4j2ChildFile = new File(tikalog4j2ChildFilePath);
    originalTikaBinFolderFile = new File(originalTikaBinFolderFilePath);
    tikaServerJarFile = new File(tikaServerJarFilePath);
    db = dbf.newDocumentBuilder();
    transformer = tf.newTransformer();
  }

  public static TikaServerCreator getInstance() throws ParserConfigurationException, TransformerConfigurationException {
    if (instance == null) {
      instance = new TikaServerCreator();
    }
    return instance;
  }

  public void createTikaOCRServer(final String installationPath, final String ocrStrategy, final String tikaHost, final String tikaPort, final String tmpDir)
      throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db = dbf.newDocumentBuilder();
    final Document tikaConfigDoc = db.parse(tikaConfOCRFile);
    final Element tikaConfigRoot = tikaConfigDoc.getDocumentElement();

    updateTikaConf(tikaConfigRoot, tikaHost, tikaPort, tmpDir);
    updateTikaOCRStrategy(tikaConfigRoot, ocrStrategy);
    final String tikaName = "Tika-OCR-Server";
    final Document log4j2Conf = createTikaLog4j2PropertiesDoc(tikaName);
    final Document log4j2ChildConf = createTikaLog4j2ChildPropertiesDoc(tikaName);

    copyTikaFiles(installationPath, tikaConfigDoc, log4j2Conf, log4j2ChildConf);
  }

  public void createTikaSimpleServer(final String installationPath, final String tikaHost, final String tikaPort, final String tmpDir)
      throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerException {
    final Document tikaConfigDoc = db.parse(tikaConfSimpleFile);
    final Element tikaConfigRoot = tikaConfigDoc.getDocumentElement();

    updateTikaConf(tikaConfigRoot, tikaHost, tikaPort, tmpDir);
    final String tikaName = "Tika-Simple-Server";
    final Document log4j2Conf = createTikaLog4j2PropertiesDoc(tikaName);
    final Document log4j2ChildConf = createTikaLog4j2ChildPropertiesDoc(tikaName);

    copyTikaFiles(installationPath, tikaConfigDoc, log4j2Conf, log4j2ChildConf);
  }

  private void updateTikaConf(final Element tikaConfigRoot, final String tikaHost, final String tikaPort, final String tmpDir) throws XPathExpressionException {
    // Change the tika host
    final Node hostNode = (Node) xPath.compile("/properties/server/params/host").evaluate(tikaConfigRoot, XPathConstants.NODE);
    hostNode.setTextContent(tikaHost);
    // Change the tika port
    final Node portNode = (Node) xPath.compile("/properties/server/params/port").evaluate(tikaConfigRoot, XPathConstants.NODE);
    portNode.setTextContent(tikaPort);
    // Change temp dir
    final NodeList forkedJvmArgs = (NodeList) xPath.compile("/properties/server/params/forkedJvmArgs/arg").evaluate(tikaConfigRoot, XPathConstants.NODESET);
    for (int i = 0; i < forkedJvmArgs.getLength(); i++) {
      final Node jvmArg = forkedJvmArgs.item(i);
      if (jvmArg.getTextContent().startsWith("-Djava.io.tmpdir")) {
        jvmArg.setTextContent("-Djava.io.tmpdir=" + tmpDir);
      }
    }
  }

  private void updateTikaOCRStrategy(final Element tikaConfigRoot, final String ocrStrategy) throws XPathExpressionException {
    final Node pdfParserNode = (Node) xPath.compile("/properties/parsers/parser[@class='org.apache.tika.parser.pdf.PDFParser']/params/param[@name='ocrStrategy']").evaluate(tikaConfigRoot,
        XPathConstants.NODE);
    pdfParserNode.setTextContent(ocrStrategy);
  }

  private Document createTikaLog4j2PropertiesDoc(final String tikaName) throws SAXException, IOException, XPathExpressionException {
    final Document tikalog4j2Doc = db.parse(tikalog4j2File);
    final Element tikalog4j2El = tikalog4j2Doc.getDocumentElement();
    setTikaNameInLog4j2Conf(tikalog4j2El, tikaName);
    return tikalog4j2Doc;
  }

  private Document createTikaLog4j2ChildPropertiesDoc(final String tikaName) throws SAXException, IOException, XPathExpressionException {
    final Document tikalog4j2Doc = db.parse(tikalog4j2ChildFile);
    final Element tikalog4j2El = tikalog4j2Doc.getDocumentElement();
    setTikaNameInLog4j2Conf(tikalog4j2El, tikaName);
    return tikalog4j2Doc;
  }

  private void setTikaNameInLog4j2Conf(final Element tikalog4j2El, final String tikaName) throws XPathExpressionException {
    final Node log4j2PatternNode = (Node) xPath.compile("/Configuration/Appenders/RollingFile/PatternLayout/Pattern").evaluate(tikalog4j2El, XPathConstants.NODE);
    final String patternContent = log4j2PatternNode.getTextContent();
    log4j2PatternNode.setTextContent(patternContent.replace("Tika-server", tikaName));
  }

  private void copyTikaFiles(final String installFolderPath, final Document tikaConfigDoc, final Document tikalog4j2Doc, final Document tikalog4j2ChildDoc) throws IOException, TransformerException {
    final String installConfFolderPath = installFolderPath + "/conf";
    final File installConfFolder = new File(installConfFolderPath);
    final StreamResult sr = new StreamResult();
    // create conf folder
    installConfFolder.mkdir();
    // create tika-config.xml
    FileWriter fw = new FileWriter(installConfFolderPath + "/tika-config.xml");
    sr.setWriter(fw);
    DOMSource domSrc = new DOMSource(tikaConfigDoc);
    transformer.transform(domSrc, sr);

    // create log4j2.properties.xml
    domSrc = new DOMSource(tikalog4j2Doc);
    // Reinit string writer
    fw.close();
    fw = new FileWriter(installConfFolderPath + "/log4j2.properties.xml");
    sr.setWriter(fw);
    transformer.transform(domSrc, sr);

    // create log4j2child.properties.xml
    domSrc = new DOMSource(tikalog4j2ChildDoc);
    // Reinit string writer
    fw.close();
    fw = new FileWriter(installConfFolderPath + "/log4j2child.properties.xml");
    sr.setWriter(fw);
    transformer.transform(domSrc, sr);

    // Close streams
    fw.close();

    // Copy other files
    final File installFolderBinFolderFile = new File(installFolderPath + "/bin");
    FileUtils.copyDirectory(originalTikaBinFolderFile, installFolderBinFolderFile);
    final File installFolderTikaServerJarFile = new File(installFolderBinFolderFile + "/tika-server.jar");
    FileUtils.copyFile(tikaServerJarFile, installFolderTikaServerJarFile);
  }

}
