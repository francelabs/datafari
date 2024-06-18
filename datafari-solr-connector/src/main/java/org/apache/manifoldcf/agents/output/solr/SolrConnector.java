/* $Id: SolrConnector.java 998081 2010-09-17 11:33:15Z kwright $ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.manifoldcf.agents.output.solr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputCheckActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputNotifyActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputRemoveActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;
import org.apache.manifoldcf.connectorcommon.interfaces.KeystoreManagerFactory;
import org.apache.manifoldcf.core.interfaces.ConfigNode;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.ConfigurationNode;
import org.apache.manifoldcf.core.interfaces.IDFactory;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.VersionContext;
import org.apache.manifoldcf.crawler.system.Logging;

/**
 * This is the output connector for SOLR. Currently, no frills.
 */
public class SolrConnector extends org.apache.manifoldcf.agents.output.BaseOutputConnector {
  public static final String _rcsid = "@(#)$Id: SolrConnector.java 998081 2010-09-17 11:33:15Z kwright $";

  // Activities we log

  /** Ingestion activity */
  public final static String INGEST_ACTIVITY = "document ingest";
  /** Document removal activity */
  public final static String REMOVE_ACTIVITY = "document deletion";

  /** Local connection */
  protected HttpPoster poster = null;

  /** Expiration */
  protected long expirationTime = -1L;

  /** The allow attribute name */
  protected String allowAttributeName = "allow_token_";
  /** The deny attribute name */
  protected String denyAttributeName = "deny_token_";
  /** The maximum document length */
  protected Long maxDocumentLength = null;
  /** Included mime types string */
  protected String includedMimeTypesString = null;
  /** Included mime types */
  protected Set<String> includedMimeTypes = null;
  /** Excluded mime types string */
  protected String excludedMimeTypesString = null;
  /** Excluded mime types */
  protected Set<String> excludedMimeTypes = null;

  // Attributes going into Solr
  protected String idAttributeName = null;
  protected String originalSizeAttributeName = null;
  protected String modifiedDateAttributeName = null;
  protected String createdDateAttributeName = null;
  protected String indexedDateAttributeName = null;
  protected String fileNameAttributeName = null;
  protected String mimeTypeAttributeName = null;
  protected String contentAttributeName = null;
  /** Use extractiing update handler? */
  protected boolean useExtractUpdateHandler = true;
  /** Allow compression? Currently static */
  protected final static boolean allowCompression = true;

  /** Whether or not to commit */
  protected boolean doCommits = false;

  /** Collection name (non-empty only if SolrCloud) */
  protected String collectionName = null;

  /** Idle connection expiration interval */
  protected final static long EXPIRATION_INTERVAL = 300000L;

  static {
    System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", new Integer(Integer.MAX_VALUE).toString());
  }

  /**
   * Constructor.
   */
  public SolrConnector() {
  }

  /**
   * Return the list of activities that this connector supports (i.e. writes into the log).
   *
   * @return the list.
   */
  @Override
  public String[] getActivitiesList() {
    return new String[] { INGEST_ACTIVITY, REMOVE_ACTIVITY };
  }

  /**
   * Connect.
   *
   * @param configParameters
   *          is the set of configuration parameters, which in this case describe the target appliance, basic auth configuration, etc. (This formerly came out of the ini file.)
   */
  @Override
  public void connect(final ConfigParams configParameters) {
    super.connect(configParameters);
  }

  /**
   * This method is periodically called for all connectors that are connected but not in active use.
   */
  @Override
  public void poll() throws ManifoldCFException {
    if (poster != null) {
      if (expirationTime <= System.currentTimeMillis()) {
        // Expire connection
        poster.shutdown();
        poster = null;
        expirationTime = -1L;
      }
    }
  }

  /**
   * This method is called to assess whether to count this connector instance should actually be counted as being connected.
   *
   * @return true if the connector instance is actually connected.
   */
  @Override
  public boolean isConnected() {
    return poster != null;
  }

  /**
   * Close the connection. Call this before discarding the connection.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    if (poster != null) {
      poster.shutdown();
      poster = null;
      expirationTime = -1L;
    }
    maxDocumentLength = null;
    includedMimeTypesString = null;
    includedMimeTypes = null;
    excludedMimeTypesString = null;
    excludedMimeTypes = null;
    idAttributeName = null;
    originalSizeAttributeName = null;
    modifiedDateAttributeName = null;
    createdDateAttributeName = null;
    indexedDateAttributeName = null;
    fileNameAttributeName = null;
    mimeTypeAttributeName = null;
    contentAttributeName = null;
    useExtractUpdateHandler = true;
    collectionName = null;
    super.disconnect();
  }

  /** Set up a session */
  protected void getSession() throws ManifoldCFException {
    if (poster == null) {
      String updatePath = params.getParameter(SolrConfig.PARAM_UPDATEPATH);
      if (updatePath == null || updatePath.length() == 0)
        updatePath = "";

      String removePath = params.getParameter(SolrConfig.PARAM_REMOVEPATH);
      if (removePath == null || removePath.length() == 0)
        removePath = "";

      String statusPath = params.getParameter(SolrConfig.PARAM_STATUSPATH);
      if (statusPath == null || statusPath.length() == 0)
        statusPath = "";

      idAttributeName = params.getParameter(SolrConfig.PARAM_IDFIELD);
      if (idAttributeName == null || idAttributeName.length() == 0)
        idAttributeName = "id";

      originalSizeAttributeName = params.getParameter(SolrConfig.PARAM_ORIGINALSIZEFIELD);
      if (originalSizeAttributeName == null || originalSizeAttributeName.length() == 0)
        originalSizeAttributeName = null;

      modifiedDateAttributeName = params.getParameter(SolrConfig.PARAM_MODIFIEDDATEFIELD);
      if (modifiedDateAttributeName == null || modifiedDateAttributeName.length() == 0)
        modifiedDateAttributeName = null;

      createdDateAttributeName = params.getParameter(SolrConfig.PARAM_CREATEDDATEFIELD);
      if (createdDateAttributeName == null || createdDateAttributeName.length() == 0)
        createdDateAttributeName = null;

      indexedDateAttributeName = params.getParameter(SolrConfig.PARAM_INDEXEDDATEFIELD);
      if (indexedDateAttributeName == null || indexedDateAttributeName.length() == 0)
        indexedDateAttributeName = null;

      fileNameAttributeName = params.getParameter(SolrConfig.PARAM_FILENAMEFIELD);
      if (fileNameAttributeName == null || fileNameAttributeName.length() == 0)
        fileNameAttributeName = null;

      mimeTypeAttributeName = params.getParameter(SolrConfig.PARAM_MIMETYPEFIELD);
      if (mimeTypeAttributeName == null || mimeTypeAttributeName.length() == 0)
        mimeTypeAttributeName = null;

      contentAttributeName = params.getParameter(SolrConfig.PARAM_CONTENTFIELD);
      if (contentAttributeName == null || contentAttributeName.length() == 0)
        contentAttributeName = null;

      final String useExtractUpdateHandlerValue = params.getParameter(SolrConfig.PARAM_EXTRACTUPDATE);
      if (useExtractUpdateHandlerValue == null || useExtractUpdateHandlerValue.length() == 0)
        useExtractUpdateHandler = true;
      else
        useExtractUpdateHandler = !useExtractUpdateHandlerValue.equals("false");
      if (contentAttributeName == null && !useExtractUpdateHandler)
        throw new ManifoldCFException("Content attribute name required for non-extract-update indexing");

      String commits = params.getParameter(SolrConfig.PARAM_COMMITS);
      if (commits == null || commits.length() == 0)
        commits = "true";

      doCommits = commits.equals("true");

      String commitWithin = params.getParameter(SolrConfig.PARAM_COMMITWITHIN);
      if (commitWithin == null || commitWithin.length() == 0)
        commitWithin = null;

      final String docMax = params.getParameter(SolrConfig.PARAM_MAXLENGTH);
      if (docMax == null || docMax.length() == 0)
        maxDocumentLength = null;
      else
        maxDocumentLength = new Long(docMax);
      if (maxDocumentLength == null && !useExtractUpdateHandler)
        throw new ManifoldCFException("Maximum document length required for non-extract-update indexing");

      includedMimeTypesString = params.getParameter(SolrConfig.PARAM_INCLUDEDMIMETYPES);
      if (includedMimeTypesString == null || includedMimeTypesString.length() == 0) {
        includedMimeTypesString = null;
        includedMimeTypes = null;
      } else {
        // Parse the included mime types
        includedMimeTypes = parseMimeTypes(includedMimeTypesString);
        if (includedMimeTypes.size() == 0) {
          includedMimeTypesString = null;
          includedMimeTypes = null;
        }
      }

      excludedMimeTypesString = params.getParameter(SolrConfig.PARAM_EXCLUDEDMIMETYPES);
      if (excludedMimeTypesString == null || excludedMimeTypesString.length() == 0) {
        excludedMimeTypesString = null;
        excludedMimeTypes = null;
      } else {
        // Parse the included mime types
        excludedMimeTypes = parseMimeTypes(excludedMimeTypesString);
        if (excludedMimeTypes.size() == 0) {
          excludedMimeTypesString = null;
          excludedMimeTypes = null;
        }
      }

      // Now, initialize Solr-j
      String solrType = params.getParameter(SolrConfig.PARAM_SOLR_TYPE);
      if (solrType == null)
        solrType = SolrConfig.SOLR_TYPE_STANDARD;

      if (solrType.equals(SolrConfig.SOLR_TYPE_STANDARD)) {
        final String userID = params.getParameter(SolrConfig.PARAM_USERID);
        final String password = params.getObfuscatedParameter(SolrConfig.PARAM_PASSWORD);
        final String realm = params.getParameter(SolrConfig.PARAM_REALM);
        final String keystoreData = params.getParameter(SolrConfig.PARAM_KEYSTORE);
        IKeystoreManager keystoreManager;
        if (keystoreData != null)
          keystoreManager = KeystoreManagerFactory.make("", keystoreData);
        else
          keystoreManager = null;

        final String protocol = params.getParameter(SolrConfig.PARAM_PROTOCOL);
        if (protocol == null || protocol.length() == 0)
          throw new ManifoldCFException("Missing parameter: " + SolrConfig.PARAM_PROTOCOL);

        final String server = params.getParameter(SolrConfig.PARAM_SERVER);
        if (server == null || server.length() == 0)
          throw new ManifoldCFException("Missing parameter: " + SolrConfig.PARAM_SERVER);

        String port = params.getParameter(SolrConfig.PARAM_PORT);
        if (port == null || port.length() == 0)
          port = "80";

        String webapp = params.getParameter(SolrConfig.PARAM_WEBAPPNAME);
        if (webapp != null && webapp.length() == 0)
          webapp = null;

        String core = params.getParameter(SolrConfig.PARAM_CORE);
        if (core != null && core.length() == 0)
          core = "collection1";

        // Pick up timeouts
        String socketTimeoutString = params.getParameter(SolrConfig.PARAM_SOCKET_TIMEOUT);
        if (socketTimeoutString == null)
          socketTimeoutString = "900";
        String connectTimeoutString = params.getParameter(SolrConfig.PARAM_CONNECTION_TIMEOUT);
        if (connectTimeoutString == null)
          connectTimeoutString = "60";

        collectionName = null;

        try {
          final int socketTimeout = Integer.parseInt(socketTimeoutString) * 1000;
          final int connectTimeout = Integer.parseInt(connectTimeoutString) * 1000;

          poster = new HttpPoster(protocol, server, Integer.parseInt(port), webapp, core, connectTimeout, socketTimeout, updatePath, removePath, statusPath, realm, userID, password, allowAttributeName, denyAttributeName, idAttributeName,
              originalSizeAttributeName, modifiedDateAttributeName, createdDateAttributeName, indexedDateAttributeName, fileNameAttributeName, mimeTypeAttributeName, contentAttributeName, keystoreManager, maxDocumentLength, commitWithin,
              useExtractUpdateHandler, includedMimeTypes, excludedMimeTypes, allowCompression);

        } catch (final NumberFormatException e) {
          throw new ManifoldCFException(e.getMessage());
        }

      } else if (solrType.equals(SolrConfig.SOLR_TYPE_SOLRCLOUD)) {
        final List<String> zookeeperHosts = new ArrayList<>();
        // Pull together the zookeeper string describing the zookeeper nodes
        for (int i = 0; i < params.getChildCount(); i++) {
          final ConfigurationNode cn = params.getChild(i);
          if (cn.getType().equals(SolrConfig.NODE_ZOOKEEPER)) {
            zookeeperHosts.add(cn.getAttributeValue(SolrConfig.ATTR_HOST) + ":" + cn.getAttributeValue(SolrConfig.ATTR_PORT));
          }
        }

        String znodePath = params.getParameter(SolrConfig.PARAM_ZOOKEEPER_ZNODE_PATH);

        // Get collection
        String collection = params.getParameter(SolrConfig.PARAM_COLLECTION);
        if (collection == null)
          collection = "collection1";
        collectionName = collection;

        // Pick up timeouts
        String zkSocketTimeoutString = params.getParameter(SolrConfig.PARAM_ZOOKEEPER_SOCKET_TIMEOUT);
        if (zkSocketTimeoutString == null)
          zkSocketTimeoutString = "60";
        String zkConnectionTimeoutString = params.getParameter(SolrConfig.PARAM_ZOOKEEPER_CONNECTION_TIMEOUT);
        if (zkConnectionTimeoutString == null)
          zkConnectionTimeoutString = "60";

        // Create an httpposter
        try {
          final int zkClientTimeout = Integer.parseInt(zkSocketTimeoutString) * 1000;
          final int zkConnectTimeout = Integer.parseInt(zkConnectionTimeoutString) * 1000;

          poster = new HttpPoster(zookeeperHosts, znodePath, collection, zkClientTimeout, zkConnectTimeout, updatePath, removePath, statusPath, allowAttributeName, denyAttributeName, idAttributeName, originalSizeAttributeName,
              modifiedDateAttributeName, createdDateAttributeName, indexedDateAttributeName, fileNameAttributeName, mimeTypeAttributeName, contentAttributeName, maxDocumentLength, commitWithin, useExtractUpdateHandler, includedMimeTypes,
              excludedMimeTypes, allowCompression);

        } catch (final NumberFormatException e) {
          throw new ManifoldCFException(e.getMessage());
        }

      } else
        throw new ManifoldCFException("Illegal value for parameter '" + SolrConfig.PARAM_SOLR_TYPE + "': '" + solrType + "'");

    }
    expirationTime = System.currentTimeMillis() + EXPIRATION_INTERVAL;
  }

  /** Parse a mime type field into individual mime types in a hash */
  protected static Set<String> parseMimeTypes(final String mimeTypes) throws ManifoldCFException {
    final Set<String> rval = new HashSet<>();
    try {
      final java.io.Reader str = new java.io.StringReader(mimeTypes);
      try {
        final java.io.BufferedReader is = new java.io.BufferedReader(str);
        try {
          while (true) {
            final String nextString = is.readLine();
            if (nextString == null)
              break;
            if (nextString.length() == 0)
              continue;
            rval.add(nextString.toLowerCase(Locale.ROOT));
          }
          return rval;
        } finally {
          is.close();
        }
      } finally {
        str.close();
      }
    } catch (final java.io.IOException e) {
      throw new ManifoldCFException("IO error: " + e.getMessage(), e);
    }
  }

  /**
   * Test the connection. Returns a string describing the connection integrity.
   *
   * @return the connection's status as a displayable string.
   */
  @Override
  public String check() throws ManifoldCFException {
    try {
      getSession();
      poster.checkPost();
      return super.check();
    } catch (final ServiceInterruption e) {
      return "Transient error: " + e.getMessage();
    }
  }

  /**
   * Get an output version string, given an output specification. The output version string is used to uniquely describe the pertinent details of the output specification and the configuration, to
   * allow the Connector Framework to determine whether a document will need to be output again. Note that the contents of the document cannot be considered by this method, and that a different
   * version string (defined in IRepositoryConnector) is used to describe the version of the actual document.
   *
   * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be necessary.
   *
   * @param spec
   *          is the current output specification for the job that is doing the crawling.
   * @return a string, of unlimited length, which uniquely describes output configuration and specification in such a way that if two such strings are equal, the document will not need to be sent
   *         again to the output data store.
   */
  @Override
  public VersionContext getPipelineDescription(final Specification spec) throws ManifoldCFException, ServiceInterruption {
    getSession();
    final SpecPacker sp = new SpecPacker(spec);
    return new VersionContext(sp.toPackedString(), params, spec);
  }

  /**
   * Detect if a mime type is indexable or not. This method is used by participating repository connectors to pre-filter the number of unusable documents that will be passed to this output connector.
   *
   * @param outputDescription
   *          is the document's output version.
   * @param mimeType
   *          is the mime type of the document.
   * @return true if the mime type is indexable by this connector.
   */
  @Override
  public boolean checkMimeTypeIndexable(final VersionContext outputDescription, final String mimeType, final IOutputCheckActivity activities) throws ManifoldCFException, ServiceInterruption {
    getSession();
    return HttpPoster.checkMimeTypeIndexable(mimeType, useExtractUpdateHandler, includedMimeTypes, excludedMimeTypes);
  }

  /**
   * Pre-determine whether a document's length is indexable by this connector. This method is used by participating repository connectors to help filter out documents that are too long to be
   * indexable.
   *
   * @param outputDescription
   *          is the document's output version.
   * @param length
   *          is the length of the document.
   * @return true if the file is indexable.
   */
  @Override
  public boolean checkLengthIndexable(final VersionContext outputDescription, final long length, final IOutputCheckActivity activities) throws ManifoldCFException, ServiceInterruption {
    getSession();
    if (maxDocumentLength != null && length > maxDocumentLength.longValue())
      return false;
    return true;
  }

  /**
   * Add (or replace) a document in the output data store using the connector. This method presumes that the connector object has been configured, and it is thus able to communicate with the output
   * data store should that be necessary.
   *
   * @param documentURI
   *          is the URI of the document. The URI is presumed to be the unique identifier which the output data store will use to process and serve the document. This URI is constructed by the
   *          repository connector which fetches the document, and is thus universal across all output connectors.
   * @param pipelineDescription
   *          includes the description string that was constructed for this document by the getOutputDescription() method.
   * @param document
   *          is the document data to be processed (handed to the output data store).
   * @param authorityNameString
   *          is the name of the authority responsible for authorizing any access tokens passed in with the repository document. May be null.
   * @param activities
   *          is the handle to an object that the implementer of a pipeline connector may use to perform operations, such as logging processing activity, or sending a modified document to the next
   *          stage in the pipeline.
   * @return the document status (accepted or permanently rejected).
   * @throws IOException
   *           only if there's a stream error reading the document data.
   */
  @Override
  public int addOrReplaceDocumentWithException(final String documentURI, final VersionContext pipelineDescription, final RepositoryDocument document, final String authorityNameString, final IOutputAddActivity activities)
      throws ManifoldCFException, ServiceInterruption, IOException {
    final SpecPacker sp = new SpecPacker(pipelineDescription.getSpecification());

    // Establish a session
    getSession();

    String title = normalizeTitle(document.getFileName());
    document.setFileName(title);

    if (Logging.connectors.isDebugEnabled()) {
      logRepositoryDocument(document, documentURI);
    }

    // Now, go off and call the ingest API.
    if (poster.indexPost(documentURI, document, sp.getArgs(), authorityNameString, activities))
      return DOCUMENTSTATUS_ACCEPTED;
    return DOCUMENTSTATUS_REJECTED;
  }

  /**
   * Remove a document using the connector. Note that the last outputDescription is included, since it may be necessary for the connector to use such information to know how to properly remove the
   * document.
   *
   * @param documentURI
   *          is the URI of the document. The URI is presumed to be the unique identifier which the output data store will use to process and serve the document. This URI is constructed by the
   *          repository connector which fetches the document, and is thus universal across all output connectors.
   * @param outputDescription
   *          is the last description string that was constructed for this document by the getOutputDescription() method above.
   * @param activities
   *          is the handle to an object that the implementer of an output connector may use to perform operations, such as logging processing activity.
   */
  @Override
  public void removeDocument(final String documentURI, final String outputDescription, final IOutputRemoveActivity activities) throws ManifoldCFException, ServiceInterruption {
    // Establish a session
    getSession();
    poster.deletePost(documentURI, activities);
  }

  /**
   * Notify the connector of a completed job. This is meant to allow the connector to flush any internal data structures it has been keeping around, or to tell the output repository that this is a
   * good time to synchronize things. It is called whenever a job is either completed or aborted.
   *
   * @param activities
   *          is the handle to an object that the implementer of an output connector may use to perform operations, such as logging processing activity.
   */
  @Override
  public void noteJobComplete(final IOutputNotifyActivity activities) throws ManifoldCFException, ServiceInterruption {
    // Establish a session
    getSession();

    // Do a commit post
    if (doCommits) {
      poster.commitPost();
    }
  }

  // UI support methods.
  //
  // These support methods come in two varieties. The first bunch is involved in setting up connection configuration information. The second bunch
  // is involved in presenting and editing output specification information for a job. The two kinds of methods are accordingly treated differently,
  // in that the first bunch cannot assume that the current connector object is connected, while the second bunch can. That is why the first bunch
  // receives a thread context argument for all UI methods, while the second bunch does not need one (since it has already been applied via the connect()
  // method, above).

  /**
   * Output the configuration header section. This method is called in the head section of the connector's configuration page. Its purpose is to add the required tabs to the list, and to output any
   * javascript methods that might be needed by the configuration editing HTML.
   *
   * @param threadContext
   *          is the local thread context.
   * @param out
   *          is the output to which any HTML should be sent.
   * @param parameters
   *          are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabsArray
   *          is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final List<String> tabsArray) throws ManifoldCFException, IOException {
    tabsArray.add(Messages.getString(locale, "SolrConnector.SolrType"));
    tabsArray.add(Messages.getString(locale, "SolrConnector.Server"));
    tabsArray.add(Messages.getString(locale, "SolrConnector.Zookeeper"));
    tabsArray.add(Messages.getString(locale, "SolrConnector.Paths"));
    tabsArray.add(Messages.getString(locale, "SolrConnector.Schema"));
    tabsArray.add(Messages.getString(locale, "SolrConnector.Arguments"));
    tabsArray.add(Messages.getString(locale, "SolrConnector.Documents"));
    tabsArray.add(Messages.getString(locale, "SolrConnector.Commits"));

    out.print("<script type=\"text/javascript\">\n" + "<!--\n" + "function SolrDeleteCertificate(aliasName)\n" + "{\n" + "  editconnection.solrkeystorealias.value = aliasName;\n" + "  editconnection.configop.value = \"Delete\";\n" + "  postForm();\n"
        + "}\n" + "\n" + "function SolrAddCertificate()\n" + "{\n" + "  if (editconnection.solrcertificate.value == \"\")\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.ChooseACertificateFile") + "\");\n"
        + "    editconnection.solrcertificate.focus();\n" + "  }\n" + "  else\n" + "  {\n" + "    editconnection.configop.value = \"Add\";\n" + "    postForm();\n" + "  }\n" + "}\n" + "\n" + "function checkConfig()\n" + "{\n"
        + "  if (editconnection.servername.value == \"\")\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.PleaseSupplyAValidSolrServerName") + "\");\n" + "    editconnection.servername.focus();\n"
        + "    return false;\n" + "  }\n" + "  if (editconnection.serverport.value != \"\" && !isInteger(editconnection.serverport.value))\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.SolrServerPortMustBeAValidInteger") + "\");\n" + "    editconnection.serverport.focus();\n" + "    return false;\n" + "  }\n"
        + "  if (editconnection.webappname.value != \"\" && editconnection.webappname.value.indexOf(\"/\") != -1)\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.WebApplicationNameCannotHaveCharacters")
        + "\");\n" + "    editconnection.webappname.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.core.value != \"\" && editconnection.core.value.indexOf(\"/\") != -1)\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.CoreNameCannotHaveCharacters") + "\");\n" + "    editconnection.core.focus();\n" + "    return false;\n" + "  }\n"
        + "  if (editconnection.webappname.value == \"\" && editconnection.core.value != \"\")\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.WebApplicationMustBeSpecifiedIfCoreIsSpecified") + "\");\n"
        + "    editconnection.webappname.focus();\n" + "    return false;\n" + "  }\n" + "  if (!isInteger(editconnection.connectiontimeout.value))\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.ConnectionTimeoutMustBeInteger") + "\");\n" + "    editconnection.connectiontimeout.focus();\n" + "    return false;\n" + "  }\n"
        + "  if (!isInteger(editconnection.sockettimeout.value))\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.SocketTimeoutMustBeInteger") + "\");\n" + "    editconnection.sockettimeout.focus();\n"
        + "    return false;\n" + "  }\n" + "  if (editconnection.updatepath.value != \"\" && editconnection.updatepath.value.substring(0,1) != \"/\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.UpdatePathMustStartWithACharacter") + "\");\n" + "    editconnection.updatepath.focus();\n" + "    return false;\n" + "  }\n"
        + "  if (editconnection.removepath.value != \"\" && editconnection.removepath.value.substring(0,1) != \"/\")\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.RemovePathMustStartWithACharacter")
        + "\");\n" + "    editconnection.removepath.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.statuspath.value != \"\" && editconnection.statuspath.value.substring(0,1) != \"/\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.StatusPathMustStartWithACharacter") + "\");\n" + "    editconnection.statuspath.focus();\n" + "    return false;\n" + "  }\n"
        + "  if (editconnection.maxdocumentlength.value != \"\" && !isInteger(editconnection.maxdocumentlength.value))\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.MaximumDocumentLengthMustBAnInteger")
        + "\");\n" + "    editconnection.maxdocumentlength.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.commitwithin.value != \"\" && !isInteger(editconnection.commitwithin.value))\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.CommitWithinValueMustBeAnInteger") + "\");\n" + "    editconnection.commitwithin.focus();\n" + "    return false;\n" + "  }\n"
        + "  if (editconnection.znodepath.value != \"\" && editconnection.znodepath.value.substring(0,1) != \"/\")\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.ZnodePathMustStartWithACharacter") + "\");\n"
        + "    editconnection.znodepath.focus();\n" + "    return false;\n" + "  }\n" + "  return true;\n" + "}\n" + "\n" + "function checkConfigForSave()\n" + "{\n" + "  if (editconnection.servername.value == \"\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.PleaseSupplyAValidSolrServerName") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Server") + "\");\n"
        + "    editconnection.servername.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.serverport.value != \"\" && !isInteger(editconnection.serverport.value))\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.SolrServerPortMustBeAValidInteger") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Server") + "\");\n"
        + "    editconnection.serverport.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.webappname.value != \"\" && editconnection.webappname.value.indexOf(\"/\") != -1)\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.WebApplicationNameCannotHaveCharacters") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Server") + "\");\n"
        + "    editconnection.webappname.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.core.value == \"\")\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.PleaseSupplySolrCoreName")
        + "\");\n" + "    editconnection.core.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.core.value != \"\" && editconnection.core.value.indexOf(\"/\") != -1)\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.CoreNameCannotHaveCharacters") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Server") + "\");\n" + "    editconnection.core.focus();\n"
        + "    return false;\n" + "  }\n" + "  if (editconnection.webappname.value == \"\" && editconnection.core.value != \"\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.WebApplicationMustBeSpecifiedIfCoreIsSpecified") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Server") + "\");\n"
        + "    editconnection.webappname.focus();\n" + "    return false;\n" + "  }\n" + "  if (!isInteger(editconnection.connectiontimeout.value))\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.ConnectionTimeoutMustBeInteger") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Server") + "\");\n"
        + "    editconnection.connectiontimeout.focus();\n" + "    return false;\n" + "  }\n" + "  if (!isInteger(editconnection.sockettimeout.value))\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.SocketTimeoutMustBeInteger") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Server") + "\");\n"
        + "    editconnection.sockettimeout.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.updatepath.value != \"\" && editconnection.updatepath.value.substring(0,1) != \"/\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.UpdatePathMustStartWithACharacter") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Paths") + "\");\n"
        + "    editconnection.updatepath.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.removepath.value != \"\" && editconnection.removepath.value.substring(0,1) != \"/\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.RemovePathMustStartWithACharacter") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Paths") + "\");\n"
        + "    editconnection.removepath.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.statuspath.value != \"\" && editconnection.statuspath.value.substring(0,1) != \"/\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.StatusPathMustStartWithACharacter") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Paths") + "\");\n"
        + "    editconnection.statuspath.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.maxdocumentlength.value != \"\" && !isInteger(editconnection.maxdocumentlength.value))\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.MaximumDocumentLengthMustBeAnInteger") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Documents") + "\");\n"
        + "    editconnection.maxdocumentlength.focus();\n" + "    return false;\n" + "  }\n"
        + "  if (editconnection.maxdocumentlength.value == \"\" && ((editconnection.extractupdatecheckbox.value == \"true\" && editconnection.extractupdate.checked == false) || (editconnection.extractupdatecheckbox.value != \"true\" && editconnection.extractupdate.value != \"true\")))\n"
        + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.MaximumDocumentLengthRequiredUnlessExtractingUpdateHandler") + "\");\n" + "    SelectTab(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.Documents") + "\");\n" + "    editconnection.maxdocumentlength.focus();\n" + "    return false;\n" + "  }\n"
        + "  if (editconnection.contentfield.value == \"\" && ((editconnection.extractupdatecheckbox.value == \"true\" && editconnection.extractupdate.checked == false) || (editconnection.extractupdatecheckbox.value != \"true\" && editconnection.extractupdate.value != \"true\")))\n"
        + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.ContentFieldNameRequiredUnlessExtractingUpdateHandler") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Schema")
        + "\");\n" + "    editconnection.contentfield.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.commitwithin.value != \"\" && !isInteger(editconnection.commitwithin.value))\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.CommitWithinValueMustBeAnInteger") + "\");\n" + "    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.Commits") + "\");\n"
        + "    editconnection.commitwithin.focus();\n" + "    return false;\n" + "  }\n" + "  if (editconnection.znodepath.value != \"\" && editconnection.znodepath.value.substring(0,1) != \"/\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.ZnodePathMustStartWithACharacter") + "\");\n" + "    editconnection.znodepath.focus();\n" + "    return false;\n" + "  }\n" + "  return true;\n" + "}\n" + "\n"
        + "function deleteZookeeperHost(i)\n" + "{\n" + "  // Set the operation\n" + "  eval(\"editconnection.op_zookeeper_\"+i+\".value=\\\"Delete\\\"\");\n" + "  // Submit\n" + "  if (editconnection.count_zookeeper.value==i)\n"
        + "    postFormSetAnchor(\"zookeeper\");\n" + "  else\n" + "    postFormSetAnchor(\"zookeeper_\"+i)\n" + "  // Undo, so we won't get two deletes next time\n" + "  eval(\"editconnection.op_zookeeper_\"+i+\".value=\\\"Continue\\\"\");\n"
        + "}\n" + "\n" + "function addZookeeperHost()\n" + "{\n" + "  if (editconnection.host_zookeeper.value == \"\")\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.ZookeeperHostCannotBeNull") + "\");\n"
        + "    editconnection.host_zookeeper.focus();\n" + "    return;\n" + "  }\n" + "  if (editconnection.port_zookeeper.value == \"\")\n" + "  {\n" + "    alert(\""
        + Messages.getBodyJavascriptString(locale, "SolrConnector.ZookeeperPortCannotBeNull") + "\");\n" + "    editconnection.port_zookeeper.focus();\n" + "    return;\n" + "  }\n" + "  if (!isInteger(editconnection.port_zookeeper.value))\n"
        + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.ZookeeperPortMustBeAnInteger") + "\");\n" + "    editconnection.port_zookeeper.focus();\n" + "    return;\n" + "  }\n"
        + "  editconnection.op_zookeeper.value=\"Add\";\n" + "  postFormSetAnchor(\"zookeeper\");\n" + "}\n" + "\n" + "function deleteArgument(i)\n" + "{\n" + "  // Set the operation\n"
        + "  eval(\"editconnection.argument_\"+i+\"_op.value=\\\"Delete\\\"\");\n" + "  // Submit\n" + "  if (editconnection.argument_count.value==i)\n" + "    postFormSetAnchor(\"argument\");\n" + "  else\n"
        + "    postFormSetAnchor(\"argument_\"+i)\n" + "  // Undo, so we won't get two deletes next time\n" + "  eval(\"editconnection.argument_\"+i+\"_op.value=\\\"Continue\\\"\");\n" + "}\n" + "\n" + "function addArgument()\n" + "{\n"
        + "  if (editconnection.argument_name.value == \"\")\n" + "  {\n" + "    alert(\"" + Messages.getBodyJavascriptString(locale, "SolrConnector.ArgumentNameCannotBeAnEmptyString") + "\");\n" + "    editconnection.argument_name.focus();\n"
        + "    return;\n" + "  }\n" + "  editconnection.argument_op.value=\"Add\";\n" + "  postFormSetAnchor(\"argument\");\n" + "}\n" + "\n" + "//-->\n" + "</script>\n");

  }

  /**
   * Output the configuration body section. This method is called in the body section of the connector's configuration page. Its purpose is to present the required form elements for editing. The coder
   * can presume that the HTML that is output from this configuration will be within appropriate &lt;html&gt;, &lt;body&gt;, and &lt;form&gt; tags. The name of the form is "editconnection".
   *
   * @param threadContext
   *          is the local thread context.
   * @param out
   *          is the output to which any HTML should be sent.
   * @param parameters
   *          are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabName
   *          is the current tab name.
   */
  @Override
  public void outputConfigurationBody(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final String tabName) throws ManifoldCFException, IOException {
    String type = parameters.getParameter(SolrConfig.PARAM_SOLR_TYPE);
    if (type == null)
      type = SolrConfig.SOLR_TYPE_STANDARD;

    String protocol = parameters.getParameter(SolrConfig.PARAM_PROTOCOL);
    if (protocol == null)
      protocol = SolrConfig.PROTOCOL_TYPE_HTTP;

    String server = parameters.getParameter(SolrConfig.PARAM_SERVER);
    if (server == null)
      server = "localhost";

    String port = parameters.getParameter(SolrConfig.PARAM_PORT);
    if (port == null)
      port = "8983";

    String webapp = parameters.getParameter(SolrConfig.PARAM_WEBAPPNAME);
    if (webapp == null)
      webapp = "solr";

    String core = parameters.getParameter(SolrConfig.PARAM_CORE);
    if (core == null)
      core = "collection1";

    String znodePath = parameters.getParameter(SolrConfig.PARAM_ZOOKEEPER_ZNODE_PATH);
    if (znodePath == null)
      znodePath = "";

    String collection = parameters.getParameter(SolrConfig.PARAM_COLLECTION);
    if (collection == null)
      collection = "collection1";

    String connectionTimeout = parameters.getParameter(SolrConfig.PARAM_CONNECTION_TIMEOUT);
    if (connectionTimeout == null)
      connectionTimeout = "60";

    String socketTimeout = parameters.getParameter(SolrConfig.PARAM_SOCKET_TIMEOUT);
    if (socketTimeout == null)
      socketTimeout = "900";

    String zkSocketTimeout = parameters.getParameter(SolrConfig.PARAM_ZOOKEEPER_SOCKET_TIMEOUT);
    if (zkSocketTimeout == null)
      zkSocketTimeout = "60";

    String zkConnectionTimeout = parameters.getParameter(SolrConfig.PARAM_ZOOKEEPER_CONNECTION_TIMEOUT);
    if (zkConnectionTimeout == null)
      zkConnectionTimeout = "60";

    String updatePath = parameters.getParameter(SolrConfig.PARAM_UPDATEPATH);
    if (updatePath == null)
      updatePath = "/update/extract";

    String removePath = parameters.getParameter(SolrConfig.PARAM_REMOVEPATH);
    if (removePath == null)
      removePath = "/update";

    String statusPath = parameters.getParameter(SolrConfig.PARAM_STATUSPATH);
    if (statusPath == null)
      statusPath = "/admin/ping";

    String idField = parameters.getParameter(SolrConfig.PARAM_IDFIELD);
    if (idField == null)
      idField = "id";

    String originalSizeField = parameters.getParameter(SolrConfig.PARAM_ORIGINALSIZEFIELD);
    if (originalSizeField == null)
      originalSizeField = "";

    String modifiedDateField = parameters.getParameter(SolrConfig.PARAM_MODIFIEDDATEFIELD);
    if (modifiedDateField == null)
      modifiedDateField = "";

    String createdDateField = parameters.getParameter(SolrConfig.PARAM_CREATEDDATEFIELD);
    if (createdDateField == null)
      createdDateField = "";

    String indexedDateField = parameters.getParameter(SolrConfig.PARAM_INDEXEDDATEFIELD);
    if (indexedDateField == null)
      indexedDateField = "";

    String fileNameField = parameters.getParameter(SolrConfig.PARAM_FILENAMEFIELD);
    if (fileNameField == null)
      fileNameField = "";

    String mimeTypeField = parameters.getParameter(SolrConfig.PARAM_MIMETYPEFIELD);
    if (mimeTypeField == null)
      mimeTypeField = "";

    String contentField = parameters.getParameter(SolrConfig.PARAM_CONTENTFIELD);
    if (contentField == null)
      contentField = "";

    String useExtractUpdate = parameters.getParameter(SolrConfig.PARAM_EXTRACTUPDATE);
    if (useExtractUpdate == null || useExtractUpdate.length() == 0)
      useExtractUpdate = "true";

    String realm = parameters.getParameter(SolrConfig.PARAM_REALM);
    if (realm == null)
      realm = "";

    String userID = parameters.getParameter(SolrConfig.PARAM_USERID);
    if (userID == null)
      userID = "";

    String password = parameters.getObfuscatedParameter(SolrConfig.PARAM_PASSWORD);
    if (password == null)
      password = "";
    else
      password = out.mapPasswordToKey(password);

    String commits = parameters.getParameter(SolrConfig.PARAM_COMMITS);
    if (commits == null)
      commits = "true";

    String commitWithin = parameters.getParameter(SolrConfig.PARAM_COMMITWITHIN);
    if (commitWithin == null)
      commitWithin = "";

    final String solrKeystore = parameters.getParameter(SolrConfig.PARAM_KEYSTORE);
    IKeystoreManager localKeystore;
    if (solrKeystore == null)
      localKeystore = KeystoreManagerFactory.make("");
    else
      localKeystore = KeystoreManagerFactory.make("", solrKeystore);

    String maxLength = parameters.getParameter(SolrConfig.PARAM_MAXLENGTH);
    if (maxLength == null)
      maxLength = "";

    String includedMimeTypes = parameters.getParameter(SolrConfig.PARAM_INCLUDEDMIMETYPES);
    if (includedMimeTypes == null)
      includedMimeTypes = "";

    String excludedMimeTypes = parameters.getParameter(SolrConfig.PARAM_EXCLUDEDMIMETYPES);
    if (excludedMimeTypes == null)
      excludedMimeTypes = "";

    // "SOLR type" tab
    if (tabName.equals(Messages.getString(locale, "SolrConnector.SolrType"))) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.SolrType2") + "</nobr></td>\n"
          + "    <td class=\"value\">\n" + "      <select name=\"solrtype\">\n" + "        <option value=\"" + SolrConfig.SOLR_TYPE_STANDARD + "\"" + (type.equals(SolrConfig.SOLR_TYPE_STANDARD) ? " selected=\"true\"" : "") + ">"
          + Messages.getBodyString(locale, "SolrConnector.SingleServer") + "</option>\n" + "        <option value=\"" + SolrConfig.SOLR_TYPE_SOLRCLOUD + "\"" + (type.equals(SolrConfig.SOLR_TYPE_SOLRCLOUD) ? " selected=\"true\"" : "") + ">"
          + Messages.getBodyString(locale, "SolrConnector.SolrCloud") + "</option>\n" + "      </select>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      // Type tab hiddens
      out.print("<input type=\"hidden\" name=\"solrtype\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(type) + "\"/>\n");
    }

    // "Server" tab
    // Always pass the whole keystore as a hidden.
    if (solrKeystore != null) {
      out.print("<input type=\"hidden\" name=\"keystoredata\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(solrKeystore) + "\"/>\n");
    }
    out.print("<input name=\"configop\" type=\"hidden\" value=\"Continue\"/>\n");

    if (tabName.equals(Messages.getString(locale, "SolrConnector.Server"))) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Protocol") + "</nobr></td>\n"
          + "    <td class=\"value\">\n" + "      <select name=\"serverprotocol\">\n" + "        <option value=\"" + SolrConfig.PROTOCOL_TYPE_HTTP + "\"" + (protocol.equals(SolrConfig.PROTOCOL_TYPE_HTTP) ? " selected=\"true\"" : "")
          + ">http</option>\n" + "        <option value=\"" + SolrConfig.PROTOCOL_TYPE_HTTPS + "\"" + (protocol.equals(SolrConfig.PROTOCOL_TYPE_HTTPS) ? " selected=\"true\"" : "") + ">https</option>\n" + "      </select>\n" + "    </td>\n"
          + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ServerName") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"servername\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(server) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
          + Messages.getBodyString(locale, "SolrConnector.Port") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"serverport\" type=\"text\" size=\"5\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(port)
          + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.WebApplicationName")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"webappname\" type=\"text\" size=\"16\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(webapp) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n"
          + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.CoreName") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"core\" type=\"text\" size=\"16\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(core) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
          + Messages.getBodyString(locale, "SolrConnector.ConnectionTimeout") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"connectiontimeout\" type=\"text\" size=\"5\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(connectionTimeout) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.SocketTimeout")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"sockettimeout\" type=\"text\" size=\"5\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(socketTimeout) + "\"/>\n" + "    </td>\n" + "  </tr>\n"
          + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Realm") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"realm\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(realm) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
          + Messages.getBodyString(locale, "SolrConnector.UserID") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"userid\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(userID)
          + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Password") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input type=\"password\" size=\"32\" name=\"password\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(password) + "\"/>\n" + "    </td>\n" + "  </tr>\n"
          + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.SSLTrustCertificateList") + "</nobr></td>\n"
          + "    <td class=\"value\">\n" + "      <input type=\"hidden\" name=\"solrkeystorealias\" value=\"\"/>\n" + "      <table class=\"displaytable\">\n");
      // List the individual certificates in the store, with a delete button for each
      final String[] contents = localKeystore.getContents();
      if (contents.length == 0) {
        out.print("        <tr><td class=\"message\" colspan=\"2\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.NoCertificatesPresent") + "</nobr></td></tr>\n");
      } else {
        int i = 0;
        while (i < contents.length) {
          final String alias = contents[i];
          String description = localKeystore.getDescription(alias);
          if (description.length() > 128)
            description = description.substring(0, 125) + "...";
          out.print("        <tr>\n" + "          <td class=\"value\"><input type=\"button\" onclick='Javascript:SolrDeleteCertificate(\"" + org.apache.manifoldcf.ui.util.Encoder.attributeJavascriptEscape(alias) + "\")' alt=\""
              + Messages.getAttributeString(locale, "SolrConnector.DeleteCert") + " " + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(alias) + "\" value=\"Delete\"/></td>\n" + "          <td>"
              + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(description) + "</td>\n" + "        </tr>\n");
          i++;
        }
      }
      out.print(
          "      </table>\n" + "      <input type=\"button\" onclick='Javascript:SolrAddCertificate()' alt=\"" + Messages.getAttributeString(locale, "SolrConnector.AddCert") + "\" value=\"" + Messages.getAttributeString(locale, "SolrConnector.Add")
              + "\"/>&nbsp;\n" + "      " + Messages.getBodyString(locale, "SolrConnector.Certificate") + "&nbsp;<input name=\"solrcertificate\" size=\"50\" type=\"file\"/>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      // Server tab hiddens
      out.print("<input type=\"hidden\" name=\"serverprotocol\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(protocol) + "\"/>\n" + "<input type=\"hidden\" name=\"servername\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(server) + "\"/>\n" + "<input type=\"hidden\" name=\"serverport\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(port) + "\"/>\n"
          + "<input type=\"hidden\" name=\"webappname\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(webapp) + "\"/>\n" + "<input type=\"hidden\" name=\"core\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(core) + "\"/>\n" + "<input type=\"hidden\" name=\"connectiontimeout\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(connectionTimeout) + "\"/>\n"
          + "<input type=\"hidden\" name=\"sockettimeout\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(socketTimeout) + "\"/>\n" + "<input type=\"hidden\" name=\"realm\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(realm) + "\"/>\n" + "<input type=\"hidden\" name=\"userid\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(userID) + "\"/>\n"
          + "<input type=\"hidden\" name=\"password\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(password) + "\"/>\n");
    }

    // "Zookeeper" tab
    if (tabName.equals(Messages.getString(locale, "SolrConnector.Zookeeper"))) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ZookeeperHosts")
          + "</nobr></td>\n" + "    <td class=\"boxcell\">\n" + "      <table class=\"formtable\">\n" + "        <tr class=\"formheaderrow\">\n" + "          <td class=\"formcolumnheader\"></td>\n" + "          <td class=\"formcolumnheader\"><nobr>"
          + Messages.getBodyString(locale, "SolrConnector.Host") + "</nobr></td>\n" + "          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Port") + "</nobr></td>\n" + "        </tr>\n");

      // Loop through the existing zookeeper nodes
      int k = 0;
      for (int i = 0; i < parameters.getChildCount(); i++) {
        final ConfigurationNode cn = parameters.getChild(i);
        if (cn.getType().equals(SolrConfig.NODE_ZOOKEEPER)) {
          final String host = cn.getAttributeValue(SolrConfig.ATTR_HOST);
          final String zkport = cn.getAttributeValue(SolrConfig.ATTR_PORT);
          final String postfix = "zookeeper_" + k;
          out.print("        <tr class=\"" + (((k % 2) == 0) ? "evenformrow" : "oddformrow") + "\">\n" + "          <td class=\"formcolumncell\">\n" + "            <a name=\"" + postfix + "\">\n" + "              <input type=\"button\" value=\""
              + Messages.getAttributeString(locale, "SolrConnector.Delete") + "\" alt=\"" + Messages.getAttributeString(locale, "SolrConnector.DeleteZookeeperHost") + Integer.toString(k + 1) + "\" onclick='javascript:deleteZookeeperHost("
              + Integer.toString(k) + ");'/>\n" + "              <input type=\"hidden\" name=\"" + "op_" + postfix + "\" value=\"Continue\"/>\n" + "              <input type=\"hidden\" name=\"" + "host_" + postfix + "\" value=\""
              + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(host) + "\"/>\n" + "              <input type=\"hidden\" name=\"" + "port_" + postfix + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(zkport) + "\"/>\n"
              + "            </a>\n" + "          </td>\n" + "          <td class=\"formcolumncell\">\n" + "            <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(host) + "</nobr>\n" + "          </td>\n"
              + "          <td class=\"formcolumncell\">\n" + "            <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(zkport) + "</nobr>\n" + "          </td>\n" + "        </tr>\n");
          k++;
        }
      }
      // If this looks like the first time through for this connection, add a default zookeeper setup.
      // Only works because after the first post, parameters always will have children.
      if (parameters.getChildCount() == 0) {
        final String postfix = "zookeeper_" + k;
        out.print("        <tr class=\"" + (((k % 2) == 0) ? "evenformrow" : "oddformrow") + "\">\n" + "          <td class=\"formcolumncell\">\n" + "            <a name=\"" + postfix + "\">\n" + "              <input type=\"button\" value=\""
            + Messages.getAttributeString(locale, "SolrConnector.Delete") + "\" alt=\"" + Messages.getAttributeString(locale, "SolrConnector.DeleteZookeeperHost") + Integer.toString(k + 1) + "\" onclick='javascript:deleteZookeeperHost("
            + Integer.toString(k) + ");'/>\n" + "              <input type=\"hidden\" name=\"" + "op_" + postfix + "\" value=\"Continue\"/>\n" + "              <input type=\"hidden\" name=\"" + "host_" + postfix + "\" value=\"localhost\"/>\n"
            + "              <input type=\"hidden\" name=\"" + "port_" + postfix + "\" value=\"2181\"/>\n" + "            </a>\n" + "          </td>\n" + "          <td class=\"formcolumncell\">\n" + "            <nobr>localhost</nobr>\n"
            + "          </td>\n" + "          <td class=\"formcolumncell\">\n" + "            <nobr>2181</nobr>\n" + "          </td>\n" + "        </tr>\n");
        k++;
      }
      if (k == 0) {
        out.print("        <tr class=\"formrow\"><td class=\"formmessage\" colspan=\"3\">" + Messages.getBodyString(locale, "SolrConnector.NoZookeeperHostsSpecified") + "</td></tr>\n");
      }
      out.print("        <tr class=\"formrow\"><td class=\"formseparator\" colspan=\"3\"><hr/></td></tr>\n" + "        <tr class=\"formrow\">\n" + "          <td class=\"formcolumncell\">\n" + "            <a name=\"zookeeper\">\n"
          + "              <input type=\"button\" value=\"" + Messages.getAttributeString(locale, "SolrConnector.Add") + "\" alt=\"" + Messages.getAttributeString(locale, "SolrConnector.AddZookeeperHost")
          + "\" onclick=\"javascript:addZookeeperHost();\"/>\n" + "            </a>\n" + "            <input type=\"hidden\" name=\"count_zookeeper\" value=\"" + k + "\"/>\n"
          + "            <input type=\"hidden\" name=\"op_zookeeper\" value=\"Continue\"/>\n" + "          </td>\n" + "          <td class=\"formcolumncell\">\n"
          + "            <nobr><input type=\"text\" size=\"30\" name=\"host_zookeeper\" value=\"\"/></nobr>\n" + "          </td>\n" + "          <td class=\"formcolumncell\">\n"
          + "            <nobr><input type=\"text\" size=\"5\" name=\"port_zookeeper\" value=\"\"/></nobr>\n" + "          </td>\n" + "        </tr>\n" + "      </table>\n" + "    </td>\n" + "  </tr>\n"
          + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ZnodePath") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"znodepath\" type=\"text\" size=\"16\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(znodePath) + "\"/>\n" + "    </td>\n" + "  </tr>\n"
          + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.CollectionName") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"collection\" type=\"text\" size=\"16\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(collection) + "\"/>\n" + "    </td>\n" + "  </tr>\n"
          + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ZookeeperSocketTimeout") + "</nobr></td>\n"
          + "    <td class=\"value\">\n" + "      <input name=\"zkclienttimeout\" type=\"text\" size=\"5\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(zkSocketTimeout) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n"
          + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ZookeeperConnectionTimeout") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"zkconnecttimeout\" type=\"text\" size=\"5\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(zkConnectionTimeout) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      // Hiddens for Zookeeper tab
      int k = 0;
      for (int i = 0; i < parameters.getChildCount(); i++) {
        final ConfigurationNode cn = parameters.getChild(i);
        if (cn.getType().equals(SolrConfig.NODE_ZOOKEEPER)) {
          final String host = cn.getAttributeValue(SolrConfig.ATTR_HOST);
          final String zkport = cn.getAttributeValue(SolrConfig.ATTR_PORT);
          final String postfix = "zookeeper_" + k;
          out.print("<input type=\"hidden\" name=\"host_" + postfix + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(host) + "\"/>\n" + "<input type=\"hidden\" name=\"port_" + postfix + "\" value=\""
              + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(zkport) + "\"/>\n");
          k++;
        }
      }
      if (parameters.getChildCount() == 0) {
        final String postfix = "zookeeper_" + k;
        out.print("<input type=\"hidden\" name=\"host_" + postfix + "\" value=\"localhost\"/>\n" + "<input type=\"hidden\" name=\"port_" + postfix + "\" value=\"2181\"/>\n");
        k++;
      }
      out.print("<input type=\"hidden\" name=\"count_zookeeper\" value=\"" + k + "\"/>\n" + "<input type=\"hidden\" name=\"znodepath\" value=\"" + znodePath + "\"/>\n" + "<input type=\"hidden\" name=\"collection\" value=\"" + collection + "\"/>\n"
          + "<input type=\"hidden\" name=\"zkclienttimeout\" value=\"" + zkSocketTimeout + "\"/>\n" + "<input type=\"hidden\" name=\"zkconnecttimeout\" value=\"" + zkConnectionTimeout + "\"/>\n");
    }

    // "Paths" tab
    if (tabName.equals(Messages.getString(locale, "SolrConnector.Paths"))) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.UpdateHandler")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"updatepath\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(updatePath) + "\"/>\n" + "    </td>\n" + "  </tr>\n"
          + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.RemoveHandler") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"removepath\" type=\"text\" size=\"32\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(removePath) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.StatusHandler")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"statuspath\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(statusPath) + "\"/>\n" + "    </td>\n" + "  </tr>\n"
          + "</table>\n");
    } else {
      // Paths tab hiddens
      out.print("<input type=\"hidden\" name=\"updatepath\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(updatePath) + "\"/>\n" + "<input type=\"hidden\" name=\"removepath\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(removePath) + "\"/>\n" + "<input type=\"hidden\" name=\"statuspath\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(statusPath) + "\"/>\n");
    }

    // "Schema" tab
    if (tabName.equals(Messages.getString(locale, "SolrConnector.Schema"))) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.IDFieldName")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"idfield\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(idField) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n"
          + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.OriginalSizeFieldName") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"originalsizefield\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(originalSizeField) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n"
          + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ModifiedDateFieldName") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"modifieddatefield\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(modifiedDateField) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n"
          + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.CreatedDateFieldName") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"createddatefield\" type=\"text\" size=\"32\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(createdDateField) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.IndexedDateFieldName")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"indexeddatefield\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(indexedDateField) + "\"/>\n" + "    </td>\n"
          + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.FileNameFieldName") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"filenamefield\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(fileNameField) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
          + Messages.getBodyString(locale, "SolrConnector.MimeTypeFieldName") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"mimetypefield\" type=\"text\" size=\"32\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(mimeTypeField) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.UseExtractUpdateHandler")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"extractupdatecheckbox\" type=\"hidden\" value=\"true\"/>\n" + "      <input name=\"extractupdatepresent\" type=\"hidden\" value=\"true\"/>\n");
      if (!useExtractUpdate.equals("false")) {
        out.print("      <input name=\"extractupdate\" type=\"checkbox\" value=\"true\" checked=\"true\"/>\n");
      } else {
        out.print("      <input name=\"extractupdate\" type=\"checkbox\" value=\"true\"/>\n");
      }
      out.print("    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ContentFieldName") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <input name=\"contentfield\" type=\"text\" size=\"32\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(contentField) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      out.print("<input type=\"hidden\" name=\"idfield\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(idField) + "\"/>\n" + "<input type=\"hidden\" name=\"originalsizefield\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(originalSizeField) + "\"/>\n" + "<input type=\"hidden\" name=\"modifieddatefield\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(modifiedDateField) + "\"/>\n"
          + "<input type=\"hidden\" name=\"createddatefield\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(createdDateField) + "\"/>\n" + "<input type=\"hidden\" name=\"indexeddatefield\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(indexedDateField) + "\"/>\n" + "<input type=\"hidden\" name=\"filenamefield\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(fileNameField) + "\"/>\n"
          + "<input type=\"hidden\" name=\"mimetypefield\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(mimeTypeField) + "\"/>\n" + "<input type=\"hidden\" name=\"contentfield\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(contentField) + "\"/>\n" + "<input name=\"extractupdatecheckbox\" type=\"hidden\" value=\"false\"/>\n" + "<input type=\"hidden\" name=\"extractupdate\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(useExtractUpdate) + "\"/>\n" + "<input name=\"extractupdatepresent\" type=\"hidden\" value=\"true\"/>\n");
    }

    // "Documents" tab
    if (tabName.equals(Messages.getString(locale, "SolrConnector.Documents"))) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.MaximumDocumentLength")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"maxdocumentlength\" type=\"text\" size=\"16\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(maxLength) + "\"/>\n" + "    </td>\n" + "  </tr>\n"
          + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.IncludedMimeTypes") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <textarea rows=\"10\" cols=\"20\" name=\"includedmimetypes\">" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(includedMimeTypes) + "</textarea>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n"
          + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ExcludedMimeTypes") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <textarea rows=\"10\" cols=\"20\" name=\"excludedmimetypes\">"
          + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(excludedMimeTypes) + "</textarea>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      out.print("<input type=\"hidden\" name=\"maxdocumentlength\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(maxLength) + "\"/>\n" + "<input type=\"hidden\" name=\"includedmimetypes\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(includedMimeTypes) + "\"/>\n" + "<input type=\"hidden\" name=\"excludedmimetypes\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(excludedMimeTypes) + "\"/>\n");
    }

    // "Commits" tab
    if (tabName.equals(Messages.getString(locale, "SolrConnector.Commits"))) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td colspan=\"2\" class=\"separator\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.CommitAtEndOfEveryJob")
          + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <input name=\"commits_present\" type=\"hidden\" value=\"true\"/>\n" + "      <input name=\"commits\" type=\"checkbox\" value=\"true\""
          + (commits.equals("true") ? " checked=\"yes\"" : "") + "/>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.CommitEachDocumentWithin") + "</nobr></td>\n"
          + "    <td class=\"value\">\n" + "      <input name=\"commitwithin\" type=\"text\" size=\"16\" value=\"" + commitWithin + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      out.print("<input type=\"hidden\" name=\"commits_present\" value=\"true\"/>\n" + "<input name=\"commits\" type=\"hidden\" value=\"" + commits + "\"/>\n" + "<input name=\"commitwithin\" type=\"hidden\" value=\"" + commitWithin + "\"/>\n");
    }

    // Prepare for the argument tab
    final Map argumentMap = new HashMap();
    int i = 0;
    while (i < parameters.getChildCount()) {
      final ConfigNode sn = parameters.getChild(i++);
      if (sn.getType().equals(SolrConfig.NODE_ARGUMENT)) {
        final String name = sn.getAttributeValue(SolrConfig.ATTRIBUTE_NAME);
        final String value = sn.getAttributeValue(SolrConfig.ATTRIBUTE_VALUE);
        ArrayList values = (ArrayList) argumentMap.get(name);
        if (values == null) {
          values = new ArrayList();
          argumentMap.put(name, values);
        }
        values.add(value);
      }
    }

    // "Arguments" tab
    if (tabName.equals(Messages.getString(locale, "SolrConnector.Arguments"))) {
      // For the display, sort the arguments into alphabetic order
      final String[] sortArray = new String[argumentMap.size()];
      i = 0;
      final Iterator iter = argumentMap.keySet().iterator();
      while (iter.hasNext()) {
        sortArray[i++] = (String) iter.next();
      }
      java.util.Arrays.sort(sortArray);
      out.print("<table class=\"displaytable\">\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Arguments2") + "</nobr></td>\n"
          + "    <td class=\"boxcell\">\n" + "      <table class=\"formtable\">\n" + "        <tr class=\"formheaderrow\">\n" + "          <td class=\"formcolumnheader\"></td>\n" + "          <td class=\"formcolumnheader\"><nobr>"
          + Messages.getBodyString(locale, "SolrConnector.Name") + "</nobr></td>\n" + "          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Value") + "</nobr></td>\n" + "        </tr>\n");
      i = 0;
      int k = 0;
      while (k < sortArray.length) {
        final String name = sortArray[k++];
        final ArrayList values = (ArrayList) argumentMap.get(name);
        int j = 0;
        while (j < values.size()) {
          final String value = (String) values.get(j++);
          // Its prefix will be...
          final String prefix = "argument_" + Integer.toString(i);
          out.print("        <tr class=\"" + (((i % 2) == 0) ? "evenformrow" : "oddformrow") + "\">\n" + "          <td class=\"formcolumncell\">\n" + "            <a name=\"" + prefix + "\"><input type=\"button\" value=\"Delete\" alt=\""
              + Messages.getAttributeString(locale, "SolrConnector.DeleteArgument") + " " + Integer.toString(i + 1) + "\" onclick=\"javascript:deleteArgument(" + Integer.toString(i) + ");" + "\"/>\n" + "              <input type=\"hidden\" name=\""
              + prefix + "_op" + "\" value=\"Continue\"/>\n" + "              <input type=\"hidden\" name=\"" + prefix + "_name" + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(name) + "\"/>\n" + "            </a>\n"
              + "          </td>\n" + "          <td class=\"formcolumncell\">\n" + "            <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(name) + "</nobr>\n" + "          </td>\n" + "          <td class=\"formcolumncell\">\n"
              + "            <nobr><input type=\"text\" size=\"30\" name=\"" + prefix + "_value" + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(value) + "\"</nobr>\n" + "          </td>\n" + "        </tr>\n");
          i++;
        }
      }
      if (i == 0) {
        out.print("        <tr class=\"formrow\"><td class=\"formmessage\" colspan=\"3\">" + Messages.getBodyString(locale, "SolrConnector.NoArgumentsSpecified") + "</td></tr>\n");
      }
      out.print("        <tr class=\"formrow\"><td class=\"formseparator\" colspan=\"3\"><hr/></td></tr>\n" + "        <tr class=\"formrow\">\n" + "          <td class=\"formcolumncell\">\n"
          + "            <a name=\"argument\"><input type=\"button\" value=\"Add\" alt=\"Add argument\" onclick=\"javascript:addArgument();\"/>\n" + "              <input type=\"hidden\" name=\"argument_count\" value=\"" + Integer.toString(i)
          + "\"/>\n" + "              <input type=\"hidden\" name=\"argument_op\" value=\"Continue\"/>\n" + "            </a>\n" + "          </td>\n" + "          <td class=\"formcolumncell\">\n"
          + "            <nobr><input type=\"text\" size=\"30\" name=\"argument_name\" value=\"\"/></nobr>\n" + "          </td>\n" + "          <td class=\"formcolumncell\">\n"
          + "            <nobr><input type=\"text\" size=\"30\" name=\"argument_value\" value=\"\"/></nobr>\n" + "          </td>\n" + "        </tr>\n" + "      </table>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      // Emit hiddens for argument tab
      i = 0;
      final Iterator iter = argumentMap.keySet().iterator();
      while (iter.hasNext()) {
        final String name = (String) iter.next();
        final ArrayList values = (ArrayList) argumentMap.get(name);
        int j = 0;
        while (j < values.size()) {
          final String value = (String) values.get(j++);
          // It's prefix will be...
          final String prefix = "argument_" + Integer.toString(i++);
          out.print("<input type=\"hidden\" name=\"" + prefix + "_name\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(name) + "\"/>\n" + "<input type=\"hidden\" name=\"" + prefix + "_value\" value=\""
              + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(value) + "\"/>\n");
        }
      }
      out.print("<input type=\"hidden\" name=\"argument_count\" value=\"" + Integer.toString(i) + "\"/>\n");
    }
  }

  /**
   * Process a configuration post. This method is called at the start of the connector's configuration page, whenever there is a possibility that form data for a connection has been posted. Its
   * purpose is to gather form information and modify the configuration parameters accordingly. The name of the posted form is "editconnection".
   *
   * @param threadContext
   *          is the local thread context.
   * @param variableContext
   *          is the set of variables available from the post, including binary file post information.
   * @param parameters
   *          are the configuration parameters, as they currently exist, for this connection being configured.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a redirection to an error page).
   */
  @Override
  public String processConfigurationPost(final IThreadContext threadContext, final IPostParameters variableContext, final Locale locale, final ConfigParams parameters) throws ManifoldCFException {
    final String type = variableContext.getParameter("solrtype");
    if (type != null)
      parameters.setParameter(SolrConfig.PARAM_SOLR_TYPE, type);

    final String protocol = variableContext.getParameter("serverprotocol");
    if (protocol != null)
      parameters.setParameter(SolrConfig.PARAM_PROTOCOL, protocol);

    final String server = variableContext.getParameter("servername");
    if (server != null)
      parameters.setParameter(SolrConfig.PARAM_SERVER, server);

    final String port = variableContext.getParameter("serverport");
    if (port != null)
      parameters.setParameter(SolrConfig.PARAM_PORT, port);

    final String webapp = variableContext.getParameter("webappname");
    if (webapp != null)
      parameters.setParameter(SolrConfig.PARAM_WEBAPPNAME, webapp);

    final String core = variableContext.getParameter("core");
    if (core != null)
      parameters.setParameter(SolrConfig.PARAM_CORE, core);

    final String collection = variableContext.getParameter("collection");
    if (collection != null)
      parameters.setParameter(SolrConfig.PARAM_COLLECTION, collection);

    final String connectionTimeout = variableContext.getParameter("connectiontimeout");
    if (connectionTimeout != null)
      parameters.setParameter(SolrConfig.PARAM_CONNECTION_TIMEOUT, connectionTimeout);

    final String socketTimeout = variableContext.getParameter("sockettimeout");
    if (socketTimeout != null)
      parameters.setParameter(SolrConfig.PARAM_SOCKET_TIMEOUT, socketTimeout);

    final String znodePath = variableContext.getParameter("znodepath");
    if (znodePath != null)
      parameters.setParameter(SolrConfig.PARAM_ZOOKEEPER_ZNODE_PATH, znodePath);

    final String zkClientTimeout = variableContext.getParameter("zkclienttimeout");
    if (zkClientTimeout != null)
      parameters.setParameter(SolrConfig.PARAM_ZOOKEEPER_SOCKET_TIMEOUT, zkClientTimeout);

    final String zkConnectTimeout = variableContext.getParameter("zkconnecttimeout");
    if (zkConnectTimeout != null)
      parameters.setParameter(SolrConfig.PARAM_ZOOKEEPER_CONNECTION_TIMEOUT, zkConnectTimeout);

    final String updatePath = variableContext.getParameter("updatepath");
    if (updatePath != null)
      parameters.setParameter(SolrConfig.PARAM_UPDATEPATH, updatePath);

    final String removePath = variableContext.getParameter("removepath");
    if (removePath != null)
      parameters.setParameter(SolrConfig.PARAM_REMOVEPATH, removePath);

    final String statusPath = variableContext.getParameter("statuspath");
    if (statusPath != null)
      parameters.setParameter(SolrConfig.PARAM_STATUSPATH, statusPath);

    final String idField = variableContext.getParameter("idfield");
    if (idField != null)
      parameters.setParameter(SolrConfig.PARAM_IDFIELD, idField);

    final String originalSizeField = variableContext.getParameter("originalsizefield");
    if (originalSizeField != null)
      parameters.setParameter(SolrConfig.PARAM_ORIGINALSIZEFIELD, originalSizeField);

    final String modifiedDateField = variableContext.getParameter("modifieddatefield");
    if (modifiedDateField != null)
      parameters.setParameter(SolrConfig.PARAM_MODIFIEDDATEFIELD, modifiedDateField);

    final String createdDateField = variableContext.getParameter("createddatefield");
    if (createdDateField != null)
      parameters.setParameter(SolrConfig.PARAM_CREATEDDATEFIELD, createdDateField);

    final String indexedDateField = variableContext.getParameter("indexeddatefield");
    if (indexedDateField != null)
      parameters.setParameter(SolrConfig.PARAM_INDEXEDDATEFIELD, indexedDateField);

    final String fileNameField = variableContext.getParameter("filenamefield");
    if (fileNameField != null)
      parameters.setParameter(SolrConfig.PARAM_FILENAMEFIELD, fileNameField);

    final String mimeTypeField = variableContext.getParameter("mimetypefield");
    if (mimeTypeField != null)
      parameters.setParameter(SolrConfig.PARAM_MIMETYPEFIELD, mimeTypeField);

    final String contentField = variableContext.getParameter("contentfield");
    if (contentField != null)
      parameters.setParameter(SolrConfig.PARAM_CONTENTFIELD, contentField);

    final String extractUpdatePresent = variableContext.getParameter("extractupdatepresent");
    if (extractUpdatePresent != null) {
      String extractUpdate = variableContext.getParameter("extractupdate");
      if (extractUpdate == null || extractUpdate.length() == 0)
        extractUpdate = "false";
      parameters.setParameter(SolrConfig.PARAM_EXTRACTUPDATE, extractUpdate);
    }

    final String realm = variableContext.getParameter("realm");
    if (realm != null)
      parameters.setParameter(SolrConfig.PARAM_REALM, realm);

    final String userID = variableContext.getParameter("userid");
    if (userID != null)
      parameters.setParameter(SolrConfig.PARAM_USERID, userID);

    final String password = variableContext.getParameter("password");
    if (password != null)
      parameters.setObfuscatedParameter(SolrConfig.PARAM_PASSWORD, variableContext.mapKeyToPassword(password));

    final String maxLength = variableContext.getParameter("maxdocumentlength");
    if (maxLength != null)
      parameters.setParameter(SolrConfig.PARAM_MAXLENGTH, maxLength);

    final String includedMimeTypes = variableContext.getParameter("includedmimetypes");
    if (includedMimeTypes != null)
      parameters.setParameter(SolrConfig.PARAM_INCLUDEDMIMETYPES, includedMimeTypes);

    final String excludedMimeTypes = variableContext.getParameter("excludedmimetypes");
    if (excludedMimeTypes != null)
      parameters.setParameter(SolrConfig.PARAM_EXCLUDEDMIMETYPES, excludedMimeTypes);

    final String commitsPresent = variableContext.getParameter("commits_present");
    if (commitsPresent != null) {
      String commits = variableContext.getParameter("commits");
      if (commits == null)
        commits = "false";
      parameters.setParameter(SolrConfig.PARAM_COMMITS, commits);
    }

    final String commitWithin = variableContext.getParameter("commitwithin");
    if (commitWithin != null)
      parameters.setParameter(SolrConfig.PARAM_COMMITWITHIN, commitWithin);

    String keystoreValue = variableContext.getParameter("keystoredata");
    if (keystoreValue != null) {
      final IKeystoreManager mgr = KeystoreManagerFactory.make("", keystoreValue);
      parameters.setParameter(SolrConfig.PARAM_KEYSTORE, mgr.getString());
    }

    String x = variableContext.getParameter("count_zookeeper");
    if (x != null && x.length() > 0) {
      // About to gather the bandwidth nodes, so get rid of the old ones.
      int i = 0;
      while (i < parameters.getChildCount()) {
        final ConfigNode node = parameters.getChild(i);
        if (node.getType().equals(SolrConfig.NODE_ZOOKEEPER))
          parameters.removeChild(i);
        else
          i++;
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String postfix = "zookeeper_" + Integer.toString(i);
        final String op = variableContext.getParameter("op_" + postfix);
        if (op == null || !op.equals("Delete")) {
          // Gather the host etc.
          final String host = variableContext.getParameter("host_" + postfix);
          final String zkport = variableContext.getParameter("port_" + postfix);
          final ConfigNode node = new ConfigNode(SolrConfig.NODE_ZOOKEEPER);
          node.setAttribute(SolrConfig.ATTR_HOST, host);
          node.setAttribute(SolrConfig.ATTR_PORT, zkport);
          parameters.addChild(parameters.getChildCount(), node);
        }
        i++;
      }
      final String addop = variableContext.getParameter("op_zookeeper");
      if (addop != null && addop.equals("Add")) {
        final String host = variableContext.getParameter("host_zookeeper");
        final String zkport = variableContext.getParameter("port_zookeeper");
        final ConfigNode node = new ConfigNode(SolrConfig.NODE_ZOOKEEPER);
        node.setAttribute(SolrConfig.ATTR_HOST, host);
        node.setAttribute(SolrConfig.ATTR_PORT, zkport);
        parameters.addChild(parameters.getChildCount(), node);
      }
    }

    x = variableContext.getParameter("argument_count");
    if (x != null && x.length() > 0) {
      // About to gather the argument nodes, so get rid of the old ones.
      int i = 0;
      while (i < parameters.getChildCount()) {
        final ConfigNode node = parameters.getChild(i);
        if (node.getType().equals(SolrConfig.NODE_ARGUMENT))
          parameters.removeChild(i);
        else
          i++;
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = "argument_" + Integer.toString(i);
        final String op = variableContext.getParameter(prefix + "_op");
        if (op == null || !op.equals("Delete")) {
          // Gather the name and value.
          final String name = variableContext.getParameter(prefix + "_name");
          final String value = variableContext.getParameter(prefix + "_value");
          final ConfigNode node = new ConfigNode(SolrConfig.NODE_ARGUMENT);
          node.setAttribute(SolrConfig.ATTRIBUTE_NAME, name);
          node.setAttribute(SolrConfig.ATTRIBUTE_VALUE, value);
          parameters.addChild(parameters.getChildCount(), node);
        }
        i++;
      }
      final String addop = variableContext.getParameter("argument_op");
      if (addop != null && addop.equals("Add")) {
        final String name = variableContext.getParameter("argument_name");
        final String value = variableContext.getParameter("argument_value");
        final ConfigNode node = new ConfigNode(SolrConfig.NODE_ARGUMENT);
        node.setAttribute(SolrConfig.ATTRIBUTE_NAME, name);
        node.setAttribute(SolrConfig.ATTRIBUTE_VALUE, value);
        parameters.addChild(parameters.getChildCount(), node);
      }
    }

    final String configOp = variableContext.getParameter("configop");
    if (configOp != null) {
      IKeystoreManager mgr;
      if (configOp.equals("Delete")) {
        final String alias = variableContext.getParameter("solrkeystorealias");
        keystoreValue = parameters.getParameter(SolrConfig.PARAM_KEYSTORE);
        if (keystoreValue != null)
          mgr = KeystoreManagerFactory.make("", keystoreValue);
        else
          mgr = KeystoreManagerFactory.make("");
        mgr.remove(alias);
        parameters.setParameter(SolrConfig.PARAM_KEYSTORE, mgr.getString());
      } else if (configOp.equals("Add")) {
        final String alias = IDFactory.make(threadContext);
        final byte[] certificateValue = variableContext.getBinaryBytes("solrcertificate");
        keystoreValue = parameters.getParameter(SolrConfig.PARAM_KEYSTORE);
        if (keystoreValue != null)
          mgr = KeystoreManagerFactory.make("", keystoreValue);
        else
          mgr = KeystoreManagerFactory.make("");
        final java.io.InputStream is = new java.io.ByteArrayInputStream(certificateValue);
        String certError = null;
        try {
          mgr.importCertificate(alias, is);
        } catch (final Throwable e) {
          certError = e.getMessage();
        } finally {
          try {
            is.close();
          } catch (final IOException e) {
            // Eat this exception
          }
        }

        if (certError != null) {
          return "Illegal certificate: " + certError;
        }
        parameters.setParameter(SolrConfig.PARAM_KEYSTORE, mgr.getString());
      }
    }

    return null;
  }

  /**
   * View configuration. This method is called in the body section of the connector's view configuration page. Its purpose is to present the connection information to the user. The coder can presume
   * that the HTML that is output from this configuration will be within appropriate &lt;html&gt; and &lt;body&gt;tags.
   *
   * @param threadContext
   *          is the local thread context.
   * @param out
   *          is the output to which any HTML should be sent.
   * @param parameters
   *          are the configuration parameters, as they currently exist, for this connection being configured.
   */
  @Override
  public void viewConfiguration(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters) throws ManifoldCFException, IOException {
    out.print("<table class=\"displaytable\">\n" + "  <tr>\n" + "    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Parameters") + "</nobr></td>\n" + "    <td class=\"value\" colspan=\"3\">\n");
    final Iterator iter = parameters.listParameters();
    while (iter.hasNext()) {
      final String param = (String) iter.next();
      final String value = parameters.getParameter(param);
      if (param.length() >= "password".length() && param.substring(param.length() - "password".length()).equalsIgnoreCase("password")) {
        out.print("      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=********</nobr><br/>\n");
      } else if (param.length() >= "keystore".length() && param.substring(param.length() - "keystore".length()).equalsIgnoreCase("keystore")) {
        final IKeystoreManager kmanager = KeystoreManagerFactory.make("", value);
        out.print("      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=&lt;" + Integer.toString(kmanager.getContents().length) + " certificate(s)&gt;</nobr><br/>\n");
      } else {
        out.print("      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(value) + "</nobr><br/>\n");
      }
    }

    out.print("    </td>\n" + "  </tr>\n");

    out.print("\n");

    out.print("  <tr>\n" + "    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.ZookeeperHosts") + "</nobr></td>\n" + "    <td class=\"boxcell\" colspan=\"3\">\n" + "      <table class=\"formtable\">\n"
        + "        <tr class=\"formheaderrow\">\n" + "          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Host") + "</nobr></td>\n" + "          <td class=\"formcolumnheader\"><nobr>"
        + Messages.getBodyString(locale, "SolrConnector.Port") + "</nobr></td>\n" + "        </tr>\n");

    int instanceNumber = 0;
    for (int i = 0; i < parameters.getChildCount(); i++) {
      final ConfigNode cn = parameters.getChild(i);
      if (cn.getType().equals(SolrConfig.NODE_ZOOKEEPER)) {
        // An argument node! Look for all its parameters.
        final String host = cn.getAttributeValue(SolrConfig.ATTR_HOST);
        final String zkport = cn.getAttributeValue(SolrConfig.ATTR_PORT);

        out.print("        <tr class=\"" + (((instanceNumber % 2) == 0) ? "evenformrow" : "oddformrow") + "\">\n" + "          <td class=\"formcolumncell\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(host) + "</nobr></td>\n"
            + "          <td class=\"formcolumncell\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(zkport) + "</nobr></td>\n" + "        </tr>\n");

        instanceNumber++;
      }
    }
    if (instanceNumber == 0) {
      out.print("        <tr class=\"formrow\"><td class=\"formmessage\" colspan=\"5\">" + Messages.getBodyString(locale, "SolrConnector.NoZookeeperHostsSpecified") + "</td></tr>\n");
    }

    out.print("      </table>\n" + "    </td>\n" + "  </tr>\n");

    out.print("\n");

    out.print("  <tr>\n" + "    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Arguments3") + "</nobr></td>\n" + "    <td class=\"boxcell\" colspan=\"3\">\n" + "      <table class=\"formtable\">\n"
        + "        <tr class=\"formheaderrow\">\n" + "          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale, "SolrConnector.Name") + "</nobr></td>\n" + "          <td class=\"formcolumnheader\"><nobr>"
        + Messages.getBodyString(locale, "SolrConnector.Value") + "</nobr></td>\n" + "        </tr>\n");

    instanceNumber = 0;
    for (int i = 0; i < parameters.getChildCount(); i++) {
      final ConfigNode cn = parameters.getChild(i);
      if (cn.getType().equals(SolrConfig.NODE_ARGUMENT)) {
        // An argument node! Look for all its parameters.
        final String name = cn.getAttributeValue(SolrConfig.ATTRIBUTE_NAME);
        final String value = cn.getAttributeValue(SolrConfig.ATTRIBUTE_VALUE);

        out.print("        <tr class=\"" + (((instanceNumber % 2) == 0) ? "evenformrow" : "oddformrow") + "\">\n" + "          <td class=\"formcolumncell\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(name) + "</nobr></td>\n"
            + "          <td class=\"formcolumncell\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(value) + "</nobr></td>\n" + "        </tr>\n");

        instanceNumber++;
      }
    }
    if (instanceNumber == 0) {
      out.print("        <tr class=\"formrow\"><td class=\"formmessage\" colspan=\"5\">" + Messages.getBodyString(locale, "SolrConnector.NoArguments") + "</td></tr>\n");
    }

    out.print("      </table>\n" + "    </td>\n" + "  </tr>\n");

    out.print("</table>\n");
  }

  /**
   * This class handles Solr connector version string packing/unpacking/interpretation.
   */
  protected class SpecPacker {

    /** Arguments, from configuration */
    private final Map<String, List<String>> args = new HashMap<>();

    public SpecPacker(final Specification spec) {

      // Process arguments
      for (int i = 0; i < params.getChildCount(); i++) {
        final ConfigNode node = params.getChild(i);
        if (node.getType().equals(SolrConfig.NODE_ARGUMENT)) {
          final String attrName = node.getAttributeValue(SolrConfig.ATTRIBUTE_NAME);
          List<String> list = args.get(attrName);
          if (list == null) {
            list = new ArrayList<>();
            args.put(attrName, list);
          }
          list.add(node.getAttributeValue(SolrConfig.ATTRIBUTE_VALUE));
        }
      }

    }

    public String toPackedString() {
      final StringBuilder sb = new StringBuilder();
      final String[] sortArray = new String[args.size()];
      final Iterator<String> iter = args.keySet().iterator();
      int i = 0;
      while (iter.hasNext()) {
        sortArray[i++] = iter.next();
      }

      // Always use sorted order, because we need this to be comparable.
      java.util.Arrays.sort(sortArray);

      final String[] fixedList = new String[2];
      final List<String> nameValues = new ArrayList<>();
      for (int k = 0; k < sortArray.length; k++) {
        final String name = sortArray[k];
        final List<String> values = args.get(name);
        java.util.Collections.sort(values);
        for (final String value : values) {
          fixedList[0] = name;
          fixedList[1] = value;
          final StringBuilder pairBuffer = new StringBuilder();
          packFixedList(pairBuffer, fixedList, '=');
          nameValues.add(pairBuffer.toString());
        }
      }

      packList(sb, nameValues, '+');

      // Here, append things which we have no intention of unpacking. This includes stuff that comes from
      // the configuration information, for instance.

      if (idAttributeName != null) {
        sb.append('+');
        pack(sb, idAttributeName, '+');
      } else
        sb.append('-');

      if (originalSizeAttributeName != null) {
        sb.append('+');
        pack(sb, originalSizeAttributeName, '+');
      } else
        sb.append('-');

      if (modifiedDateAttributeName != null) {
        sb.append('+');
        pack(sb, modifiedDateAttributeName, '+');
      } else
        sb.append('-');

      if (createdDateAttributeName != null) {
        sb.append('+');
        pack(sb, createdDateAttributeName, '+');
      } else
        sb.append('-');

      if (indexedDateAttributeName != null) {
        sb.append('+');
        pack(sb, indexedDateAttributeName, '+');
      } else
        sb.append('-');

      if (fileNameAttributeName != null) {
        sb.append('+');
        pack(sb, fileNameAttributeName, '+');
      } else
        sb.append('-');

      if (mimeTypeAttributeName != null) {
        sb.append('+');
        pack(sb, mimeTypeAttributeName, '+');
      } else
        sb.append('-');

      if (contentAttributeName != null) {
        sb.append('+');
        pack(sb, contentAttributeName, '+');
      } else
        sb.append('-');

      if (useExtractUpdateHandler)
        sb.append('+');
      else
        sb.append('-');

      // Length limitation. We pack this because when it is changed we want to be sure we get any previously excluded documents.
      if (maxDocumentLength != null) {
        sb.append('+');
        pack(sb, maxDocumentLength.toString(), '+');
      } else
        sb.append('-');
      // Included mime types
      if (includedMimeTypesString != null) {
        sb.append('+');
        pack(sb, includedMimeTypesString, '+');
      } else
        sb.append('-');
      // Excluded mime types
      if (excludedMimeTypesString != null) {
        sb.append('+');
        pack(sb, excludedMimeTypesString, '+');
      } else
        sb.append('-');

      if (collectionName != null) {
        sb.append('+');
        pack(sb, collectionName, '+');
      } else
        sb.append('-');

      return sb.toString();
    }

    public Map<String, List<String>> getArgs() {
      return args;
    }

  }

  private String normalizeTitle (String titleToNormalize){
    if (titleToNormalize == null){
      return titleToNormalize;
    }

    String title = titleToNormalize.replaceAll("[^a-zA-Z0-9 -]", "");
    return truncate(title);
  }

  private String truncate(String toTruncate){
    if (toTruncate != null && toTruncate.length() > 100) {
      toTruncate = toTruncate.substring(0, 100);
    }
    return toTruncate;
  }

  /**
   * Use this method to log {@link RepositoryDocument} object content.
   *
   * @param document the current {@link RepositoryDocument} that is used to send the update request to Solr.
   * @param docID the MCF document ID. This ID is given by the Repository Connector that creates the RepositoryDocument.
   */
  //TODO refactor this method so that it can be used in any MCF connectors.
  private void logRepositoryDocument(RepositoryDocument document, String docID){
    StringBuilder toLog = new StringBuilder("\n!! RepositoryDocument content ------------------------------------- !!\nfilename = ");
    final String filename = document.getFileName();
    toLog.append(filename).append("\n");
    toLog.append("Document identifier = ").append(docID).append("\n");
    toLog.append("Getters:\n");
    toLog.append("getCreatedDate = ").append(document.getCreatedDate()).append("\n");
    toLog.append("getModifiedDate = ").append(document.getModifiedDate()).append("\n");
    toLog.append("getIndexingDate = ").append(document.getIndexingDate()).append("\n");
    toLog.append("getMimeType = ").append(document.getMimeType()).append("\n");
    toLog.append("getModifiedDate = ").append(document.getModifiedDate()).append("\n");
    toLog.append("getOriginalSize = ").append(document.getOriginalSize()).append("\n");
    logList(toLog, "getRootPath", document.getRootPath().toArray());
    logList(toLog, "getSourcePath", document.getSourcePath().toArray());
    toLog.append("Security Success:\n");
    logList(toLog, "getSecurityACL [SECURITY_TYPE_DIRECTORY_LEVEL]", document.getSecurityACL(RepositoryDocument.SECURITY_TYPE_DIRECTORY_LEVEL));
    logList(toLog, "getSecurityACL [SECURITY_TYPE_DOCUMENT]", document.getSecurityACL(RepositoryDocument.SECURITY_TYPE_DOCUMENT));
    logList(toLog, "getSecurityACL [SECURITY_TYPE_SHARE]", document.getSecurityACL(RepositoryDocument.SECURITY_TYPE_SHARE));
    logList(toLog, "getSecurityACL [SECURITY_TYPE_PARENT]", document.getSecurityACL(RepositoryDocument.SECURITY_TYPE_PARENT));
    toLog.append("Security Deny:\n");
    logList(toLog, "getSecurityDenyACL [SECURITY_TYPE_DIRECTORY_LEVEL]", document.getSecurityDenyACL(RepositoryDocument.SECURITY_TYPE_DIRECTORY_LEVEL));
    logList(toLog, "getSecurityDenyACL [SECURITY_TYPE_DOCUMENT]", document.getSecurityDenyACL(RepositoryDocument.SECURITY_TYPE_DOCUMENT));
    logList(toLog, "getSecurityDenyACL [SECURITY_TYPE_SHARE]", document.getSecurityDenyACL(RepositoryDocument.SECURITY_TYPE_SHARE));
    logList(toLog, "getSecurityDenyACL [SECURITY_TYPE_PARENT]", document.getSecurityDenyACL(RepositoryDocument.SECURITY_TYPE_PARENT));


    // Other Fields not fixed
    toLog.append("\nFields:\n");
    Iterator<String> itFields = document.getFields();
    String fieldName;
    Object[] fieldValues;
    while (itFields.hasNext()) {
      fieldName = itFields.next();
      fieldValues = document.getField(fieldName);
      logList(toLog, fieldName, fieldValues);
    }


    // Specific treatment for content
    // Read content : create a copy before reading for connector after, otherwise the content can't be read for ingestion.
    toLog.append("\nDocument BinaryStream (content field) = \n");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int)document.getBinaryLength());
    // Transfert document content to the storage
    long binaryLength = 0;
    try {
      binaryLength = document.getBinaryStream().transferTo(outputStream);
    } catch (IOException e) {
      Logging.connectors.error(e.getMessage(), e);
    }

    // The input stream of the document has been totally read by previous instruction, so set a new one
    document.setBinary(new ByteArrayInputStream(outputStream.toByteArray()), binaryLength);

    try (Scanner scanner = new Scanner(new ByteArrayInputStream(outputStream.toByteArray()))) {
      String line;
      while (scanner.hasNext()){
        line = scanner.nextLine();
        toLog.append(line).append("\n");
      }
    }
    toLog.append("!! End - ").append(filename).append("--------------------------------------------- !!\n");

    Logging.connectors.debug(toLog);
  }

  private void logReaderField(StringBuilder toLog, Reader reader){
    if (reader == null){
      toLog.append("null\n");
      return;
    }

    try (Scanner scanner = new Scanner(reader)){
      String line;
      while (scanner.hasNext()){
        line = scanner.nextLine();
        toLog.append(line).append("\n");
      }
    }
  }

  private void logList(StringBuilder toLog, String fieldName, Object[] fieldValues){
    toLog.append(fieldName).append(" = ");
    if (fieldValues == null){
      toLog.append("null\n");
      return;
    }
    if (fieldValues.length > 1){
      toLog.append("[");
    }
    for (Object oneValue : fieldValues) {
      if (oneValue instanceof Reader) {
        logReaderField(toLog, (Reader)oneValue);
      } else {
        toLog.append(oneValue.toString());
        if (fieldValues.length > 1) {
          toLog.append(" ; ");
        }
      }
    }
    if (fieldValues.length > 1){
      toLog.append("]");
    }
    toLog.append("\n");
  }

}
