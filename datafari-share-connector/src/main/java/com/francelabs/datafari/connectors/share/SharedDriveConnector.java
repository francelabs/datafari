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
package com.francelabs.datafari.connectors.share;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.connectorcommon.extmimemap.ExtensionMimeMap;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;
import org.apache.manifoldcf.connectorcommon.interfaces.KeystoreManagerFactory;
import org.apache.manifoldcf.core.common.DateParser;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.Configuration;
import org.apache.manifoldcf.core.interfaces.ConfigurationNode;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.LockManagerFactory;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.core.util.URLEncoder;
import org.apache.manifoldcf.crawler.interfaces.IExistingVersions;
import org.apache.manifoldcf.crawler.interfaces.IFingerprintActivity;
import org.apache.manifoldcf.crawler.interfaces.IHistoryActivity;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.crawler.system.ManifoldCF;

import com.francelabs.datafari.exception.RegexException;

import jcifs.ACE;
import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

/**
 * This is the "repository connector" for a smb/cifs shared drive file system. It's a relative of the share crawler, and should have comparable basic functionality.
 */
public class SharedDriveConnector extends org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector {
  public static final String _rcsid = "@(#)$Id: SharedDriveConnector.java 996524 2010-09-13 13:38:01Z kwright $";

  private static Level DOCPROCESSLEVEL = Level.forName("DOCPROCESS", 450);
  private static final Logger LOGGER = LogManager.getLogger(SharedDriveConnector.class.getName());

  // Activities we log
  private final static String ACTIVITY_ACCESS = "access";

  private static final String EDIT_CONFIGURATION_SERVER_HTML = "editConfiguration_Server.html";
  private static final String EDIT_CONFIGURATION_JS = "editConfiguration.js";
  private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
  private static final String EDIT_SPECIFICATION_GLOBAL_FILTERS_HTML = "editSpecification_Global_Filters.html";
  private static final String EDIT_SPECIFICATION_CONTENT_LENGTH_HTML = "editSpecification_Content_Length.html";
  private static final String VIEW_SPECIFICATION_GLOBAL_FILTERS_HTML = "viewSpecification_Global_Filters.html";

  // These are the share connector nodes and attributes in the document
  // specification
  private static final String NODE_STARTPOINT = "startpoint";
  private static final String NODE_INCLUDE = "include";
  private static final String NODE_EXCLUDE = "exclude";
  private static final String NODE_PATHNAMEATTRIBUTE = "pathnameattribute";
  private static final String NODE_PATHMAP = "pathmap";
  private static final String NODE_FILEMAP = "filemap";
  private static final String NODE_URIMAP = "urimap";
  private static final String NODE_SHAREACCESS = "shareaccess";
  private static final String NODE_SHARESECURITY = "sharesecurity";
  private static final String NODE_PARENTFOLDERACCESS = "parentfolderaccess";
  private static final String NODE_PARENTFOLDERSECURITY = "parentfoldersecurity";
  private static final String NODE_MAXLENGTH = "maxlength";
  private static final String NODE_MINLENGTH = "minlength";
  private static final String NODE_ACCESS = "access";
  private static final String NODE_SECURITY = "security";
  private static final String ATTRIBUTE_PATH = "path";
  private static final String ATTRIBUTE_TYPE = "type";
  private static final String ATTRIBUTE_INDEXABLE = "indexable";
  private static final String ATTRIBUTE_FILESPEC = "filespec";
  private static final String ATTRIBUTE_VALUE = "value";
  private static final String ATTRIBUTE_TOKEN = "token";
  private static final String ATTRIBUTE_MATCH = "match";
  private static final String ATTRIBUTE_REPLACE = "replace";
  private static final String VALUE_DIRECTORY = "directory";
  private static final String VALUE_FILE = "file";

  private static final String NODE_INCLUDEFILEFILTER = "includefilefilter";
  private static final String NODE_INCLUDEFOLDERFILTER = "includefolderfilter";
  private static final String NODE_EXCLUDEFILTER = "excludefilter";
  private static final String INCLUDEFILTER_DEFAULT = ".*";
  private static final String ATTRIBUTE_REGEX = "regex";

  // Default values for CIFS
  private static final String DEFAULT_LIST_COUNT = "200";
  private static final String DEFAULT_RESOLVE_ORDER = "LMHOSTS,DNS,WINS";
  private static final String DEFAULT_RESPONSE_TIMEOUT = "120000";
  private static final String DEFAULT_SO_TIMEOUT = "150000";
  private static final String DEFAULT_CONN_TIMEOUT = "150000";
  private static final String DEFAULT_SMB_MAX_VERSION = "SMB210";

  // Properties this connector needs (that can only be configured once)
  private final static String PROPERTY_JCIFS_USE_NTLM_V1 = "org.apache.manifoldcf.crawler.connectors.jcifs.usentlmv1";

  private String smbconnectionPath = null;
  private String server = null;
  private String domain = null;
  private String username = null;
  private String password = null;
  private boolean useSIDs = true;
  private String binName = null;
  private String connectionTimeout = null;
  private String sessionTimeout = null;
  private String socketTimeout = null;
  private String responseTimeout = null;
  private String resolveOrder = null;
  private String listCount = null;
  private String lmCompatibility = "3";
  private String useExtendedSecurity = "true";
  private String smbMaxVersion = null;

  private CIFSContext ctx;

  /** Deny access token for default authority */
  private final static String defaultAuthorityDenyToken = GLOBAL_DENY_TOKEN;

  /**
   * Constructor.
   */
  public SharedDriveConnector() {
  }

  /**
   * Set thread context. Use the opportunity to set the system properties we'll need.
   */
  @Override
  public void setThreadContext(final IThreadContext threadContext) throws ManifoldCFException {
    super.setThreadContext(threadContext);
    // We need to know whether to operate in NTLMv2 mode, or in NTLM mode. We do
    // this before jcifs called the first time.
    final boolean useV1 = LockManagerFactory.getBooleanProperty(threadContext, PROPERTY_JCIFS_USE_NTLM_V1, false);
    if (!useV1) {
      lmCompatibility = "3";
      useExtendedSecurity = "true";
    } else {
      lmCompatibility = "0";
      useExtendedSecurity = "false";
    }
  }

  /**
   * Establish a "session". In the case of the jcifs connector, this just builds the appropriate smbconnectionPath string, and does the necessary checks.
   */
  protected void getSession() throws ManifoldCFException {
    if (smbconnectionPath == null) {

      // Get the server
      if (server == null || server.length() == 0) {
        throw new ManifoldCFException("Missing parameter '" + SharedDriveParameters.server + "'");
      }

      // make the smb connection to the server
      if (domain == null || domain.length() == 0) {
        domain = null;
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Connecting to: " + "smb://" + (domain == null ? "" : domain) + ";" + username + ":<password>@" + server + "/");
      }

      try {
        // use NtlmPasswordAuthentication so that we can reuse credential for
        // DFS support
        final Properties props = new Properties();
        props.setProperty("jcifs.smb.client.maxVersion", smbMaxVersion);
        props.setProperty("jcifs.smb.client.dfs.strictView", "true");
        props.setProperty("jcifs.smb.lmCompatibility", lmCompatibility);
        props.setProperty("jcifs.smb.client.useExtendedSecurity", useExtendedSecurity);
        props.setProperty("jcifs.smb.client.listCount", listCount);
        props.setProperty("jcifs.resolveOrder", resolveOrder);
        props.setProperty("jcifs.smb.client.responseTimeout", responseTimeout);
        props.setProperty("jcifs.smb.client.soTimeout", socketTimeout);
        props.setProperty("jcifs.smb.client.connTimeout", connectionTimeout);
        props.setProperty("jcifs.smb.client.sessionTimeout", sessionTimeout);
        final CIFSContext bc = new BaseContext(new PropertyConfiguration(props));
        ctx = bc.withCredentials(new NtlmPasswordAuthenticator(domain, username, password));
        final SmbFile smbconnection = new SmbFile("smb://" + server + "/", ctx);
        smbconnectionPath = getFileCanonicalPath(smbconnection);
      } catch (final MalformedURLException | CIFSException e) {
        Logging.connectors.error("Unable to access SMB/CIFS share: " + "smb://" + (domain == null ? "" : domain) + ";" + username + ":<password>@" + server + "/\n" + e);
        throw new ManifoldCFException("Unable to access SMB/CIFS share: " + server, e, ManifoldCFException.REPOSITORY_CONNECTION_ERROR);
      }
    }
  }

  /**
   * Return the list of activities that this connector supports (i.e. writes into the log).
   *
   * @return the list.
   */
  @Override
  public String[] getActivitiesList() {
    return new String[] { ACTIVITY_ACCESS };
  }

  /**
   * Close the connection. Call this before discarding the repository connector.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    server = null;
    domain = null;
    username = null;
    password = null;
    ctx = null;
    smbconnectionPath = null;
    binName = null;
    connectionTimeout = null;
    sessionTimeout = null;
    socketTimeout = null;
    responseTimeout = null;
    resolveOrder = null;
    listCount = null;
    super.disconnect();
  }

  /**
   * Connect.
   *
   * @param configParameters is the set of configuration parameters, which in this case describe the root directory.
   */
  @Override
  public void connect(final ConfigParams configParameters) {
    super.connect(configParameters);

    // Get the server
    server = configParameters.getParameter(SharedDriveParameters.server);
    domain = configParameters.getParameter(SharedDriveParameters.domain);
    username = configParameters.getParameter(SharedDriveParameters.username);
    if (username == null) {
      username = "";
    }
    password = configParameters.getObfuscatedParameter(SharedDriveParameters.password);
    if (password == null) {
      password = "";
    }

    connectionTimeout = configParameters.getParameter(SharedDriveParameters.connectionTimeout);
    if (connectionTimeout == null) {
      connectionTimeout = DEFAULT_CONN_TIMEOUT;
    }

    sessionTimeout = configParameters.getParameter(SharedDriveParameters.sessionTimeout);
    if (sessionTimeout == null) {
      sessionTimeout = DEFAULT_CONN_TIMEOUT;
    }

    socketTimeout = configParameters.getParameter(SharedDriveParameters.socketTimeout);
    if (socketTimeout == null) {
      socketTimeout = DEFAULT_SO_TIMEOUT;
    }

    responseTimeout = configParameters.getParameter(SharedDriveParameters.responseTimeout);
    if (responseTimeout == null) {
      responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
    }

    resolveOrder = configParameters.getParameter(SharedDriveParameters.resolveOrder);
    if (resolveOrder == null) {
      resolveOrder = DEFAULT_RESOLVE_ORDER;
    }

    listCount = configParameters.getParameter(SharedDriveParameters.listCount);
    if (listCount == null) {
      listCount = DEFAULT_LIST_COUNT;
    }

    smbMaxVersion = configParameters.getParameter(SharedDriveParameters.smbMaxVersion);
    if (smbMaxVersion == null) {
      smbMaxVersion = DEFAULT_SMB_MAX_VERSION;
    }

    String useSIDsString = configParameters.getParameter(SharedDriveParameters.useSIDs);
    if (useSIDsString == null) {
      useSIDsString = "true";
    }
    useSIDs = "true".equals(useSIDsString);

    final String configBinName = configParameters.getParameter(SharedDriveParameters.binName);

    binName = configBinName == null || configBinName.length() == 0 ? server : configBinName;

    if (binName.length() > 255) {
      binName = binName.substring(0, 255);
    }

    // Rejigger the username/domain to be sure we PASS in a domain and we do not
    // include the domain attached to the user!
    // (This became essential at jcifs 1.3.0)
    int index = username.indexOf("@");
    if (index != -1) {
      // Strip off the domain from the user
      final String userDomain = username.substring(index + 1);
      if (domain == null || domain.length() == 0) {
        domain = userDomain;
      }
      username = username.substring(0, index);
    }
    index = username.indexOf("\\");
    if (index != -1) {
      final String userDomain = username.substring(0, index);
      if (domain == null || domain.length() == 0) {
        domain = userDomain;
      }
      username = username.substring(index + 1);
    }
  }

  /**
   * Get the bin name string for a document identifier. The bin name describes the queue to which the document will be assigned for throttling purposes. Throttling controls the rate at which items in
   * a given queue are fetched; it does not say anything about the overall fetch rate, which may operate on multiple queues or bins. For example, if you implement a web crawler, a good choice of bin
   * name would be the server name, since that is likely to correspond to a real resource that will need real throttle protection.
   *
   * @param documentIdentifier is the document identifier.
   * @return the bin name.
   */
  @Override
  public String[] getBinNames(final String documentIdentifier) {
    return new String[] { binName };
  }

  /**
   * Convert a document identifier to a URI. The URI is the URI that will be the unique key from the search index, and will be presented to the user as part of the search results.
   *
   * @param documentIdentifier is the document identifier.
   * @return the document uri.
   */
  protected static String convertToURI(final String documentIdentifier, final MatchMap fileMap, final MatchMap uriMap) throws ManifoldCFException {
    //
    // Note well: This MUST be a legal URI!!
    // e.g.
    // smb://10.33.65.1/Test Folder/PPT Docs/Dearman_University of Texas
    // 20030220.ppt
    // file:////10.33.65.1/Test Folder/PPT Docs/Dearman_University of Texas
    // 20030220.ppt

    String serverPath = documentIdentifier.substring("smb://".length());

    // The first mapping converts one server path to another.
    // If not present, we leave the original path alone.
    serverPath = fileMap.translate(serverPath);

    // The second mapping, if present, creates a URI, using certain rules. If
    // not present, the old standard IRI conversion is done.
    if (uriMap.getMatchCount() != 0) {
      // URI translation.
      // First step is to perform utf-8 translation and %-encoding.

      final byte[] byteArray = serverPath.getBytes(StandardCharsets.UTF_8);
      final StringBuilder output = new StringBuilder();
      int i = 0;
      while (i < byteArray.length) {
        final int x = byteArray[i++] & 0xff;
        if (x >= 0x80 || x >= 0 && x <= ' ' || x == ':' || x == '?' || x == '^' || x == '{' || x == '}' || x == '%' || x == '#' || x == '`' || x == ';' || x == '@' || x == '&' || x == '=' || x == '+'
            || x == '$' || x == ',') {
          output.append('%');
          final String hexValue = Integer.toHexString(x).toUpperCase(Locale.ROOT);
          if (hexValue.length() == 1) {
            output.append('0');
          }
          output.append(hexValue);
        } else {
          output.append((char) x);
        }
      }

      // Second step is to perform the mapping. This strips off the server name
      // and glues on the protocol and web server name, most likely.
      return uriMap.translate(output.toString());
    } else {
      // Convert to a URI that begins with file://///. This used to be done
      // according to the following IE7 specification:
      // http://blogs.msdn.com/ie/archive/2006/12/06/file-uris-in-windows.aspx
      // However, two factors required change. First, IE8 decided to no longer
      // adhere to the same specification as IE7.
      // Second, the ingestion API does not (and will never) accept anything
      // other than a well-formed URI. Thus, file
      // specifications are ingested in a canonical form (which happens to be
      // pretty much what this connector used prior to
      // 3.9.0), and the various clients are responsible for converting that
      // form into something the browser will accept.

      final StringBuilder output = new StringBuilder();

      int i = 0;
      while (i < serverPath.length()) {
        int pos = serverPath.indexOf("/", i);
        if (pos == -1) {
          pos = serverPath.length();
        }
        final String piece = serverPath.substring(i, pos);
        // Note well. This does *not* %-encode some characters such as '#',
        // which are legal in URI's but have special meanings!
        final String replacePiece = URLEncoder.encode(piece);
        // Convert the +'s back to %20's
        int j = 0;
        while (j < replacePiece.length()) {
          int plusPos = replacePiece.indexOf("+", j);
          if (plusPos == -1) {
            plusPos = replacePiece.length();
          }
          output.append(replacePiece.substring(j, plusPos));
          if (plusPos < replacePiece.length()) {
            output.append("%20");
            plusPos++;
          }
          j = plusPos;
        }

        if (pos < serverPath.length()) {
          output.append("/");
          pos++;
        }
        i = pos;
      }
      return "file://///" + output.toString();
    }
  }

  /**
   * Request arbitrary connector information. This method is called directly from the API in order to allow API users to perform any one of several connector-specific queries.
   *
   * @param output  is the response object, to be filled in by this method.
   * @param command is the command, which is taken directly from the API request.
   * @return true if the resource is found, false if not. In either case, output may be filled in.
   */
  @Override
  public boolean requestInfo(final Configuration output, final String command) throws ManifoldCFException {
    if (command.startsWith("folders/")) {
      final String parentFolder = command.substring("folders/".length());
      try {
        final String[] folders = getChildFolderNames(parentFolder);
        int i = 0;
        while (i < folders.length) {
          final String folder = folders[i++];
          final ConfigurationNode node = new ConfigurationNode("folder");
          node.setValue(folder);
          output.addChild(output.getChildCount(), node);
        }
      } catch (final ManifoldCFException e) {
        ManifoldCF.createErrorNode(output, e);
      }
    } else if (command.startsWith("folder/")) {
      final String folder = command.substring("folder/".length());
      try {
        final String canonicalFolder = validateFolderName(folder);
        if (canonicalFolder != null) {
          final ConfigurationNode node = new ConfigurationNode("folder");
          node.setValue(canonicalFolder);
          output.addChild(output.getChildCount(), node);
        }
      } catch (final ManifoldCFException e) {
        ManifoldCF.createErrorNode(output, e);
      }
    } else {
      return super.requestInfo(output, command);
    }
    return true;
  }

  /**
   * Queue "seed" documents. Seed documents are the starting places for crawling activity. Documents are seeded when this method calls appropriate methods in the passed in ISeedingActivity object.
   *
   * This method can choose to find repository changes that happen only during the specified time interval. The seeds recorded by this method will be viewed by the framework based on what the
   * getConnectorModel() method returns.
   *
   * It is not a big problem if the connector chooses to create more seeds than are strictly necessary; it is merely a question of overall work required.
   *
   * The end time and seeding version string passed to this method may be interpreted for greatest efficiency. For continuous crawling jobs, this method will be called once, when the job starts, and
   * at various periodic intervals as the job executes.
   *
   * When a job's specification is changed, the framework automatically resets the seeding version string to null. The seeding version string may also be set to null on each job run, depending on the
   * connector model returned by getConnectorModel().
   *
   * Note that it is always ok to send MORE documents rather than less to this method. The connector will be connected before this method can be called.
   *
   * @param activities      is the interface this method should use to perform whatever framework actions are desired.
   * @param spec            is a document specification (that comes from the job).
   * @param seedTime        is the end of the time range of documents to consider, exclusive.
   * @param lastSeedVersion is the last seeding version string for this job, or null if the job has no previous seeding version string.
   * @param jobMode         is an integer describing how the job is being run, whether continuous or once-only.
   * @return an updated seeding version string, to be stored with the job.
   */
  @Override
  public String addSeedDocuments(final ISeedingActivity activities, final Specification spec, final String lastSeedVersion, final long seedTime, final int jobMode)
      throws ManifoldCFException, ServiceInterruption {
    getSession();
    try {
      for (int i = 0; i < spec.getChildCount(); i++) {
        final SpecificationNode n = spec.getChild(i);
        if (n.getType().equals(NODE_STARTPOINT)) {
          // The id returned MUST be in canonical form!!!
          final String seed = mapToIdentifier(n.getAttributeValue(ATTRIBUTE_PATH));
          if (Logging.connectors.isDebugEnabled()) {
            Logging.connectors.debug("Seed = '" + seed + "'");
          }
          activities.addSeedDocument(seed);
        }
      }
    } catch (final MalformedURLException e) {
      throw new ManifoldCFException("Could not get a canonical path: " + e.getMessage(), e);
    } catch (final UnknownHostException e) {
      throw new ManifoldCFException("Could not get a canonical path: " + e.getMessage(), e);
    }
    return "";
  }

  /**
   * Process a set of documents. This is the method that should cause each document to be fetched, processed, and the results either added to the queue of documents for the current job, and/or entered
   * into the incremental ingestion manager. The document specification allows this class to filter what is done based on the job. The connector will be connected before this method can be called.
   *
   * @param documentIdentifiers  is the set of document identifiers to process.
   * @param statuses             are the currently-stored document versions for each document in the set of document identifiers passed in above.
   * @param activities           is the interface this method should use to queue up new document references and ingest documents.
   * @param jobMode              is an integer describing how the job is being run, whether continuous or once-only.
   * @param usesDefaultAuthority will be true only if the authority in use for these documents is the default one.
   */
  @Override
  public void processDocuments(final String[] documentIdentifiers, final IExistingVersions statuses, final Specification spec, final IProcessActivity activities, final int jobMode,
      final boolean usesDefaultAuthority) throws ManifoldCFException, ServiceInterruption {

    // Check if we should abort
    activities.checkJobStillActive();
    getSession();

    // Retrieve the global filters
    final GlobalFilters gf = new GlobalFilters(spec);

    // Read the forced acls. A null return indicates that security is
    // disabled!!!
    // A zero-length return indicates that the native acls should be used.
    // All of this is germane to how we ingest the document, so we need to note
    // it in
    // the version string completely.
    final String[] acls = getForcedAcls(spec);
    final String[] shareAcls = getForcedShareAcls(spec);
    final String[] parentFolderAcls = getForcedParentFolderAcls(spec);

    String pathAttributeName = null;
    final MatchMap matchMap = new MatchMap();
    final MatchMap fileMap = new MatchMap();
    final MatchMap uriMap = new MatchMap();

    // Set of configured paths in the job spec
    final Set<String> startPaths = new HashSet<String>();

    int i = 0;
    while (i < spec.getChildCount()) {
      final SpecificationNode n = spec.getChild(i++);
      if (n.getType().equals(NODE_PATHNAMEATTRIBUTE)) {
        pathAttributeName = n.getAttributeValue(ATTRIBUTE_VALUE);
      } else if (n.getType().equals(NODE_PATHMAP)) {
        // Path mapping info also needs to be looked at, because it affects what
        // is
        // ingested.
        final String pathMatch = n.getAttributeValue(ATTRIBUTE_MATCH);
        final String pathReplace = n.getAttributeValue(ATTRIBUTE_REPLACE);
        matchMap.appendMatchPair(pathMatch, pathReplace);
      } else if (n.getType().equals(NODE_FILEMAP)) {
        final String pathMatch = n.getAttributeValue(ATTRIBUTE_MATCH);
        final String pathReplace = n.getAttributeValue(ATTRIBUTE_REPLACE);
        fileMap.appendMatchPair(pathMatch, pathReplace);
      } else if (n.getType().equals(NODE_URIMAP)) {
        final String pathMatch = n.getAttributeValue(ATTRIBUTE_MATCH);
        final String pathReplace = n.getAttributeValue(ATTRIBUTE_REPLACE);
        uriMap.appendMatchPair(pathMatch, pathReplace);
      } else if (n.getType().equals(NODE_STARTPOINT)) {
        // The id returned MUST be in canonical form!!!
        try {
          final String seed = mapToIdentifier(n.getAttributeValue(ATTRIBUTE_PATH));
          startPaths.add(seed);
        } catch (UnknownHostException | MalformedURLException e) {
          throw new ManifoldCFException("Couldn't map to canonical path: " + e.getMessage(), e);
        }
      }
    }

    for (final String documentIdentifier : documentIdentifiers) {
      LOGGER.log(DOCPROCESSLEVEL, "DOC_PROCESS_START|WINSHARE|" + documentIdentifier);
      try {
        getSession();

        if (Logging.connectors.isDebugEnabled()) {
          Logging.connectors.debug("JCIFS: Processing '" + documentIdentifier + "'");
        }

        String versionString;
        SmbFile file;

        String ingestionURI = null;
        String pathAttributeValue = null;

        String[] shareAllow = null;
        String[] shareDeny = null;
        boolean shareSecurityOn = false;

        String[] parentAllow = null;
        String[] parentDeny = null;
        boolean parentSecurityOn = false;

        String[] documentAllow = null;
        String[] documentDeny = null;
        boolean documentSecurityOn = false;

        // Common info we really need to fetch only once
        long fileLength = 0L;
        long lastModified = 0L;
        boolean fileExists = false;
        boolean fileIsDirectory = false;
        boolean matchFolder = true;
        String errorCode = null;
        String errorDesc = null;

        try {
          file = new SmbFile(documentIdentifier, ctx);
          fileExists = fileExists(file);

          // File has to exist AND have a non-null canonical path to be readable.
          // If the canonical path is
          // null, it means that the windows permissions are not right and
          // directory/file is not readable!!!
          final String newPath = getFileCanonicalPath(file);
          // We MUST check the specification here, otherwise a recrawl may not
          // delete what it's supposed to!
          if (fileExists && newPath != null) {
            fileIsDirectory = fileIsDirectory(file);
            if (fileIsDirectory) {
              // First of all check if document match global filters
              try {
                // Check if the file is included
                // there must be declared folder filters AND the current folder must be different from a folder configured in the job specs
                if (!gf.includeFolderFilters.isEmpty()) {
                  final String matchingRegex = matchingRegex(gf.includeFolderFilters, documentIdentifier);
                  if (matchingRegex == null) {
                    if (!startPaths.contains(documentIdentifier)) {
                      activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, documentIdentifier, "EXCLUDED", "Does not match any global include folder filters", null);
                      activities.deleteDocument(documentIdentifier);
                      continue;
                    } else {
                      matchFolder = false;
                    }
                  }
                }
              } catch (final RegexException e) {
                Logging.connectors.warn("Global include folder filter regular expression syntax error: " + e.getRegex());
              }

            } else {
              // First of all check if document match global filters
              try {
                // Check if the file is included
                if (!gf.includeFileFilters.isEmpty()) {
                  final String matchingRegex = matchingRegex(gf.includeFileFilters, documentIdentifier);
                  if (matchingRegex == null) {
                    activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, documentIdentifier, "EXCLUDED", "Does not match any global include filters", null);
                    activities.deleteDocument(documentIdentifier);
                    continue;
                  }
                }
              } catch (final RegexException e) {
                Logging.connectors.warn("Global include filter regular expression syntax error: " + e.getRegex());
              }

              try {
                // Check if the file has to be excluded
                if (!gf.excludeFilters.isEmpty()) {
                  final String matchingRegex = matchingRegex(gf.excludeFilters, documentIdentifier);
                  if (matchingRegex != null) {
                    activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, documentIdentifier, "EXCLUDED",
                        "Has been excluded by the following global exclude filter: " + matchingRegex, null);
                    activities.deleteDocument(documentIdentifier);
                    continue;
                  }
                }
              } catch (final RegexException e) {
                Logging.connectors.warn("Global exclude filter regular expression syntax error: " + e.getRegex());
              }
            }
            if (checkInclude(fileIsDirectory, newPath, spec)) {
              if (fileIsDirectory) {
                // Hmm, this is not correct; version string should be empty for
                // windows directories, since
                // they are not hierarchical in modified date propagation.
                // It's a directory. The version ID will be the
                // last modified date.
                // long lastModified = fileLastModified(file);
                // versionString = new Long(lastModified).toString();
                versionString = "";

              } else {
                fileLength = fileLength(file);
                if (checkIncludeFile(fileLength, newPath, spec, activities)) {
                  // It's a file of acceptable length.
                  // The ability to get ACLs, list files, and an inputstream under
                  // DFS all work now.
                  // The SmbFile for parentFolder acls.
                  final SmbFile parentFolder = new SmbFile(file.getParent(), ctx);

                  // Compute the security information
                  final String[] modelArray = new String[0];

                  final List<String> allowList = new ArrayList<>();
                  final List<String> denyList = new ArrayList<>();
                  shareSecurityOn = getFileShareSecuritySet(allowList, denyList, file, shareAcls);
                  shareAllow = allowList.toArray(modelArray);
                  shareDeny = denyList.toArray(modelArray);

                  allowList.clear();
                  denyList.clear();
                  parentSecurityOn = getFileSecuritySet(allowList, denyList, parentFolder, parentFolderAcls);
                  parentAllow = allowList.toArray(modelArray);
                  parentDeny = denyList.toArray(modelArray);

                  allowList.clear();
                  denyList.clear();
                  documentSecurityOn = getFileSecuritySet(allowList, denyList, file, acls);
                  documentAllow = allowList.toArray(modelArray);
                  documentDeny = denyList.toArray(modelArray);

                  // This is stuff we need for computing the version string AND
                  // for indexing
                  lastModified = fileLastModified(file);

                  // The format of this string changed on 11/8/2006 to be
                  // comformant with the standard way
                  // acls and metadata descriptions are being stuffed into the
                  // version string across connectors.

                  // The format of this string changed again on 7/3/2009 to permit
                  // the ingestion uri/iri to be included.
                  // This was to support filename/uri mapping functionality.

                  final StringBuilder sb = new StringBuilder();

                  addSecuritySet(sb, shareSecurityOn, shareAllow, shareDeny);
                  addSecuritySet(sb, parentSecurityOn, parentAllow, parentDeny);
                  addSecuritySet(sb, documentSecurityOn, documentAllow, documentDeny);

                  // Include the path attribute name and value in the parseable
                  // area.
                  if (pathAttributeName != null) {
                    sb.append('+');
                    pack(sb, pathAttributeName, '+');
                    // Calculate path string; we'll include that wholesale in the
                    // version
                    pathAttributeValue = documentIdentifier;
                    // 3/13/2008
                    // In looking at what comes into the path metadata attribute
                    // by default, and cogitating a bit, I've concluded that
                    // the smb:// and the server/domain name at the start of the
                    // path are just plain old noise, and should be stripped.
                    // This changes a behavior that has been around for a while,
                    // so there is a risk, but a quick back-and-forth with the
                    // SE's leads me to believe that this is safe.

                    if (pathAttributeValue.startsWith("smb://")) {
                      int index = pathAttributeValue.indexOf("/", "smb://".length());
                      if (index == -1) {
                        index = pathAttributeValue.length();
                      }
                      pathAttributeValue = pathAttributeValue.substring(index);
                    }
                    // Now, translate
                    pathAttributeValue = matchMap.translate(pathAttributeValue);
                    pack(sb, pathAttributeValue, '+');
                  } else {
                    sb.append('-');
                  }

                  // Calculate the ingestion IRI/URI, and include that in the
                  // parseable area.
                  ingestionURI = convertToURI(documentIdentifier, fileMap, uriMap);
                  pack(sb, ingestionURI, '+');

                  // The stuff from here on down is non-parseable.
                  sb.append(String.valueOf(lastModified)).append(":").append(String.valueOf(fileLength));
                  // Also include the specification-based answer for the question
                  // of whether fingerprinting is
                  // going to be done. Although we may not consider this to truly
                  // be "version" information, the
                  // specification does affect whether anything is ingested or
                  // not, so it really is. The alternative
                  // is to fingerprint right here, in the version part of the
                  // world, but that's got a performance
                  // downside, because it means that we'd have to suck over pretty
                  // much everything just to determine
                  // what we wanted to ingest.
                  final boolean ifIndexable = wouldFileBeIncluded(newPath, spec, true);
                  final boolean ifNotIndexable = wouldFileBeIncluded(newPath, spec, false);
                  if (ifIndexable == ifNotIndexable) {
                    sb.append("I");
                  } else {
                    sb.append(ifIndexable ? "Y" : "N");
                  }
                  versionString = sb.toString();
                } else {
                  activities.deleteDocument(documentIdentifier);
                  activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, documentIdentifier, "FILTERED", "File does not match required specifications to be indexed", null);
                  continue;
                }
              }
            } else {
              activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, documentIdentifier, "EXCLUDED", "Has been excluded by filters", null);
              activities.deleteDocument(documentIdentifier);
              continue;
            }
          } else {
            activities.deleteDocument(documentIdentifier);
            activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, documentIdentifier, "UNREADABLE", "File does not exists, is not accessible or readable", null);
            continue;
          }
        } catch (final jcifs.smb.SmbAuthException e) {
          Logging.connectors.warn("JCIFS: Authorization exception reading version information for " + documentIdentifier + " - skipping");
          errorCode = "AUTHEXCEPTION";
          errorDesc = "Authorization exception: " + e.getMessage();
          activities.deleteDocument(documentIdentifier);
          continue;
        } catch (final MalformedURLException mue) {
          Logging.connectors.error("JCIFS: MalformedURLException thrown: " + mue.getMessage() + " - skipping", mue);
          errorCode = "MALFORMEDURLEXCEPTION";
          errorDesc = "MalformedURLException thrown: " + mue.getMessage();
          activities.deleteDocument(documentIdentifier);
          continue;
        } catch (final SmbException se) {
          errorCode = "SMBEXCEPTION";
          errorDesc = "SMB protocol exception: " + se.getMessage();
          processSMBException(se, documentIdentifier, "getting document version", "fetching share security");
          activities.deleteDocument(documentIdentifier);
          continue;
        } catch (final java.net.SocketTimeoutException e) {
          errorCode = "SOCKETTIMEOUT";
          errorDesc = "Socket timeout: " + e.getMessage();
          final long currentTime = System.currentTimeMillis();
          Logging.connectors.warn("JCIFS: Socket timeout reading version information for document " + documentIdentifier + ": " + e.getMessage(), e);
          throw new ServiceInterruption("Timeout or other service interruption: " + e.getMessage(), e, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
        } catch (final InterruptedIOException e) {
          errorCode = "INTERRUPTEDIOEXCEPTION";
          errorDesc = "InterruptedIOException: " + e.getMessage();
          final long currentTime = System.currentTimeMillis();
          Logging.connectors.warn("JCIFS: InterruptedIOException reading version information for document " + documentIdentifier + ": " + e.getMessage(), e);
          throw new ServiceInterruption("Timeout or other service interruption: " + e.getMessage(), e, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
        } catch (final IOException e) {
          errorCode = "IOEXCEPTION";
          errorDesc = "I/O error: " + e.getMessage();
          final long currentTime = System.currentTimeMillis();
          Logging.connectors.warn("JCIFS: I/O error reading version information for document " + documentIdentifier + ": " + e.getMessage(), e);
          throw new ServiceInterruption("Timeout or other service interruption: " + e.getMessage(), e, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
        } finally {
          if (errorCode != null) {
            activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, documentIdentifier, errorCode, errorDesc, null);
          }
        }

        if (versionString.length() == 0 || activities.checkDocumentNeedsReindexing(documentIdentifier, versionString)) {
          Long fileLengthLong = null;
          final long startFetchTime = System.currentTimeMillis();
          try {
            byte[] transferBuffer = null;

            try {

              if (fileExists) {
                if (fileIsDirectory) {
                  if (Logging.connectors.isDebugEnabled()) {
                    Logging.connectors.debug("JCIFS: '" + documentIdentifier + "' is a directory");
                  }

                  // Queue up stuff for directory
                  // DFS special support no longer needed, because JCifs now does
                  // the right thing.

                  // This is the string we replace in the child canonical paths.
                  // String matchPrefix = "";
                  // This is what we replace it with, to get back to a DFS path.
                  // String matchReplace = "";

                  // DFS resolved.

                  // Use a filter to actually do the work here. This prevents
                  // large arrays from being
                  // created when there are big directories.
                  final ProcessDocumentsFilter filter = new ProcessDocumentsFilter(activities, spec, matchFolder);
                  fileListFiles(file, filter);
                  filter.checkAndThrow();
                } else {
                  if (Logging.connectors.isDebugEnabled()) {
                    Logging.connectors.debug("JCIFS: '" + documentIdentifier + "' is a file");
                  }

                  // We've already avoided queuing documents that we
                  // don't want, based on file specifications.
                  // We still need to check based on file data.

                  // DFS support is now implicit in JCifs.

                  final String fileName = getFileCanonicalPath(file);
                  if (fileName != null && !file.isHidden()) {
                    final String uri = ingestionURI;
                    final String fileNameString = file.getName();
                    final Date lastModifiedDate = new Date(lastModified);
                    final Date creationDate = new Date(file.createTime());
                    final Long originalLength = fileLength;
                    final String contentType = mapExtensionToMimeType(fileNameString);

                    if (!activities.checkURLIndexable(uri)) {
                      Logging.connectors.debug("JCIFS: Skipping file because output connector cannot accept URL ('" + uri + "')");
                      errorCode = IHistoryActivity.EXCLUDED_URL;
                      errorDesc = "Rejected due to URL ('" + uri + "')";
                      activities.noDocument(documentIdentifier, versionString);
                      continue;
                    }

                    if (!activities.checkMimeTypeIndexable(contentType)) {
                      Logging.connectors.debug("JCIFS: Skipping file because output connector cannot accept content type ('" + contentType + "')");
                      errorCode = IHistoryActivity.EXCLUDED_MIMETYPE;
                      errorDesc = "Rejected due to mime type (" + contentType + ")";
                      activities.noDocument(documentIdentifier, versionString);
                      continue;
                    }

                    if (!activities.checkDateIndexable(lastModifiedDate)) {
                      Logging.connectors.debug("JCIFS: Skipping file because output connector cannot accept date (" + lastModifiedDate + ")");
                      errorCode = IHistoryActivity.EXCLUDED_DATE;
                      errorDesc = "Rejected due to date (" + lastModifiedDate + ")";
                      activities.noDocument(documentIdentifier, versionString);
                      continue;
                    }

                    // Initialize repository document with common stuff, and find
                    // the URI
                    final RepositoryDocument rd = new RepositoryDocument();

                    // If using the lastAccess patched/Google version of jcifs
                    // then this can be uncommented
                    // Date lastAccessDate = new Date(file.lastAccess());
                    final Integer attributes = file.getAttributes();
                    final String shareName = file.getShare();

                    rd.setFileName(fileNameString);
                    rd.setOriginalSize(originalLength);

                    if (contentType != null) {
                      rd.setMimeType(contentType);
                    }
                    rd.addField("lastModified", lastModifiedDate.toString());
                    rd.addField("fileLastModified", DateParser.formatISO8601Date(lastModifiedDate));
                    rd.setModifiedDate(lastModifiedDate);

                    // Add extra obtainable fields to the field map
                    rd.addField("createdOn", creationDate.toString());
                    rd.addField("fileCreatedOn", DateParser.formatISO8601Date(creationDate));
                    rd.setCreatedDate(creationDate);

                    // rd.addField("lastAccess", lastModifiedDate.toString());
                    rd.addField("attributes", Integer.toString(attributes));
                    rd.addField("shareName", shareName);

                    setDocumentSecurity(rd, shareAllow, shareDeny, parentAllow, parentDeny, documentAllow, documentDeny);
                    setPathMetadata(rd, pathAttributeName, pathAttributeValue);

                    // manipulate path to include the DFS alias, not the literal
                    // path
                    // String newPath = matchPrefix +
                    // fileName.substring(matchReplace.length());
                    final String newPath = fileName;
                    if (checkNeedFileData(newPath, spec)) {
                      if (Logging.connectors.isDebugEnabled()) {
                        Logging.connectors.debug("JCIFS: Local file data needed for '" + documentIdentifier + "'");
                      }

                      // Create a temporary file, and use that for the check and
                      // then the ingest
                      final File tempFile = File.createTempFile("_sdc_", null);
                      try {
                        final FileOutputStream os = new FileOutputStream(tempFile);
                        try {

                          // Now, make a local copy so we can fingerprint
                          final InputStream inputStream = getFileInputStream(file);
                          try {
                            // Copy!
                            if (transferBuffer == null) {
                              transferBuffer = new byte[65536];
                            }
                            while (true) {
                              final int amt = inputStream.read(transferBuffer, 0, transferBuffer.length);
                              if (amt == -1) {
                                break;
                              }
                              os.write(transferBuffer, 0, amt);
                            }
                          } finally {
                            inputStream.close();
                          }
                        } finally {
                          os.close();
                        }

                        if (checkIngest(tempFile, newPath, spec, activities)) {

                          if (Logging.connectors.isDebugEnabled()) {
                            Logging.connectors.debug("JCIFS: Decided to ingest '" + documentIdentifier + "'");
                          }
                          // OK, do ingestion itself!
                          final InputStream inputStream = new FileInputStream(tempFile);
                          try {
                            rd.setBinary(inputStream, fileLength);

                            activities.ingestDocumentWithException(documentIdentifier, versionString, uri, rd);
                            errorCode = "OK";
                            fileLengthLong = fileLength;
                          } finally {
                            inputStream.close();
                          }

                        } else {
                          // We must actively remove the document here, because
                          // the getDocumentVersions()
                          // method has no way of signalling this, since it does
                          // not do the fingerprinting.
                          if (Logging.connectors.isDebugEnabled()) {
                            Logging.connectors.debug("JCIFS: Decided to remove '" + documentIdentifier + "'");
                          }
                          activities.noDocument(documentIdentifier, versionString);
                          errorCode = "NOWORKNEEDED";
                          errorDesc = "No indexing needed for document at this time";
                        }
                      } finally {
                        tempFile.delete();
                      }
                    } else {
                      if (Logging.connectors.isDebugEnabled()) {
                        Logging.connectors.debug("JCIFS: Local file data not needed for '" + documentIdentifier + "'");
                      }

                      // Presume that since the file was queued that it fulfilled
                      // the needed criteria.
                      // Go off and ingest the fast way.

                      // Ingest the document.
                      final InputStream inputStream = getFileInputStream(file);
                      try {
                        rd.setBinary(inputStream, fileLength);

                        activities.ingestDocumentWithException(documentIdentifier, versionString, uri, rd);
                        errorCode = "OK";
                        fileLengthLong = fileLength;
                      } finally {
                        inputStream.close();
                      }
                    }
                  } else {
                    Logging.connectors.debug("JCIFS: Skipping file because canonical path is null, or because file is hidden");
                    errorCode = "NULLORHIDDEN";
                    errorDesc = "Null canonical path or hidden file";
                    activities.noDocument(documentIdentifier, versionString);
                    continue;
                  }
                }
              }
            } catch (final MalformedURLException mue) {
              Logging.connectors.error("MalformedURLException tossed: " + mue.getMessage(), mue);
              errorCode = mue.getClass().getSimpleName().toUpperCase(Locale.ROOT);
              errorDesc = "Malformed URL: " + mue.getMessage();
              activities.noDocument(documentIdentifier, versionString);
              continue;
            } catch (final jcifs.smb.SmbAuthException e) {
              Logging.connectors.warn("JCIFS: Authorization exception reading document/directory " + documentIdentifier + " - skipping");
              errorCode = e.getClass().getSimpleName().toUpperCase(Locale.ROOT);
              errorDesc = "Authorization: " + e.getMessage();
              activities.noDocument(documentIdentifier, versionString);
              continue;
            } catch (final SmbException se) {
              // At least some of these are transport errors, and should be
              // treated as service
              // interruptions.
              final long currentTime = System.currentTimeMillis();
              final Throwable cause = se.getCause();
              if (cause != null && cause instanceof jcifs.util.transport.TransportException) {
                Logging.connectors.warn("JCIFS: Timeout processing document/directory " + documentIdentifier + ": retrying...", se);
                errorCode = cause.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Transport: " + cause.getMessage();
                throw new ServiceInterruption("Timeout or other service interruption: " + cause.getMessage(), cause, currentTime + 300000L, currentTime + 12 * 60 * 60000L, -1, false);
              }
              if (se.getMessage().indexOf("reset by peer") != -1 || se.getMessage().indexOf("busy") != -1 || se.getMessage().toLowerCase(Locale.ROOT).indexOf("file in use") != -1
                  || se.getMessage().toLowerCase(Locale.ROOT).indexOf("is being used") != -1) {
                Logging.connectors.warn("JCIFS: 'Busy' response when processing document/directory for " + documentIdentifier + ": retrying...", se);
                errorCode = se.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Busy: " + se.getMessage();
                throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
              } else if (se.getMessage().indexOf("handle is invalid") != -1) {
                Logging.connectors.warn("JCIFS: 'Handle is invalid' response when processing document/directory for " + documentIdentifier + ": retrying...", se);
                errorCode = se.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Expiration: " + se.getMessage();
                throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
              } else if (se.getMessage().indexOf("parameter is incorrect") != -1) {
                Logging.connectors.warn("JCIFS: 'Parameter is incorrect' response when processing document/directory for " + documentIdentifier + ": retrying...", se);
                errorCode = se.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Expiration: " + se.getMessage();
                throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
              } else if (se.getMessage().indexOf("no longer available") != -1) {
                Logging.connectors.warn("JCIFS: 'No longer available' response when processing document/directory for " + documentIdentifier + ": retrying...", se);
                errorCode = se.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Expiration: " + se.getMessage();
                throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
              } else if (se.getMessage().indexOf("cannot find") != -1 || se.getMessage().indexOf("cannot be found") != -1) {
                if (Logging.connectors.isDebugEnabled()) {
                  Logging.connectors.debug("JCIFS: Skipping document/directory " + documentIdentifier + " because it cannot be found");
                }
                errorCode = se.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Not found: " + se.getMessage();
                activities.noDocument(documentIdentifier, versionString);
              } else if (se.getMessage().indexOf("0xC0000205") != -1) {
                Logging.connectors.warn("JCIFS: Out of resources exception reading document/directory " + documentIdentifier + " - skipping");
                // We call the delete even if it's a directory; this is harmless
                // and it cleans up the jobqueue row.
                errorCode = se.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Resources: " + se.getMessage();
                activities.noDocument(documentIdentifier, versionString);
              } else if (se.getMessage().indexOf("is denied") != -1) {
                Logging.connectors.warn("JCIFS: Access exception reading document/directory " + documentIdentifier + " - skipping");
                // We call the delete even if it's a directory; this is harmless
                // and it cleans up the jobqueue row.
                errorCode = se.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Authorization: " + se.getMessage();
                activities.noDocument(documentIdentifier, versionString);
              } else {
                Logging.connectors.error("JCIFS: SmbException tossed processing " + documentIdentifier + " - skipping", se);
                errorCode = se.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = "Unknown: " + se.getMessage();
                activities.noDocument(documentIdentifier, versionString);
              }
            } catch (final IOException e) {
              errorCode = e.getClass().getSimpleName().toUpperCase(Locale.ROOT);
              errorDesc = e.getMessage();
              handleIOException(documentIdentifier, e);
            }
          } catch (final ManifoldCFException e) {
            if (e.getErrorCode() == ManifoldCFException.INTERRUPTED) {
              errorCode = null;
            }
            throw e;
          } finally {
            if (errorCode != null) {
              activities.recordActivity(startFetchTime, ACTIVITY_ACCESS, fileLengthLong, documentIdentifier, errorCode, errorDesc, null);
            }
          }

        }
      } finally {
        LOGGER.log(DOCPROCESSLEVEL, "DOC_PROCESS_END|WINSHARE|" + documentIdentifier);
      }
    }
  }

  protected static void handleIOException(final String documentIdentifier, final IOException e) throws ManifoldCFException, ServiceInterruption {
    if (e instanceof java.net.SocketTimeoutException) {
      final long currentTime = System.currentTimeMillis();
      Logging.connectors.warn("JCIFS: Socket timeout processing " + documentIdentifier + ": " + e.getMessage(), e);
      throw new ServiceInterruption("Timeout or other service interruption: " + e.getMessage(), e, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    } else if (e instanceof InterruptedIOException) {
      final long currentTime = System.currentTimeMillis();
      Logging.connectors.warn("JCIFS: Interrupted processing " + documentIdentifier + ": " + e.getMessage(), e);
      throw new ServiceInterruption("Interrupted service: " + e.getMessage(), e, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    } else {
      final long currentTime = System.currentTimeMillis();
      Logging.connectors.warn("JCIFS: IO error processing " + documentIdentifier + ": " + e.getMessage(), e);
      throw new ServiceInterruption("Timeout or other service interruption: " + e.getMessage(), e, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    }
  }

  /** Map an extension to a mime type */
  protected static String mapExtensionToMimeType(String fileName) {
    final int slashIndex = fileName.lastIndexOf("/");
    if (slashIndex != -1) {
      fileName = fileName.substring(slashIndex + 1);
    }
    final int dotIndex = fileName.lastIndexOf(".");
    if (dotIndex == -1) {
      return null;
    }
    return ExtensionMimeMap.mapToMimeType(fileName.substring(dotIndex + 1).toLowerCase(java.util.Locale.ROOT));
  }

  protected static void addSecuritySet(final StringBuilder description, final boolean enabled, final String[] allowTokens, final String[] denyTokens) {
    if (enabled) {
      description.append("+");
      java.util.Arrays.sort(allowTokens);
      java.util.Arrays.sort(denyTokens);
      // Stuff the acls into the description string.
      packList(description, allowTokens, '+');
      packList(description, denyTokens, '+');
    } else {
      description.append("-");
    }

  }

  protected boolean getFileSecuritySet(final List<String> allowList, final List<String> denyList, final SmbFile file, final String[] forced) throws ManifoldCFException, IOException {
    if (forced != null) {
      if (forced.length == 0) {
        convertACEs(allowList, denyList, getFileSecurity(file, useSIDs));
      } else {
        for (final String forcedToken : forced) {
          allowList.add(forcedToken);
        }
        denyList.add(defaultAuthorityDenyToken);
      }
      return true;
    } else {
      return false;
    }
  }

  protected boolean getFileShareSecuritySet(final List<String> allowList, final List<String> denyList, final SmbFile file, final String[] forced) throws ManifoldCFException, IOException {
    if (forced != null) {
      if (forced.length == 0) {
        convertACEs(allowList, denyList, getFileShareSecurity(file, useSIDs));
      } else {
        for (final String forcedToken : forced) {
          allowList.add(forcedToken);
        }
        denyList.add(defaultAuthorityDenyToken);
      }
      return true;
    } else {
      return false;
    }
  }

  protected void convertACEs(final List<String> allowList, final List<String> denyList, final ACE[] aces) {
    if (aces == null) {
      // "Public" share: S-1-1-0
      allowList.add("S-1-1-0");
      denyList.add(defaultAuthorityDenyToken);
    } else {
      denyList.add(defaultAuthorityDenyToken);
      for (final ACE ace : aces) {
        if ((ace.getAccessMask() & ACE.FILE_READ_DATA) != 0) {
          if (ace.isAllow()) {
            allowList.add(useSIDs ? ace.getSID().toString() : ace.getSID().getAccountName());
          } else {
            denyList.add(useSIDs ? ace.getSID().toString() : ace.getSID().getAccountName());
          }
        }
      }
    }
  }

  protected static void processSMBException(final SmbException se, final String documentIdentifier, final String activity, final String operation) throws ManifoldCFException, ServiceInterruption {
    // At least some of these are transport errors, and should be treated as
    // service
    // interruptions.
    final long currentTime = System.currentTimeMillis();
    final Throwable cause = se.getCause();
    if (cause != null && cause instanceof jcifs.util.transport.TransportException) {
      Logging.connectors.warn("JCIFS: Timeout " + activity + " for " + documentIdentifier + ": retrying...", se);
      // Transport exceptions no longer abort when they give up, so we can't get
      // notified that there is a problem.

      throw new ServiceInterruption("Timeout or other service interruption: " + cause.getMessage(), cause, currentTime + 300000L, currentTime + 12 * 60 * 60000L, -1, false);
    }
    if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("busy") != -1) {
      Logging.connectors.warn("JCIFS: 'Busy' response when " + activity + " for " + documentIdentifier + ": retrying...", se);
      // Busy exceptions just skip the document and keep going
      throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    } else if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("handle is invalid") != -1 || se.getMessage().indexOf("0xC0000128") != -1) {
      Logging.connectors.warn("JCIFS: 'Handle is invalid' response when " + activity + " for " + documentIdentifier + ": retrying...", se);
      // Invalid handle errors treated like "busy"
      throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    } else if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("parameter is incorrect") != -1) {
      Logging.connectors.warn("JCIFS: 'Parameter is incorrect' response when " + activity + " for " + documentIdentifier + ": retrying...", se);
      // Invalid handle errors treated like "busy"
      throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    } else if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("no longer available") != -1) {
      Logging.connectors.warn("JCIFS: 'No longer available' response when " + activity + " for " + documentIdentifier + ": retrying...", se);
      // No longer available == busy
      throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    } else if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("no process is on the other end of the pipe") != -1) {
      Logging.connectors.warn("JCIFS: 'No process is on the other end of the pipe' response when " + activity + " for " + documentIdentifier + ": retrying...", se);
      // 'No process is on the other end of the pipe' skip the document and keep
      // going
      throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    } else if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("busy") != -1 || se.getMessage().toLowerCase(Locale.ROOT).indexOf("file in use") != -1
        || se.getMessage().toLowerCase(Locale.ROOT).indexOf("is being used") != -1) {
      Logging.connectors.warn("JCIFS: 'File in Use' response when " + activity + " for " + documentIdentifier + ": retrying...", se);
      // 'File in Use' skip the document and keep going
      throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    } else if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("cannot find") != -1 || se.getMessage().toLowerCase(Locale.ROOT).indexOf("cannot be found") != -1) {
      return;
    } else if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("is denied") != -1) {
      Logging.connectors.warn("JCIFS: Access exception when " + activity + " for " + documentIdentifier + " - skipping");
      return;
    } else if (se.getMessage().toLowerCase(Locale.ROOT).indexOf("incorrect function") != -1) {
      Logging.connectors.error("JCIFS: Server does not support a required operation (" + operation + "?) for " + documentIdentifier);
      return;
    } else {
      Logging.connectors.error("SmbException thrown " + activity + " for " + documentIdentifier, se);
      throw new ServiceInterruption("Timeout or other service interruption: " + se.getMessage(), se, currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
    }
  }

  protected static void setDocumentSecurity(final RepositoryDocument rd, final String[] shareAllow, final String[] shareDeny, final String[] parentAllow, final String[] parentDeny,
      final String[] allow, final String[] deny) {
    // set share acls
    if (shareAllow.length > 0 || shareDeny.length > 0) {
      rd.setSecurity(RepositoryDocument.SECURITY_TYPE_SHARE, shareAllow, shareDeny);
    }
    // set parent folder acls
    if (parentAllow.length > 0 || parentDeny.length > 0) {
      rd.setSecurity(RepositoryDocument.SECURITY_TYPE_PARENT, parentAllow, parentDeny);
    }
    // set native file acls
    if (allow.length > 0 || deny.length > 0) {
      rd.setSecurity(RepositoryDocument.SECURITY_TYPE_DOCUMENT, allow, deny);
    }
  }

  protected static void setPathMetadata(final RepositoryDocument rd, final String pathAttributeName, final String pathAttributeValue) throws ManifoldCFException {
    if (pathAttributeName != null && pathAttributeValue != null) {
      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JCIFS: Path attribute name is '" + pathAttributeName + "'");
      }
      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JCIFS: Path attribute value is '" + pathAttributeValue + "'");
      }
      rd.addField(pathAttributeName, pathAttributeValue);
    } else {
      Logging.connectors.debug("JCIFS: Path attribute name is null");
    }
  }

  /**
   * Check status of connection.
   */
  @Override

  public String check() throws ManifoldCFException {
    getSession();
    final String serverURI = smbconnectionPath;
    SmbFile server = null;
    try {
      server = new SmbFile(serverURI, ctx);
    } catch (final MalformedURLException e1) {
      return "Malformed URL: '" + serverURI + "': " + e1.getMessage();
    }
    try {
      // check to make sure it's a server or a folder
      final int type = getFileType(server);
      if (type == SmbFile.TYPE_SERVER || type == SmbFile.TYPE_SHARE || type == SmbFile.TYPE_FILESYSTEM) {
        try {
          server.connect();
          if (!server.exists()) {
            return "Server or path does not exist";
          }
        } catch (final java.net.SocketTimeoutException e) {
          return "Timeout connecting to server: " + e.getMessage();
        } catch (final InterruptedIOException e) {
          throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
        } catch (final IOException e) {
          return "Couldn't connect to server: " + e.getMessage();
        }
        return super.check();
      } else {
        return "URI is not a server URI: '" + serverURI + "'";
      }
    } catch (final SmbException e) {
      return "Could not connect: " + e.getMessage();
    }
  }

  // Protected methods

  /**
   * Check if a file's stats are OK for inclusion.
   */
  protected static boolean checkIncludeFile(final long fileLength, final String fileName, final Specification documentSpecification, final IProcessActivity activities)
      throws ManifoldCFException, ServiceInterruption {
    // If it's a file, make sure the maximum length is not exceeded
    if (!activities.checkLengthIndexable(fileLength) || !activities.checkMimeTypeIndexable(mapExtensionToMimeType(fileName))) {
      return false;
    }
    long maxFileLength = Long.MAX_VALUE;
    long minFileLength = 0L;
    for (int i = 0; i < documentSpecification.getChildCount(); i++) {
      final SpecificationNode sn = documentSpecification.getChild(i);
      if (sn.getType().equals(NODE_MAXLENGTH)) {
        try {
          final String value = sn.getAttributeValue(ATTRIBUTE_VALUE);
          if (value != null && value.length() > 0) {
            maxFileLength = Long.valueOf(value);
          }
        } catch (final NumberFormatException e) {
          throw new ManifoldCFException("Bad number: " + e.getMessage(), e);
        }
      }
      if (sn.getType().equals(NODE_MINLENGTH)) {
        try {
          final String value = sn.getAttributeValue(ATTRIBUTE_VALUE);
          if (value != null && value.length() > 0) {
            minFileLength = Long.valueOf(value);
          }
        } catch (final NumberFormatException e) {
          throw new ManifoldCFException("Bad number: " + e.getMessage(), e);
        }
      }
    }
    if (fileLength > maxFileLength) {
      activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, fileName, "EXCLUDED", "File exceeds the max allowed length", null);
      return false;
    } else if (fileLength < minFileLength) {
      activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, fileName, "EXCLUDED", "File length is below the min allowed length", null);
      return false;
    }
    return true;
  }

  /**
   * Check if a file or directory should be included, given a document specification.
   *
   * @param isDirectory           is true if the file is a directory.
   * @param fileName              is the canonical file name.
   * @param documentSpecification is the specification.
   * @return true if it should be included.
   */
  protected boolean checkInclude(final boolean isDirectory, final String fileName, final Specification documentSpecification) throws ManifoldCFException {
    if (Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("JCIFS: In checkInclude for '" + fileName + "'");
    }

    // This method does not attempt to do any fingerprinting. Instead, it will
    // opt to include any
    // file that may depend on fingerprinting, and exclude everything else. The
    // actual setup for
    // the fingerprinting test is in checkNeedFileData(), while the actual code
    // that determines in vs.
    // out using the file data is in checkIngest().
    try {
      String pathPart;
      if (isDirectory) {

        pathPart = fileName;
      } else {
        final int lastSlash = fileName.lastIndexOf("/");
        if (lastSlash == -1) {
          pathPart = "";
        } else {
          // Pathpart has to include the slash
          pathPart = fileName.substring(0, lastSlash + 1);
        }
      }

      int i;

      // Scan until we match a startpoint
      i = 0;
      while (i < documentSpecification.getChildCount()) {
        final SpecificationNode sn = documentSpecification.getChild(i++);
        if (sn.getType().equals(NODE_STARTPOINT)) {
          // Prepend the server URL to the path, since that's what pathpart will
          // have.
          final String path = mapToIdentifier(sn.getAttributeValue(ATTRIBUTE_PATH));

          // Compare with filename
          if (Logging.connectors.isDebugEnabled()) {
            Logging.connectors.debug("JCIFS: Matching startpoint '" + path + "' against actual '" + pathPart + "'");
          }
          final int matchEnd = matchSubPath(path, pathPart);
          if (matchEnd == -1) {
            Logging.connectors.debug("JCIFS: No match");
            continue;
          }

          Logging.connectors.debug("JCIFS: Startpoint found!");

          // If this is the root, it's always included.
          if (matchEnd == fileName.length()) {
            Logging.connectors.debug("JCIFS: Startpoint: always included");
            return true;
          }

          // matchEnd is the start of the rest of the path (after the match) in
          // fileName.
          // We need to walk through the rules and see whether it's in or out.
          int j = 0;
          while (j < sn.getChildCount()) {
            final SpecificationNode node = sn.getChild(j++);
            final String flavor = node.getType();
            if (flavor.equals(NODE_INCLUDE) || flavor.equals(NODE_EXCLUDE)) {
              String type = node.getAttributeValue(ATTRIBUTE_TYPE);
              if (type == null) {
                type = "";
              }
              String indexable = node.getAttributeValue(ATTRIBUTE_INDEXABLE);
              if (indexable == null) {
                indexable = "";
              }
              final String match = node.getAttributeValue(ATTRIBUTE_FILESPEC);

              // Check if there's a match against the filespec
              if (Logging.connectors.isDebugEnabled()) {
                Logging.connectors.debug("JCIFS: Checking '" + match + "' against '" + fileName.substring(matchEnd - 1) + "'");
              }
              boolean isMatch = checkMatch(fileName, matchEnd - 1, match);
              boolean isKnown = true;

              // Check the directory/file criteria
              if (isMatch) {
                Logging.connectors.debug("JCIFS: Match found.");
                isMatch = type.length() == 0 || type.equals(VALUE_DIRECTORY) && isDirectory || type.equals(VALUE_FILE) && !isDirectory;
              } else {
                Logging.connectors.debug("JCIFS: No match!");
              }

              // Check the indexable criteria
              if (isMatch) {
                if (indexable.length() != 0) {
                  // Directories are never considered indexable.
                  // But if this is not a directory, things become ambiguous.
                  boolean isIndexable;
                  if (isDirectory) {
                    isIndexable = false;
                    isMatch = indexable.equals("yes") && isIndexable || indexable.equals("no") && !isIndexable;
                  } else {
                    isKnown = false;
                  }

                }
              }

              if (isKnown) {
                if (isMatch) {
                  if (flavor.equals(NODE_INCLUDE)) {
                    return true;
                  } else {
                    return false;
                  }
                }
              } else {
                // Not known
                // What we do depends on whether this is an include rule or an
                // exclude one.
                // We want to err on the side of inclusion, which means for
                // include rules
                // we return true, and for exclude rules we simply continue.
                if (flavor.equals(NODE_INCLUDE)) {
                  return true;
                  // Continue
                }
              }
            }
          }

        }
      }
      return false;
    } catch (final MalformedURLException e) {
      throw new ManifoldCFException("Couldn't map to canonical path: " + e.getMessage(), e);
    } catch (final UnknownHostException e) {
      throw new ManifoldCFException("Couldn't map to canonical path: " + e.getMessage(), e);
    } finally {
      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JCIFS: Leaving checkInclude for '" + fileName + "'");
      }
    }

  }

  /**
   * Pretend that a file is either indexable or not, and return whether or not it would be ingested. This is only ever called for files.
   *
   * @param fileName              is the canonical file name.
   * @param documentSpecification is the specification.
   * @param pretendIndexable      should be set to true if the document's contents would be fingerprinted as "indexable", or false otherwise.
   * @return true if the file would be ingested given the parameters.
   */
  protected boolean wouldFileBeIncluded(final String fileName, final Specification documentSpecification, final boolean pretendIndexable) throws ManifoldCFException {
    if (Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("JCIFS: In wouldFileBeIncluded for '" + fileName + "', pretendIndexable=" + (pretendIndexable ? "true" : "false"));
    }

    // This file was flagged as needing file data. However, that doesn't tell us
    // *for what* we need it.
    // So we need to redo the decision tree, but this time do everything
    // completely.

    try {
      String pathPart;
      final boolean isDirectory = false;

      final int lastSlash = fileName.lastIndexOf("/");
      if (lastSlash == -1) {
        pathPart = "";
      } else {
        pathPart = fileName.substring(0, lastSlash + 1);
      }

      // Scan until we match a startpoint
      int i = 0;
      while (i < documentSpecification.getChildCount()) {
        final SpecificationNode sn = documentSpecification.getChild(i++);
        if (sn.getType().equals(NODE_STARTPOINT)) {
          // Prepend the server URL to the path, since that's what pathpart will
          // have.
          final String path = mapToIdentifier(sn.getAttributeValue(ATTRIBUTE_PATH));

          // Compare with filename
          final int matchEnd = matchSubPath(path, pathPart);
          if (matchEnd == -1) {
            continue;
          }

          // matchEnd is the start of the rest of the path (after the match) in
          // fileName.
          // We need to walk through the rules and see whether it's in or out.
          int j = 0;
          while (j < sn.getChildCount()) {
            final SpecificationNode node = sn.getChild(j++);
            final String flavor = node.getType();
            if (flavor.equals(NODE_INCLUDE) || flavor.equals(NODE_EXCLUDE)) {
              String type = node.getAttributeValue(ATTRIBUTE_TYPE);
              if (type == null) {
                type = "";
              }
              String indexable = node.getAttributeValue(ATTRIBUTE_INDEXABLE);
              if (indexable == null) {
                indexable = "";
              }
              final String match = node.getAttributeValue(ATTRIBUTE_FILESPEC);

              // Check if there's a match against the filespec
              boolean isMatch = checkMatch(fileName, matchEnd - 1, match);

              // Check the directory/file criteria
              if (isMatch) {
                isMatch = type.length() == 0 || type.equals(VALUE_DIRECTORY) && isDirectory || type.equals(VALUE_FILE) && !isDirectory;
              }

              // Check the indexable criteria
              if (isMatch) {
                if (indexable.length() != 0) {
                  // Directories are never considered indexable.
                  // But if this is not a directory, things become ambiguous.
                  boolean isIndexable;
                  if (isDirectory) {
                    isIndexable = false;
                  } else {
                    // Evaluate the parts of being indexable that are based on
                    // the filename, mime type, and url
                    isIndexable = pretendIndexable;
                  }

                  isMatch = indexable.equals("yes") && isIndexable || indexable.equals("no") && !isIndexable;

                }
              }

              if (isMatch) {
                if (flavor.equals(NODE_INCLUDE)) {
                  return true;
                } else {
                  return false;
                }
              }
            }
          }

        }
      }
      return false;
    } catch (final MalformedURLException e) {
      throw new ManifoldCFException("Couldn't map to canonical path: " + e.getMessage(), e);
    } catch (final UnknownHostException e) {
      throw new ManifoldCFException("Couldn't map to canonical path: " + e.getMessage(), e);
    } finally {
      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JCIFS: Leaving wouldFileBeIncluded for '" + fileName + "'");
      }
    }
  }

  /**
   * Check to see whether we need the contents of the file for anything. We do this by assuming that the file is indexable, and assuming that it's not, and seeing if the same thing would happen.
   *
   * @param fileName              is the name of the file.
   * @param documentSpecification is the document specification.
   * @return true if the file needs to be fingerprinted.
   */
  protected boolean checkNeedFileData(final String fileName, final Specification documentSpecification) throws ManifoldCFException {
    return wouldFileBeIncluded(fileName, documentSpecification, true) != wouldFileBeIncluded(fileName, documentSpecification, false);
  }

  /**
   * Check if a file should be ingested, given a document specification and a local copy of the file. It is presumed that only files that passed checkInclude() and were also flagged as needing file
   * data by checkNeedFileData() will be checked by this method.
   *
   * @param localFile             is the file.
   * @param fileName              is the JCIFS file name.
   * @param documentSpecification is the specification.
   * @param activities            are the activities available to determine indexability.
   * @return true if the file should be ingested.
   */
  protected boolean checkIngest(final File localFile, final String fileName, final Specification documentSpecification, final IFingerprintActivity activities)
      throws ManifoldCFException, ServiceInterruption {
    if (Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("JCIFS: In checkIngest for '" + fileName + "'");
    }

    // This file was flagged as needing file data. However, that doesn't tell us
    // *for what* we need it.
    // So we need to redo the decision tree, but this time do everything
    // completely.

    try {
      String pathPart;
      final boolean isDirectory = false;

      final int lastSlash = fileName.lastIndexOf("/");
      if (lastSlash == -1) {
        pathPart = "";
      } else {
        pathPart = fileName.substring(0, lastSlash + 1);
      }

      // Scan until we match a startpoint
      int i = 0;
      while (i < documentSpecification.getChildCount()) {
        final SpecificationNode sn = documentSpecification.getChild(i++);
        if (sn.getType().equals(NODE_STARTPOINT)) {
          // Prepend the server URL to the path, since that's what pathpart will
          // have.
          final String path = mapToIdentifier(sn.getAttributeValue(ATTRIBUTE_PATH));

          // Compare with filename
          final int matchEnd = matchSubPath(path, pathPart);
          if (matchEnd == -1) {
            continue;
          }

          // matchEnd is the start of the rest of the path (after the match) in
          // fileName.
          // We need to walk through the rules and see whether it's in or out.
          int j = 0;
          while (j < sn.getChildCount()) {
            final SpecificationNode node = sn.getChild(j++);
            final String flavor = node.getType();
            if (flavor.equals(NODE_INCLUDE) || flavor.equals(NODE_EXCLUDE)) {
              String type = node.getAttributeValue(ATTRIBUTE_TYPE);
              if (type == null) {
                type = "";
              }
              String indexable = node.getAttributeValue(ATTRIBUTE_INDEXABLE);
              if (indexable == null) {
                indexable = "";
              }
              final String match = node.getAttributeValue(ATTRIBUTE_FILESPEC);

              // Check if there's a match against the filespec
              boolean isMatch = checkMatch(fileName, matchEnd - 1, match);

              // Check the directory/file criteria
              if (isMatch) {
                isMatch = type.length() == 0 || type.equals(VALUE_DIRECTORY) && isDirectory || type.equals(VALUE_FILE) && !isDirectory;
              }

              // Check the indexable criteria
              if (isMatch) {
                if (indexable.length() != 0) {
                  // Directories are never considered indexable.
                  // But if this is not a directory, things become ambiguous.
                  boolean isIndexable;
                  if (isDirectory) {
                    isIndexable = false;
                  } else {
                    isIndexable = activities.checkDocumentIndexable(localFile);
                  }

                  isMatch = indexable.equals("yes") && isIndexable || indexable.equals("no") && !isIndexable;

                }
              }

              if (isMatch) {
                if (flavor.equals(NODE_INCLUDE)) {
                  return true;
                } else {
                  return false;
                }
              }
            }
          }

        }
      }
      return false;
    } catch (final MalformedURLException e) {
      throw new ManifoldCFException("Couldn't map to canonical path: " + e.getMessage(), e);
    } catch (final UnknownHostException e) {
      throw new ManifoldCFException("Couldn't map to canonical path: " + e.getMessage(), e);
    } finally {
      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JCIFS: Leaving checkIngest for '" + fileName + "'");
      }
    }

  }

  /**
   * Match a sub-path. The sub-path must match the complete starting part of the full path, in a path sense. The returned value should point into the file name beyond the end of the matched path, or
   * be -1 if there is no match.
   *
   * @param subPath  is the sub path.
   * @param fullPath is the full path.
   * @return the index of the start of the remaining part of the full path, or -1.
   */
  protected static int matchSubPath(final String subPath, final String fullPath) {
    if (subPath.length() > fullPath.length()) {
      return -1;
    }
    if (fullPath.startsWith(subPath) == false) {
      return -1;
    }
    int rval = subPath.length();
    if (fullPath.length() == rval) {
      return rval;
    }
    final char x = fullPath.charAt(rval);
    if (x == File.separatorChar) {
      rval++;
    }
    return rval;
  }

  /**
   * Check a match between two strings with wildcards.
   *
   * @param sourceMatch is the expanded string (no wildcards)
   * @param sourceIndex is the starting point in the expanded string.
   * @param match       is the wildcard-based string.
   * @return true if there is a match.
   */
  protected static boolean checkMatch(final String sourceMatch, final int sourceIndex, final String match) {
    // Note: The java regex stuff looks pretty heavyweight for this purpose.
    // I've opted to try and do a simple recursive version myself, which is not
    // compiled.
    // Basically, the match proceeds by recursive descent through the string, so
    // that all *'s cause
    // recursion.
    final boolean caseSensitive = false;

    return processCheck(caseSensitive, sourceMatch, sourceIndex, match, 0);
  }

  /**
   * Recursive worker method for checkMatch. Returns 'true' if there is a path that consumes both strings in their entirety in a matched way.
   *
   * @param caseSensitive is true if file names are case sensitive.
   * @param sourceMatch   is the source string (w/o wildcards)
   * @param sourceIndex   is the current point in the source string.
   * @param match         is the match string (w/wildcards)
   * @param matchIndex    is the current point in the match string.
   * @return true if there is a match.
   */
  protected static boolean processCheck(final boolean caseSensitive, final String sourceMatch, int sourceIndex, final String match, int matchIndex) {
    // Logging.connectors.debug("Matching '"+sourceMatch+"' position
    // "+Integer.toString(sourceIndex)+
    // " against '"+match+"' position "+Integer.toString(matchIndex));

    // Match up through the next * we encounter
    while (true) {
      // If we've reached the end, it's a match.
      if (sourceMatch.length() == sourceIndex && match.length() == matchIndex) {
        return true;
      }
      // If one has reached the end but the other hasn't, no match
      if (match.length() == matchIndex) {
        return false;
      }
      if (sourceMatch.length() == sourceIndex) {
        if (match.charAt(matchIndex) != '*') {
          return false;
        }
        matchIndex++;
        continue;
      }
      char x = sourceMatch.charAt(sourceIndex);
      char y = match.charAt(matchIndex);
      if (!caseSensitive) {
        if (x >= 'A' && x <= 'Z') {
          x -= 'A' - 'a';
        }
        if (y >= 'A' && y <= 'Z') {
          y -= 'A' - 'a';
        }
      }
      if (y == '*') {
        // Wildcard!
        // We will recurse at this point.
        // Basically, we want to combine the results for leaving the "*" in the
        // match string
        // at this point and advancing the source index, with skipping the "*"
        // and leaving the source
        // string alone.
        return processCheck(caseSensitive, sourceMatch, sourceIndex + 1, match, matchIndex) || processCheck(caseSensitive, sourceMatch, sourceIndex, match, matchIndex + 1);
      }
      if (y == '?' || x == y) {
        sourceIndex++;
        matchIndex++;
      } else {
        return false;
      }
    }
  }

  /**
   * Grab forced acl out of document specification.
   *
   * @param spec is the document specification.
   * @return the acls.
   */
  protected static String[] getForcedAcls(final Specification spec) {
    final Map<String, String> map = new HashMap<String, String>();
    int i = 0;
    boolean securityOn = true;
    while (i < spec.getChildCount()) {
      final SpecificationNode sn = spec.getChild(i++);
      if (sn.getType().equals(NODE_ACCESS)) {
        final String token = sn.getAttributeValue(ATTRIBUTE_TOKEN);
        map.put(token, token);
      } else if (sn.getType().equals(NODE_SECURITY)) {
        final String value = sn.getAttributeValue(ATTRIBUTE_VALUE);
        if (value.equals("on")) {
          securityOn = true;
        } else if (value.equals("off")) {
          securityOn = false;
        }
      }
    }
    if (!securityOn) {
      return null;
    }

    final String[] rval = new String[map.size()];
    final Iterator<String> iter = map.keySet().iterator();
    i = 0;
    while (iter.hasNext()) {
      rval[i++] = iter.next();
    }
    return rval;
  }

  /**
   * Grab forced share acls out of document specification.
   *
   * @param spec is the document specification.
   * @return the acls.
   */
  protected static String[] getForcedShareAcls(final Specification spec) {
    final Map<String, String> map = new HashMap<String, String>();
    int i = 0;
    boolean securityOn = true;
    while (i < spec.getChildCount()) {
      final SpecificationNode sn = spec.getChild(i++);
      if (sn.getType().equals(NODE_SHAREACCESS)) {
        final String token = sn.getAttributeValue(ATTRIBUTE_TOKEN);
        map.put(token, token);
      } else if (sn.getType().equals(NODE_SHARESECURITY)) {
        final String value = sn.getAttributeValue(ATTRIBUTE_VALUE);
        if (value.equals("on")) {
          securityOn = true;
        } else if (value.equals("off")) {
          securityOn = false;
        }
      }
    }
    if (!securityOn) {
      return null;
    }
    final String[] rval = new String[map.size()];
    final Iterator<String> iter = map.keySet().iterator();
    i = 0;
    while (iter.hasNext()) {
      rval[i++] = iter.next();
    }
    return rval;
  }

  /**
   * Grab forced parent folder acls out of document specification.
   *
   * @param spec is the document specification.
   * @return the acls.
   */
  protected static String[] getForcedParentFolderAcls(final Specification spec) {
    final Map<String, String> map = new HashMap<String, String>();
    int i = 0;
    boolean securityOn = false;
    while (i < spec.getChildCount()) {
      final SpecificationNode sn = spec.getChild(i++);
      if (sn.getType().equals(NODE_PARENTFOLDERACCESS)) {
        final String token = sn.getAttributeValue(ATTRIBUTE_TOKEN);
        map.put(token, token);
      } else if (sn.getType().equals(NODE_PARENTFOLDERSECURITY)) {
        final String value = sn.getAttributeValue(ATTRIBUTE_VALUE);
        if (value.equals("on")) {
          securityOn = true;
        } else if (value.equals("off")) {
          securityOn = false;
        }
      }
    }
    if (!securityOn) {
      return null;
    }
    final String[] rval = new String[map.size()];
    final Iterator<String> iter = map.keySet().iterator();
    i = 0;
    while (iter.hasNext()) {
      rval[i++] = iter.next();
    }
    return rval;
  }

  /**
   * Map a "path" specification to a full identifier.
   */
  protected String mapToIdentifier(final String path) throws MalformedURLException, UnknownHostException {
    final String smburi = smbconnectionPath;
    final String uri = smburi + path + "/";
    return getFileCanonicalPath(new SmbFile(uri, ctx));
  }

  // These methods allow me to experiment with cluster-mandated error handling
  // on an entirely local level. They correspond to individual SMBFile methods.

  /** Get canonical path */
  protected static String getFileCanonicalPath(final SmbFile file) {
    return file.getCanonicalPath();
  }

  /** Check for file/directory existence */
  protected static boolean fileExists(final SmbFile file) throws SmbException {
    int totalTries = 0;
    int retriesRemaining = 3;
    SmbException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.exists();
      } catch (final SmbException e) {
        // If it's an interruption, throw it right away.
        final Throwable cause = e.getCause();
        if (cause != null && cause instanceof jcifs.util.transport.TransportException) {
          // See if it's an interruption
          final jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException) cause;
          if (te.getCause() != null && te.getCause() instanceof java.lang.InterruptedException) {
            throw e;
          }
        }

        Logging.connectors
            .warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while checking if file " + file.getCanonicalPath() + " exists: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentSmbExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** Check if file is a directory */
  protected static boolean fileIsDirectory(final SmbFile file) throws SmbException {
    int totalTries = 0;
    int retriesRemaining = 3;
    SmbException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.isDirectory();
      } catch (final SmbException e) {
        // If it's an interruption, throw it right away.
        final Throwable cause = e.getCause();
        if (cause != null && cause instanceof jcifs.util.transport.TransportException) {
          // See if it's an interruption
          final jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException) cause;
          if (te.getCause() != null && te.getCause() instanceof java.lang.InterruptedException) {
            throw e;
          }
        }

        Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while seeing if file is a directory: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentSmbExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** Get last modified date for file */
  protected static long fileLastModified(final SmbFile file) throws SmbException {
    int totalTries = 0;
    int retriesRemaining = 3;
    SmbException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.lastModified();
      } catch (final SmbException e) {
        // If it's an interruption, throw it right away.
        final Throwable cause = e.getCause();
        if (cause != null && cause instanceof jcifs.util.transport.TransportException) {
          // See if it's an interruption
          final jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException) cause;
          if (te.getCause() != null && te.getCause() instanceof java.lang.InterruptedException) {
            throw e;
          }
        }

        Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while getting file last-modified date: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentSmbExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** Get file length */
  protected static long fileLength(final SmbFile file) throws SmbException {
    int totalTries = 0;
    int retriesRemaining = 3;
    SmbException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.length();
      } catch (final SmbException e) {
        // If it's an interruption, throw it right away.
        final Throwable cause = e.getCause();
        if (cause != null && cause instanceof jcifs.util.transport.TransportException) {
          // See if it's an interruption
          final jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException) cause;
          if (te.getCause() != null && te.getCause() instanceof java.lang.InterruptedException) {
            throw e;
          }
        }

        Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while getting file length: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentSmbExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** List files */
  protected static SmbFile[] fileListFiles(final SmbFile file, final SmbFileFilter filter) throws SmbException {
    int totalTries = 0;
    int retriesRemaining = 3;
    SmbException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.listFiles(filter);
      } catch (final SmbException e) {
        // If it's an interruption, throw it right away.
        final Throwable cause = e.getCause();
        if (cause != null && cause instanceof jcifs.util.transport.TransportException) {
          // See if it's an interruption
          final jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException) cause;
          if (te.getCause() != null && te.getCause() instanceof java.lang.InterruptedException) {
            throw e;
          }
        }
        if (e.getMessage().equals("0x8000002D")) {
          // Symlink
          Logging.connectors.warn("JCIFS: Symlink detected: " + file);
          return new SmbFile[0];
        }
        Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while listing files: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentSmbExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** Get input stream for file */
  protected static InputStream getFileInputStream(final SmbFile file) throws IOException {
    int totalTries = 0;
    int retriesRemaining = 3;
    IOException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.getInputStream();
      } catch (final java.net.SocketTimeoutException e) {
        throw e;
      } catch (final InterruptedIOException e) {
        throw e;
      } catch (final IOException e) {
        Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while getting file input stream: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentIOExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** Get file security */
  protected static ACE[] getFileSecurity(final SmbFile file, final boolean useSIDs) throws IOException {
    int totalTries = 0;
    int retriesRemaining = 3;
    IOException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.getSecurity(!useSIDs);
      } catch (final java.net.SocketTimeoutException e) {
        throw e;
      } catch (final InterruptedIOException e) {
        throw e;
      } catch (final IOException e) {
        Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while getting file security: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentIOExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** Get share security */
  protected static ACE[] getFileShareSecurity(final SmbFile file, final boolean useSIDs) throws IOException {
    int totalTries = 0;
    int retriesRemaining = 3;
    IOException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.getShareSecurity(!useSIDs);
      } catch (final java.net.SocketTimeoutException e) {
        throw e;
      } catch (final InterruptedIOException e) {
        throw e;
      } catch (final IOException e) {
        Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while getting share security: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentIOExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** Get file type */
  protected static int getFileType(final SmbFile file) throws SmbException {
    int totalTries = 0;
    int retriesRemaining = 3;
    SmbException currentException = null;
    while (retriesRemaining > 0 && totalTries < 5) {
      retriesRemaining--;
      totalTries++;
      try {
        return file.getType();
      } catch (final SmbException e) {
        // If it's an interruption, throw it right away.
        final Throwable cause = e.getCause();
        if (cause != null && cause instanceof jcifs.util.transport.TransportException) {
          // See if it's an interruption
          final jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException) cause;
          if (te.getCause() != null && te.getCause() instanceof java.lang.InterruptedException) {
            throw e;
          }
        }

        Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt " + Integer.toString(totalTries) + " while getting file type: " + e.getMessage(), e);
        if (currentException != null) {
          // Compare exceptions. If they differ, reset the retry count.
          if (!equivalentSmbExceptions(currentException, e)) {
            retriesRemaining = 3;
          }
        }
        currentException = e;
      }
    }
    throw currentException;
  }

  /** Check if two SmbExceptions are equivalent */
  protected static boolean equivalentSmbExceptions(final SmbException e1, final SmbException e2) {
    // The thing we want to compare is the message. This is a little risky in
    // that if there are (for example) object addresses in the message, the
    // comparison will always fail.
    // However, I don't think we expect any such thing in this case.
    String e1m = e1.getMessage();
    String e2m = e2.getMessage();
    if (e1m == null) {
      e1m = "";
    }
    if (e2m == null) {
      e2m = "";
    }
    return e1m.equals(e2m);
  }

  /** Check if two IOExceptions are equivalent */
  protected static boolean equivalentIOExceptions(final IOException e1, final IOException e2) {
    // The thing we want to compare is the message. This is a little risky in
    // that if there are (for example) object addresses in the message, the
    // comparison will always fail.
    // However, I don't think we expect any such thing in this case.
    String e1m = e1.getMessage();
    String e2m = e2.getMessage();
    if (e1m == null) {
      e1m = "";
    }
    if (e2m == null) {
      e2m = "";
    }
    return e1m.equals(e2m);
  }

  // UI support methods.
  //
  // These support methods come in two varieties. The first bunch is involved in
  // setting up connection configuration information. The second bunch
  // is involved in presenting and editing document specification information
  // for a job. The two kinds of methods are accordingly treated differently,
  // in that the first bunch cannot assume that the current connector object is
  // connected, while the second bunch can. That is why the first bunch
  // receives a thread context argument for all UI methods, while the second
  // bunch does not need one (since it has already been applied via the
  // connect()
  // method, above).

  /**
   * Output the configuration header section. This method is called in the head section of the connector's configuration page. Its purpose is to add the required tabs to the list, and to output any
   * javascript methods that might be needed by the configuration editing HTML.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabsArray     is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final List<String> tabsArray)
      throws ManifoldCFException, IOException {
    tabsArray.add(Messages.getString(locale, "SharedDriveConnector.Server"));
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_JS, null);
  }

  protected static void fillInServerTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) throws ManifoldCFException {

    String server = parameters.getParameter(SharedDriveParameters.server);
    if (server == null) {
      server = "";
    }
    String domain = parameters.getParameter(SharedDriveParameters.domain);
    if (domain == null) {
      domain = "";
    }
    String username = parameters.getParameter(SharedDriveParameters.username);
    if (username == null) {
      username = "";
    }
    String password = parameters.getObfuscatedParameter(SharedDriveParameters.password);
    if (password == null) {
      password = "";
    } else {
      password = out.mapPasswordToKey(password);
    }
    String resolvesids = parameters.getParameter(SharedDriveParameters.useSIDs);
    if (resolvesids == null) {
      resolvesids = "true";
    }
    String binName = parameters.getParameter(SharedDriveParameters.binName);
    if (binName == null) {
      binName = "";
    }

    String connectionTimeout = parameters.getParameter(SharedDriveParameters.connectionTimeout);
    if (connectionTimeout == null) {
      connectionTimeout = DEFAULT_CONN_TIMEOUT;
    }
    String sessionTimeout = parameters.getParameter(SharedDriveParameters.sessionTimeout);
    if (sessionTimeout == null) {
      sessionTimeout = DEFAULT_CONN_TIMEOUT;
    }
    String socketTimeout = parameters.getParameter(SharedDriveParameters.socketTimeout);
    if (socketTimeout == null) {
      socketTimeout = DEFAULT_SO_TIMEOUT;
    }
    String responseTimeout = parameters.getParameter(SharedDriveParameters.responseTimeout);
    if (responseTimeout == null) {
      responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
    }
    String resolveOrder = parameters.getParameter(SharedDriveParameters.resolveOrder);
    if (resolveOrder == null) {
      resolveOrder = DEFAULT_RESOLVE_ORDER;
    }
    String listCount = parameters.getParameter(SharedDriveParameters.listCount);
    if (listCount == null) {
      listCount = DEFAULT_LIST_COUNT;
    }
    String smbMaxVersion = parameters.getParameter(SharedDriveParameters.smbMaxVersion);
    if (smbMaxVersion == null) {
      smbMaxVersion = DEFAULT_SMB_MAX_VERSION;
    }

    // Fill in context
    velocityContext.put("SERVER", server);
    velocityContext.put("DOMAIN", domain);
    velocityContext.put("USERNAME", username);
    velocityContext.put("PASSWORD", password);
    velocityContext.put("RESOLVEIDS", resolvesids);
    velocityContext.put("BINNAME", binName);

    velocityContext.put("CONNECTIONTIMEOUT", connectionTimeout);
    velocityContext.put("SESSIONTIMEOUT", sessionTimeout);
    velocityContext.put("SOCKETTIMEOUT", socketTimeout);
    velocityContext.put("RESPONSETIMEOUT", responseTimeout);
    velocityContext.put("RESOLVEORDER", resolveOrder);
    velocityContext.put("LISTCOUNT", listCount);
    velocityContext.put("SMBMAXVERSION", smbMaxVersion);
  }

  /**
   * Output the configuration body section. This method is called in the body section of the connector's configuration page. Its purpose is to present the required form elements for editing. The coder
   * can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags. The name of the form is "editconnection".
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabName       is the current tab name.
   */
  @Override
  public void outputConfigurationBody(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final String tabName)
      throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("TabName", tabName);
    fillInServerTab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_SERVER_HTML, velocityContext);
  }

  /**
   * Process a configuration post. This method is called at the start of the connector's configuration page, whenever there is a possibility that form data for a connection has been posted. Its
   * purpose is to gather form information and modify the configuration parameters accordingly. The name of the posted form is "editconnection".
   *
   * @param threadContext   is the local thread context.
   * @param variableContext is the set of variables available from the post, including binary file post information.
   * @param parameters      are the configuration parameters, as they currently exist, for this connection being configured.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a redirection to an error page).
   */
  @Override
  public String processConfigurationPost(final IThreadContext threadContext, final IPostParameters variableContext, final Locale locale, final ConfigParams parameters) throws ManifoldCFException {

    final String server = variableContext.getParameter("server");
    if (server != null) {
      parameters.setParameter(SharedDriveParameters.server, server);
    }

    final String connectionTimeout = variableContext.getParameter("connectiontimeout");
    if (connectionTimeout != null) {
      parameters.setParameter(SharedDriveParameters.connectionTimeout, connectionTimeout);
    }

    final String sessionTimeout = variableContext.getParameter("sessiontimeout");
    if (sessionTimeout != null) {
      parameters.setParameter(SharedDriveParameters.sessionTimeout, sessionTimeout);
    }

    final String socketTimeout = variableContext.getParameter("sockettimeout");
    if (socketTimeout != null) {
      parameters.setParameter(SharedDriveParameters.socketTimeout, socketTimeout);
    }

    final String responseTimeout = variableContext.getParameter("responsetimeout");
    if (responseTimeout != null) {
      parameters.setParameter(SharedDriveParameters.responseTimeout, responseTimeout);
    }

    final String resolveOrder = variableContext.getParameter("resolveorder");
    if (resolveOrder != null) {
      parameters.setParameter(SharedDriveParameters.resolveOrder, resolveOrder);
    }

    final String listCount = variableContext.getParameter("listcount");
    if (listCount != null) {
      parameters.setParameter(SharedDriveParameters.listCount, listCount);
    }

    final String domain = variableContext.getParameter("domain");
    if (domain != null) {
      parameters.setParameter(SharedDriveParameters.domain, domain);
    }

    final String username = variableContext.getParameter("username");
    if (username != null) {
      parameters.setParameter(SharedDriveParameters.username, username);
    }

    final String password = variableContext.getParameter("password");
    if (password != null) {
      parameters.setObfuscatedParameter(SharedDriveParameters.password, variableContext.mapKeyToPassword(password));
    }

    final String resolvesidspresent = variableContext.getParameter("resolvesidspresent");
    if (resolvesidspresent != null) {
      parameters.setParameter(SharedDriveParameters.useSIDs, "false");
      final String resolvesids = variableContext.getParameter("resolvesids");
      if (resolvesids != null) {
        parameters.setParameter(SharedDriveParameters.useSIDs, resolvesids);
      }
    }
    final String binName = variableContext.getParameter("binname");
    if (binName != null) {
      parameters.setParameter(SharedDriveParameters.binName, binName);
    }

    final String smbMaxVersion = variableContext.getParameter("smbmaxversion");
    if (smbMaxVersion != null) {
      parameters.setParameter(SharedDriveParameters.smbMaxVersion, smbMaxVersion);
    }

    return null;
  }

  /**
   * View configuration. This method is called in the body section of the connector's view configuration page. Its purpose is to present the connection information to the user. The coder can presume
   * that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   */
  @Override
  public void viewConfiguration(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters) throws ManifoldCFException, IOException {
    out.print("<table class=\"displaytable\">\n" + "  <tr>\n" + "    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.Parameters")
        + "</nobr></td>\n" + "    <td class=\"value\" colspan=\"3\">\n");
    final Iterator iter = parameters.listParameters();
    while (iter.hasNext()) {
      final String param = (String) iter.next();
      final String value = parameters.getParameter(param);
      if (param.length() >= "password".length() && param.substring(param.length() - "password".length()).equalsIgnoreCase("password")) {
        out.print("      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=********</nobr><br/>\n");
      } else if (param.length() >= "keystore".length() && param.substring(param.length() - "keystore".length()).equalsIgnoreCase("keystore")) {
        final IKeystoreManager kmanager = KeystoreManagerFactory.make("", value);
        out.print("      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=&lt;" + Integer.toString(kmanager.getContents().length)
            + Messages.getBodyString(locale, "SharedDriveConnector.certificate") + "&gt;</nobr><br/>\n");
      } else {
        out.print("      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(value) + "</nobr><br/>\n");
      }
    }
    out.print("    </td>\n" + "  </tr>\n" + "</table>\n");
  }

  protected static void fillInGlobalFiltersSpecification(final Map<String, Object> paramMap, final Specification os) {

    final List<String> includeFileFilters = new ArrayList<>();
    final List<String> includeFolderFilters = new ArrayList<>();
    final List<String> excludeFilters = new ArrayList<>();

    for (int i = 0; i < os.getChildCount(); i++) {
      final SpecificationNode sn = os.getChild(i);

      if (sn.getType().equals(SharedDriveConnector.NODE_INCLUDEFILEFILTER)) {
        final String includeFileFilter = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REGEX);
        if (includeFileFilter != null) {
          includeFileFilters.add(includeFileFilter);
        }
      } else if (sn.getType().equals(SharedDriveConnector.NODE_INCLUDEFOLDERFILTER)) {
        final String includeFolderFilter = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REGEX);
        if (includeFolderFilter != null) {
          includeFolderFilters.add(includeFolderFilter);
        }
      } else if (sn.getType().equals(SharedDriveConnector.NODE_EXCLUDEFILTER)) {
        final String excludeFilter = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REGEX);
        if (excludeFilter != null) {
          excludeFilters.add(excludeFilter);
        }
      }
    }

    paramMap.put("INCLUDEFILEFILTERS", includeFileFilters);
    paramMap.put("INCLUDEFOLDERFILTERS", includeFolderFilters);
    paramMap.put("EXCLUDEFILTERS", excludeFilters);
  }

  protected static void fillInContentLengthSpecification(final Map<String, Object> paramMap, final Specification os) {

    String maxLength = null;
    String minLength = null;
    for (int i = 0; i < os.getChildCount(); i++) {
      final SpecificationNode sn = os.getChild(i);

      if (sn.getType().equals(SharedDriveConnector.NODE_MAXLENGTH)) {
        maxLength = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
      } else if (sn.getType().equals(SharedDriveConnector.NODE_MINLENGTH)) {
        minLength = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
      }
    }
    if (maxLength == null) {
      maxLength = "";
    }
    if (minLength == null) {
      minLength = "";
    }

    paramMap.put("MAXLENGTH", maxLength);
    paramMap.put("MINLENGTH", minLength);
  }

  /**
   * Output the specification header section. This method is called in the head section of a job page which has selected a repository connection of the current type. Its purpose is to add the required
   * tabs to the list, and to output any javascript methods that might be needed by the job editing HTML. The connector will be connected before this method can be called.
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the locale the output is preferred to be in.
   * @param ds                       is the current document specification for this job.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @param tabsArray                is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputSpecificationHeader(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber, final List<String> tabsArray)
      throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

    tabsArray.add(Messages.getString(locale, "SharedDriveConnector.Paths"));
    tabsArray.add(Messages.getString(locale, "SharedDriveConnector.GlobalFilters"));
    tabsArray.add(Messages.getString(locale, "SharedDriveConnector.Security"));
    tabsArray.add(Messages.getString(locale, "SharedDriveConnector.Metadata"));
    tabsArray.add(Messages.getString(locale, "SharedDriveConnector.ContentLength"));
    tabsArray.add(Messages.getString(locale, "SharedDriveConnector.FileMapping"));
    tabsArray.add(Messages.getString(locale, "SharedDriveConnector.URLMapping"));

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JS, paramMap);
  }

  /**
   * Output the specification body section. This method is called in the body section of a job page which has selected a repository connection of the current type. Its purpose is to present the
   * required form elements for editing. The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags. The name of the form is
   * always "editjob". The connector will be connected before this method can be called.
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the locale the output is preferred to be in.
   * @param ds                       is the current document specification for this job.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @param actualSequenceNumber     is the connection within the job that has currently been selected.
   * @param tabName                  is the current tab name. (actualSequenceNumber, tabName) form a unique tuple within the job.
   */
  @Override
  public void outputSpecificationBody(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber, final int actualSequenceNumber, final String tabName)
      throws ManifoldCFException, IOException {

    final Map<String, Object> paramMap = new HashMap<>();

    // Set the tab name
    paramMap.put("TABNAME", tabName);
    paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));
    paramMap.put("SELECTEDNUM", Integer.toString(actualSequenceNumber));

    // Fill in the field mapping tab data
    fillInGlobalFiltersSpecification(paramMap, ds);
    fillInContentLengthSpecification(paramMap, ds);

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_CONTENT_LENGTH_HTML, paramMap);
    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_GLOBAL_FILTERS_HTML, paramMap);

    final String seqPrefix = "s" + connectionSequenceNumber + "_";

    int i;
    int k;

    // Check for Paths tab
    if (tabName.equals(Messages.getString(locale, "SharedDriveConnector.Paths")) && connectionSequenceNumber == actualSequenceNumber) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n");
      // Now, loop through paths. There will be a row in the current table for
      // each one.
      // The row will contain a delete button on the left. On the right will be
      // the startpoint itself at the top,
      // and underneath it the table where the filter criteria are edited.
      i = 0;
      k = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i++);
        if (sn.getType().equals(SharedDriveConnector.NODE_STARTPOINT)) {
          final String pathDescription = "_" + Integer.toString(k);
          final String pathOpName = seqPrefix + "pathop" + pathDescription;
          final String startPath = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_PATH);
          out.print("  <tr>\n" + "    <td class=\"value\">\n" + "      <a name=\"" + seqPrefix + "path_" + Integer.toString(k) + "\">\n" + "        <input type=\"button\" value=\"Delete\" alt=\""
              + Messages.getAttributeString(locale, "SharedDriveConnector.DeletePath") + Integer.toString(k) + "\" onClick='Javascript:" + seqPrefix + "SpecOp(\"" + pathOpName + "\",\"Delete\",\""
              + seqPrefix + "path_" + Integer.toString(k) + "\")'/>\n" + "      </a>&nbsp;\n" + "    </td>\n" + "    <td class=\"value\">\n" + "      <table class=\"displaytable\">\n"
              + "        <tr>\n" + "          <td class=\"value\">\n" + "            <input type=\"hidden\" name=\"" + seqPrefix + "specpath" + pathDescription + "\" value=\""
              + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_PATH)) + "\"/>\n" + "            <input type=\"hidden\" name=\"" + pathOpName
              + "\" value=\"\"/>\n" + "            <nobr>" + (startPath.length() == 0 ? "(root)" : org.apache.manifoldcf.ui.util.Encoder.bodyEscape(startPath)) + "</nobr>\n" + "          </td>\n"
              + "        </tr>\n" + "        <tr>\n" + "          <td class=\"boxcell\">\n" + "            <table class=\"displaytable\">\n");
          // Now go through the include/exclude children of this node, and
          // display one line per node, followed
          // an "add" line.
          int j = 0;
          while (j < sn.getChildCount()) {
            final SpecificationNode excludeNode = sn.getChild(j);
            final String instanceDescription = "_" + Integer.toString(k) + "_" + Integer.toString(j);
            final String instanceOpName = seqPrefix + "specop" + instanceDescription;

            final String nodeFlavor = excludeNode.getType();
            String nodeType = excludeNode.getAttributeValue(SharedDriveConnector.ATTRIBUTE_TYPE);
            if (nodeType == null) {
              nodeType = "";
            }
            final String filespec = excludeNode.getAttributeValue(SharedDriveConnector.ATTRIBUTE_FILESPEC);
            String indexable = excludeNode.getAttributeValue(SharedDriveConnector.ATTRIBUTE_INDEXABLE);
            if (indexable == null) {
              indexable = "";
            }
            out.print("              <tr>\n" + "                <td class=\"value\">\n" + "                    <input type=\"button\" value=\"Insert\" onClick='Javascript:" + seqPrefix
                + "SpecInsertSpec(\"" + instanceDescription + "\",\"" + seqPrefix + "filespec_" + Integer.toString(k) + "_" + Integer.toString(j + 1) + "\")' alt=\""
                + Messages.getAttributeString(locale, "SharedDriveConnector.InsertNewMatchForPath") + Integer.toString(k) + " before position #" + Integer.toString(j) + "\"/>\n"
                + "                </td>\n" + "                <td class=\"value\">\n" + "                  <nobr>\n" + "                    <select name=\"" + seqPrefix + "specfl_i"
                + instanceDescription + "\">\n" + "                      <option value=\"include\">" + Messages.getBodyString(locale, "SharedDriveConnector.Include") + "</option>\n"
                + "                      <option value=\"exclude\">" + Messages.getBodyString(locale, "SharedDriveConnector.Exclude") + "</option>\n" + "                    </select>&nbsp;\n"
                + "                    <select name=\"" + seqPrefix + "spectin_i" + instanceDescription + "\">\n" + "                      <option value=\"\" selected=\"selected\">"
                + Messages.getBodyString(locale, "SharedDriveConnector.AnyFileOrDirectory") + "</option>\n" + "                      <option value=\"file\">"
                + Messages.getBodyString(locale, "SharedDriveConnector.files") + "</option>\n" + "                      <option value=\"indexable-file\">"
                + Messages.getBodyString(locale, "SharedDriveConnector.indexableFiles") + "</option>\n" + "                      <option value=\"unindexable-file\">"
                + Messages.getBodyString(locale, "SharedDriveConnector.unindexableFiles") + "</option>\n" + "                      <option value=\"directory\">"
                + Messages.getBodyString(locale, "SharedDriveConnector.directorys") + "</option>\n" + "                    </select>&nbsp;"
                + Messages.getBodyString(locale, "SharedDriveConnector.matching") + "&nbsp;\n" + "                    <input type=\"text\" size=\"20\" name=\"" + seqPrefix + "specfile_i"
                + instanceDescription + "\" value=\"\"/>\n" + "                  </nobr>\n" + "                </td>\n" + "\n" + "              </tr>\n" + "              <tr>\n"
                + "                <td class=\"value\">\n" + "                  <a name=\"" + seqPrefix + "filespec_" + Integer.toString(k) + "_" + Integer.toString(j) + "\">\n"
                + "                    <input type=\"button\" value=\"Delete\" onClick='Javascript:" + seqPrefix + "SpecOp(\"" + instanceOpName + "\",\"Delete\",\"" + seqPrefix + "filespec_"
                + Integer.toString(k) + "_" + Integer.toString(j) + "\")' alt=\"" + Messages.getAttributeString(locale, "SharedDriveConnector.DeletePath") + Integer.toString(k)
                + Messages.getAttributeString(locale, "SharedDriveConnector.matchSpec") + Integer.toString(j) + "\"/>\n" + "                  </a>\n" + "                </td>\n"
                + "                <td class=\"value\">\n" + "                  <nobr>\n" + "                    <input type=\"hidden\" name=\"" + instanceOpName + "\" value=\"\"/>\n"
                + "                    <input type=\"hidden\" name=\"" + seqPrefix + "specfl" + instanceDescription + "\" value=\"" + nodeFlavor + "\"/>\n"
                + "                    <input type=\"hidden\" name=\"" + seqPrefix + "specty" + instanceDescription + "\" value=\"" + nodeType + "\"/>\n"
                + "                    <input type=\"hidden\" name=\"" + seqPrefix + "specin" + instanceDescription + "\" value=\"" + indexable + "\"/>\n"
                + "                    <input type=\"hidden\" name=\"" + seqPrefix + "specfile" + instanceDescription + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(filespec)
                + "\"/>\n" + "                    " + Integer.toString(j + 1) + ".&nbsp;" + (nodeFlavor.equals("include") ? "Include" : "") + "" + (nodeFlavor.equals("exclude") ? "Exclude" : "") + ""
                + (indexable.equals("yes") ? "&nbsp;indexable" : "") + "" + (indexable.equals("no") ? "&nbsp;un-indexable" : "") + "" + (nodeType.equals("file") ? "&nbsp;file(s)" : "") + ""
                + (nodeType.equals("directory") ? "&nbsp;directory(s)" : "") + "" + (nodeType.equals("") ? "&nbsp;file(s)&nbsp;or&nbsp;directory(s)" : "") + "&nbsp;matching&nbsp;"
                + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(filespec) + "\n" + "                  </nobr>\n" + "                </td>\n" + "              </tr>\n");
            j++;
          }
          if (j == 0) {
            out.print("              <tr><td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoRulesDefined") + "</td></tr>\n");
          }
          out.print("              <tr><td class=\"lightseparator\" colspan=\"2\"><hr/></td></tr>\n" + "              <tr>\n" + "                <td class=\"value\">\n"
              + "                  <input type=\"hidden\" name=\"" + seqPrefix + "specchildcount" + pathDescription + "\" value=\"" + Integer.toString(j) + "\"/>\n" + "                  <a name=\""
              + seqPrefix + "filespec_" + Integer.toString(k) + "_" + Integer.toString(j) + "\">\n" + "                    <input type=\"button\" value=\"Add\" onClick='Javascript:" + seqPrefix
              + "SpecAddSpec(\"" + pathDescription + "\",\"" + seqPrefix + "filespec_" + Integer.toString(k) + "_" + Integer.toString(j + 1) + "\")' alt=\""
              + Messages.getAttributeString(locale, "SharedDriveConnector.AddNewMatchForPath") + Integer.toString(k) + "\"/>\n" + "                  </a>\n" + "                </td>\n"
              + "                <td class=\"value\">\n" + "                  <nobr>\n" + "                    <select name=\"" + seqPrefix + "specfl" + pathDescription + "\">\n"
              + "                      <option value=\"include\">" + Messages.getBodyString(locale, "SharedDriveConnector.Include") + "</option>\n" + "                      <option value=\"exclude\">"
              + Messages.getBodyString(locale, "SharedDriveConnector.Exclude") + "</option>\n" + "                    </select>&nbsp;\n" + "                    <select name=\"" + seqPrefix + "spectin"
              + pathDescription + "\">\n" + "                      <option value=\"\">" + Messages.getBodyString(locale, "SharedDriveConnector.AnyFileOrDirectory") + "</option>\n"
              + "                      <option value=\"file\">" + Messages.getBodyString(locale, "SharedDriveConnector.files") + "</option>\n"
              + "                      <option value=\"indexable-file\">" + Messages.getBodyString(locale, "SharedDriveConnector.indexableFiles") + "</option>\n"
              + "                      <option value=\"unindexable-file\">" + Messages.getBodyString(locale, "SharedDriveConnector.unindexableFiles") + "</option>\n"
              + "                      <option value=\"directory\">" + Messages.getBodyString(locale, "SharedDriveConnector.directorys") + "</option>\n" + "                    </select>&nbsp;"
              + Messages.getBodyString(locale, "SharedDriveConnector.matching") + "&nbsp;\n" + "                    <input type=\"text\" size=\"20\" name=\"" + seqPrefix + "specfile" + pathDescription
              + "\" value=\"\"/>\n" + "                  </nobr>\n" + "                </td>\n" + "              </tr>\n" + "            </table>\n" + "          </td>\n" + "        </tr>\n"
              + "      </table>\n" + "    </td>\n" + "  </tr>\n");
          k++;
        }
      }
      if (k == 0) {
        out.print("  <tr>\n" + "    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoStartingPointsDefined") + "</td>\n" + "  </tr>\n");
      }
      out.print(
          "  <tr><td class=\"lightseparator\" colspan=\"2\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"value\" colspan=\"2\">\n" + "      <nobr>\n" + "        <input type=\"hidden\" name=\""
              + seqPrefix + "pathcount\" value=\"" + Integer.toString(k) + "\"/>\n" + "        <a name=\"" + seqPrefix + "path_" + Integer.toString(k) + "\">\n");

      String pathSoFar = (String) currentContext.get(seqPrefix + "specpath");
      if (pathSoFar == null) {
        pathSoFar = "";
      }

      // Grab next folder/project list
      try {
        String[] childList;
        childList = getChildFolderNames(pathSoFar);
        if (childList == null) {
          // Illegal path - set it back
          pathSoFar = "";
          childList = getChildFolderNames("");
          if (childList == null) {
            throw new ManifoldCFException("Can't find any children for root folder");
          }
        }
        out.print("          <input type=\"hidden\" name=\"" + seqPrefix + "specpath\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pathSoFar) + "\"/>\n"
            + "          <input type=\"hidden\" name=\"" + seqPrefix + "pathop\" value=\"\"/>\n" + "          <input type=\"button\" value=\"Add\" alt=\""
            + Messages.getAttributeString(locale, "SharedDriveConnector.AddPath") + "\" onClick='Javascript:" + seqPrefix + "SpecOp(\"" + seqPrefix + "pathop\",\"Add\",\"" + seqPrefix + "path_"
            + Integer.toString(k + 1) + "\")'/>\n" + "          &nbsp;" + (pathSoFar.length() == 0 ? "(root)" : org.apache.manifoldcf.ui.util.Encoder.bodyEscape(pathSoFar)) + "\n");
        if (pathSoFar.length() > 0) {
          out.print("          <input type=\"button\" value=\"-\" alt=\"" + Messages.getAttributeString(locale, "SharedDriveConnector.RemoveFromPath") + "\" onClick='Javascript:" + seqPrefix
              + "SpecOp(\"" + seqPrefix + "pathop\",\"Up\",\"" + seqPrefix + "path_" + Integer.toString(k) + "\")'/>\n");
        }
        if (childList.length > 0) {
          out.print("          <nobr>\n" + "            <input type=\"button\" value=\"+\" alt=\"" + Messages.getAttributeString(locale, "SharedDriveConnector.AddPath") + "\" onClick='Javascript:"
              + seqPrefix + "SpecAddToPath(\"" + seqPrefix + "path_" + Integer.toString(k) + "\")'/>&nbsp;\n" + "            <select name=\"" + seqPrefix + "pathaddon\">\n"
              + "              <option value=\"\" selected=\"selected\">" + Messages.getBodyString(locale, "SharedDriveConnector.PickAFolder") + "</option>\n");
          int j = 0;
          while (j < childList.length) {
            final String folder = org.apache.manifoldcf.ui.util.Encoder.attributeEscape(childList[j]);
            out.print("              <option value=\"" + folder + "\">" + folder + "</option>\n");
            j++;
          }
          out.print("            </select>" + Messages.getBodyString(locale, "SharedDriveConnector.orTypeAPath") + "            <input type=\"text\" name=\"" + seqPrefix
              + "pathtypein\" size=\"16\" value=\"\"/>\n" + "          </nobr>\n");
        }
      } catch (final ManifoldCFException e) {
        e.printStackTrace();
        out.println(org.apache.manifoldcf.ui.util.Encoder.bodyEscape(e.getMessage()));
      }
      out.print("        </a>\n" + "      </nobr>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      // Generate hiddens for the pathspec tab
      i = 0;
      k = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i++);
        if (sn.getType().equals(SharedDriveConnector.NODE_STARTPOINT)) {
          final String pathDescription = "_" + Integer.toString(k);
          final String startPath = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_PATH);
          out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specpath" + pathDescription + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(startPath) + "\"/>\n");
          // Now go through the include/exclude children of this node.
          int j = 0;
          while (j < sn.getChildCount()) {
            final SpecificationNode excludeNode = sn.getChild(j);
            final String instanceDescription = "_" + Integer.toString(k) + "_" + Integer.toString(j);

            final String nodeFlavor = excludeNode.getType();
            String nodeType = excludeNode.getAttributeValue(SharedDriveConnector.ATTRIBUTE_TYPE);
            if (nodeType == null) {
              nodeType = "";
            }
            final String filespec = excludeNode.getAttributeValue(SharedDriveConnector.ATTRIBUTE_FILESPEC);
            String indexable = excludeNode.getAttributeValue(SharedDriveConnector.ATTRIBUTE_INDEXABLE);
            if (indexable == null) {
              indexable = "";
            }
            out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specfl" + instanceDescription + "\" value=\"" + nodeFlavor + "\"/>\n" + "<input type=\"hidden\" name=\"" + seqPrefix + "specty"
                + instanceDescription + "\" value=\"" + nodeType + "\"/>\n" + "<input type=\"hidden\" name=\"" + seqPrefix + "specin" + instanceDescription + "\" value=\"" + indexable + "\"/>\n"
                + "<input type=\"hidden\" name=\"" + seqPrefix + "specfile" + instanceDescription + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(filespec) + "\"/>\n");
            j++;
          }
          k++;
          out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specchildcount" + pathDescription + "\" value=\"" + Integer.toString(j) + "\"/>\n");
        }
      }
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "pathcount\" value=\"" + Integer.toString(k) + "\"/>\n");
    }

    // Security tab

    // Find whether security is on or off
    i = 0;
    boolean securityOn = true;
    boolean shareSecurityOn = true;
    boolean parentFolderSecurityOn = false;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_SECURITY)) {
        final String securityValue = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
        if (securityValue.equals("off")) {
          securityOn = false;
        } else if (securityValue.equals("on")) {
          securityOn = true;
        }
      }
      if (sn.getType().equals(SharedDriveConnector.NODE_SHARESECURITY)) {
        final String securityValue = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
        if (securityValue.equals("off")) {
          shareSecurityOn = false;
        } else if (securityValue.equals("on")) {
          shareSecurityOn = true;
        }
      }
      if (sn.getType().equals(SharedDriveConnector.NODE_PARENTFOLDERSECURITY)) {
        final String securityValue = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
        if (securityValue.equals("off")) {
          parentFolderSecurityOn = false;
        } else if (securityValue.equals("on")) {
          parentFolderSecurityOn = true;
        }
      }
    }

    if (tabName.equals(Messages.getString(locale, "SharedDriveConnector.Security")) && connectionSequenceNumber == actualSequenceNumber) {
      out.print("<table class=\"displaytable\">\n" + "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n" + "\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
          + Messages.getBodyString(locale, "SharedDriveConnector.FileSecurity") + "</nobr></td>\n" + "    <td colspan=\"3\" class=\"value\">\n" + "      <nobr>\n"
          + "        <input type=\"radio\" name=\"" + seqPrefix + "specsecurity\" value=\"on\" " + (securityOn ? "checked=\"true\"" : "") + " />"
          + Messages.getBodyString(locale, "SharedDriveConnector.Enabled") + "&nbsp;\n" + "        <input type=\"radio\" name=\"" + seqPrefix + "specsecurity\" value=\"off\" "
          + (securityOn == false ? "checked=\"true\"" : "") + " />" + Messages.getBodyString(locale, "SharedDriveConnector.Disabled") + "\n" + "      </nobr>\n" + "    </td>\n" + "  </tr>\n" + "\n"
          + "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n" + "\n");
      // Finally, go through forced ACL
      i = 0;
      k = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i++);
        if (sn.getType().equals(SharedDriveConnector.NODE_ACCESS)) {
          final String accessDescription = "_" + Integer.toString(k);
          final String accessOpName = seqPrefix + "accessop" + accessDescription;
          final String token = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_TOKEN);
          out.print("  <tr>\n" + "    <td class=\"description\" colspan=\"1\">\n" + "      <input type=\"hidden\" name=\"" + accessOpName + "\" value=\"\"/>\n" + "      <input type=\"hidden\" name=\""
              + seqPrefix + "spectoken" + accessDescription + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(token) + "\"/>\n" + "      <a name=\"" + seqPrefix + "token_"
              + Integer.toString(k) + "\">\n" + "        <input type=\"button\" value=\"Delete\" alt=\"" + Messages.getAttributeString(locale, "SharedDriveConnector.DeleteToken") + Integer.toString(k)
              + "\" onClick='Javascript:" + seqPrefix + "SpecOp(\"" + accessOpName + "\",\"Delete\",\"" + seqPrefix + "token_" + Integer.toString(k) + "\")'/>\n" + "      </a>\n" + "    </td>\n"
              + "    <td class=\"value\" colspan=\"3\">\n" + "      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(token) + "</nobr>\n" + "    </td>\n" + "  </tr>\n");
          k++;
        }
      }
      if (k == 0) {
        out.print("  <tr>\n" + "    <td class=\"message\" colspan=\"4\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoFileAccessTokensPresent") + "</td>\n" + "  </tr>\n");
      }
      out.print("  <tr><td class=\"lightseparator\" colspan=\"4\"><hr/></td></tr>\n" + "  <tr>\n" + "    <td class=\"description\" colspan=\"1\">\n" + "      <input type=\"hidden\" name=\""
          + seqPrefix + "tokencount\" value=\"" + Integer.toString(k) + "\"/>\n" + "      <input type=\"hidden\" name=\"" + seqPrefix + "accessop\" value=\"\"/>\n" + "      <a name=\"" + seqPrefix
          + "token_" + Integer.toString(k) + "\">\n" + "        <input type=\"button\" value=\"Add\" alt=\"" + Messages.getAttributeString(locale, "SharedDriveConnector.AddToken")
          + "\" onClick='Javascript:" + seqPrefix + "SpecAddToken(\"" + seqPrefix + "token_" + Integer.toString(k + 1) + "\")'/>\n" + "      </a>\n" + "    </td>\n"
          + "    <td class=\"value\" colspan=\"3\">\n" + "      <nobr><input type=\"text\" size=\"30\" name=\"" + seqPrefix + "spectoken\" value=\"\"/></nobr>\n" + "    </td>\n" + "  </tr>\n" + "\n"
          + "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n" + "\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
          + Messages.getBodyString(locale, "SharedDriveConnector.ShareSecurity") + "</nobr></td>\n" + "    <td colspan=\"3\" class=\"value\">\n" + "      <nobr>\n"
          + "        <input type=\"radio\" name=\"" + seqPrefix + "specsharesecurity\" value=\"on\" " + (shareSecurityOn ? "checked=\"true\"" : "") + " />"
          + Messages.getBodyString(locale, "SharedDriveConnector.Enabled") + "&nbsp;\n" + "        <input type=\"radio\" name=\"" + seqPrefix + "specsharesecurity\" value=\"off\" "
          + (shareSecurityOn == false ? "checked=\"true\"" : "") + " />" + Messages.getBodyString(locale, "SharedDriveConnector.Disabled") + "\n" + "      </nobr>\n" + "    </td>\n" + "  </tr>\n"
          + "\n" + "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n" + "\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
          + Messages.getBodyString(locale, "SharedDriveConnector.ParentFolderSecurity") + "</nobr></td>\n" + "    <td colspan=\"3\" class=\"value\">\n" + "      <nobr>\n"
          + "        <input type=\"radio\" name=\"" + seqPrefix + "specparentfoldersecurity\" value=\"on\" " + (parentFolderSecurityOn ? "checked=\"true\"" : "") + " />"
          + Messages.getBodyString(locale, "SharedDriveConnector.Enabled") + "&nbsp;\n" + "        <input type=\"radio\" name=\"" + seqPrefix + "specparentfoldersecurity\" value=\"off\" "
          + (parentFolderSecurityOn == false ? "checked=\"true\"" : "") + " />" + Messages.getBodyString(locale, "SharedDriveConnector.Disabled") + "\n" + "      </nobr>\n" + "    </td>\n"
          + "  </tr>\n" + "\n" + "</table>\n");
    } else {
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specsecurity\" value=\"" + (securityOn ? "on" : "off") + "\"/>\n");
      // Finally, go through forced ACL
      i = 0;
      k = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i++);
        if (sn.getType().equals(SharedDriveConnector.NODE_ACCESS)) {
          final String accessDescription = "_" + Integer.toString(k);
          final String token = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_TOKEN);
          out.print("<input type=\"hidden\" name=\"" + seqPrefix + "spectoken" + accessDescription + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(token) + "\"/>\n");
          k++;
        }
      }
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "tokencount\" value=\"" + Integer.toString(k) + "\"/>\n" + "<input type=\"hidden\" name=\"" + seqPrefix + "specsharesecurity\" value=\""
          + (shareSecurityOn ? "on" : "off") + "\"/>\n" + "<input type=\"hidden\" name=\"" + seqPrefix + "specparentfoldersecurity\" value=\"" + (parentFolderSecurityOn ? "on" : "off") + "\"/>\n");
    }

    // Metadata tab

    // Find the path-value metadata attribute name
    // Find the path-value mapping data
    i = 0;
    String pathNameAttribute = "";
    final MatchMap matchMap = new MatchMap();
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_PATHNAMEATTRIBUTE)) {
        pathNameAttribute = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
      } else if (sn.getType().equals(SharedDriveConnector.NODE_PATHMAP)) {
        final String pathMatch = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_MATCH);
        final String pathReplace = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REPLACE);
        matchMap.appendMatchPair(pathMatch, pathReplace);
      }
    }

    if (tabName.equals(Messages.getString(locale, "SharedDriveConnector.Metadata")) && connectionSequenceNumber == actualSequenceNumber) {
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specmappingcount\" value=\"" + Integer.toString(matchMap.getMatchCount()) + "\"/>\n" + "<input type=\"hidden\" name=\"" + seqPrefix
          + "specmappingop\" value=\"\"/>\n" + "<table class=\"displaytable\">\n" + "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n" + "\n" + "  <tr>\n"
          + "    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.PathAttributeName") + "</nobr></td>\n"
          + "    <td class=\"value\" colspan=\"3\">\n" + "      <input type=\"text\" name=\"" + seqPrefix + "specpathnameattribute\" size=\"20\" value=\""
          + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pathNameAttribute) + "\"/>\n" + "    </td>\n" + "  </tr>\n" + "\n" + "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n"
          + "\n");
      i = 0;
      while (i < matchMap.getMatchCount()) {
        final String matchString = matchMap.getMatchString(i);
        final String replaceString = matchMap.getReplaceString(i);
        out.print("  <tr>\n" + "    <td class=\"value\">\n" + "      <input type=\"hidden\" name=\"" + seqPrefix + "specmappingop_" + Integer.toString(i) + "\" value=\"\"/>\n" + "      <a name=\""
            + seqPrefix + "mapping_" + Integer.toString(i) + "\">\n" + "        <input type=\"button\" onClick='Javascript:" + seqPrefix + "SpecOp(\"" + seqPrefix + "specmappingop_"
            + Integer.toString(i) + "\",\"Delete\",\"" + seqPrefix + "mapping_" + Integer.toString(i) + "\")' alt=\"" + Messages.getAttributeString(locale, "SharedDriveConnector.DeleteMapping")
            + Integer.toString(i) + "\" value=\"Delete\"/>\n" + "      </a>\n" + "    </td>\n" + "    <td class=\"value\"><input type=\"hidden\" name=\"" + seqPrefix + "specmatch_"
            + Integer.toString(i) + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString) + "\"/>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString)
            + "</td>\n" + "    <td class=\"value\">==></td>\n" + "    <td class=\"value\"><input type=\"hidden\" name=\"" + seqPrefix + "specreplace_" + Integer.toString(i) + "\" value=\""
            + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString) + "\"/>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString) + "</td>\n" + "  </tr>\n");
        i++;
      }
      if (i == 0) {
        out.print("  <tr><td colspan=\"4\" class=\"message\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoMappingsSpecified") + "</td></tr>\n");
      }
      out.print("  <tr><td class=\"lightseparator\" colspan=\"4\"><hr/></td></tr>\n" + "\n" + "  <tr>\n" + "    <td class=\"value\">\n" + "      <a name=\"" + seqPrefix + "mapping_"
          + Integer.toString(i) + "\">\n" + "        <input type=\"button\" onClick='Javascript:" + seqPrefix + "SpecAddMapping(\"" + seqPrefix + "mapping_" + Integer.toString(i + 1) + "\")' alt=\""
          + Messages.getAttributeString(locale, "SharedDriveConnector.AddToMappings") + "\" value=\"Add\"/>\n" + "      </a>\n" + "    </td>\n" + "    <td class=\"value\"><nobr>"
          + Messages.getBodyString(locale, "SharedDriveConnector.MatchRegexp") + "<input type=\"text\" name=\"" + seqPrefix + "specmatch\" size=\"32\" value=\"\"/></nobr></td>\n"
          + "    <td class=\"value\">==></td>\n" + "    <td class=\"value\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.ReplaceString") + "<input type=\"text\" name=\"" + seqPrefix
          + "specreplace\" size=\"32\" value=\"\"/></nobr></td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specpathnameattribute\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pathNameAttribute) + "\"/>\n"
          + "<input type=\"hidden\" name=\"" + seqPrefix + "specmappingcount\" value=\"" + Integer.toString(matchMap.getMatchCount()) + "\"/>\n");
      i = 0;
      while (i < matchMap.getMatchCount()) {
        final String matchString = matchMap.getMatchString(i);
        final String replaceString = matchMap.getReplaceString(i);
        out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specmatch_" + Integer.toString(i) + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString) + "\"/>\n"
            + "<input type=\"hidden\" name=\"" + seqPrefix + "specreplace_" + Integer.toString(i) + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString) + "\"/>\n");
        i++;
      }
    }

    // File and URL Mapping tabs

    // Find the filename mapping data
    // Find the URL mapping data
    final MatchMap fileMap = new MatchMap();
    final MatchMap uriMap = new MatchMap();
    i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_FILEMAP)) {
        final String pathMatch = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_MATCH);
        final String pathReplace = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REPLACE);
        fileMap.appendMatchPair(pathMatch, pathReplace);
      } else if (sn.getType().equals(SharedDriveConnector.NODE_URIMAP)) {
        final String pathMatch = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_MATCH);
        final String pathReplace = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REPLACE);
        uriMap.appendMatchPair(pathMatch, pathReplace);
      }
    }

    if (tabName.equals(Messages.getString(locale, "SharedDriveConnector.FileMapping")) && connectionSequenceNumber == actualSequenceNumber) {
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specfmapcount\" value=\"" + Integer.toString(fileMap.getMatchCount()) + "\"/>\n" + "<input type=\"hidden\" name=\"" + seqPrefix
          + "specfmapop\" value=\"\"/>\n" + "<table class=\"displaytable\">\n" + "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n");
      i = 0;
      while (i < fileMap.getMatchCount()) {
        final String matchString = fileMap.getMatchString(i);
        final String replaceString = fileMap.getReplaceString(i);
        out.print("  <tr>\n" + "    <td class=\"value\">\n" + "      <input type=\"hidden\" name=\"" + seqPrefix + "specfmapop_" + Integer.toString(i) + "\" value=\"\"/>\n" + "      <a name=\""
            + seqPrefix + "fmap_" + Integer.toString(i) + "\">\n" + "        <input type=\"button\" onClick='Javascript:" + seqPrefix + "SpecOp(\"" + seqPrefix + "specfmapop_" + Integer.toString(i)
            + "\",\"Delete\",\"" + seqPrefix + "fmap_" + Integer.toString(i) + "\")' alt=\"" + Messages.getAttributeString(locale, "SharedDriveConnector.DeleteFileMapping") + Integer.toString(i)
            + "\" value=\"Delete\"/>\n" + "      </a>\n" + "    </td>\n" + "    <td class=\"value\"><input type=\"hidden\" name=\"" + seqPrefix + "specfmapmatch_" + Integer.toString(i) + "\" value=\""
            + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString) + "\"/>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString) + "</td>\n"
            + "    <td class=\"value\">==></td>\n" + "    <td class=\"value\"><input type=\"hidden\" name=\"" + seqPrefix + "specfmapreplace_" + Integer.toString(i) + "\" value=\""
            + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString) + "\"/>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString) + "</td>\n" + "  </tr>\n");
        i++;
      }
      if (i == 0) {
        out.print("  <tr><td colspan=\"4\" class=\"message\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoFileMappingsSpecified") + "</td></tr>\n");
      }
      out.print("  <tr><td class=\"lightseparator\" colspan=\"4\"><hr/></td></tr>\n" + "\n" + "  <tr>\n" + "    <td class=\"value\">\n" + "      <a name=\"" + seqPrefix + "fmap_" + Integer.toString(i)
          + "\">\n" + "        <input type=\"button\" onClick='Javascript:" + seqPrefix + "SpecAddFMap(\"" + seqPrefix + "fmap_" + Integer.toString(i + 1) + "\")' alt=\""
          + Messages.getAttributeString(locale, "SharedDriveConnector.AddToFileMappings") + "\" value=\"Add\"/>\n" + "      </a>\n" + "    </td>\n" + "    <td class=\"value\">"
          + Messages.getBodyString(locale, "SharedDriveConnector.MatchRegexp") + "<input type=\"text\" name=\"" + seqPrefix + "specfmapmatch\" size=\"32\" value=\"\"/></td>\n"
          + "    <td class=\"value\">==></td>\n" + "    <td class=\"value\">" + Messages.getBodyString(locale, "SharedDriveConnector.ReplaceString") + "<input type=\"text\" name=\"" + seqPrefix
          + "specfmapreplace\" size=\"32\" value=\"\"/></td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specfmapcount\" value=\"" + Integer.toString(fileMap.getMatchCount()) + "\"/>\n");
      i = 0;
      while (i < fileMap.getMatchCount()) {
        final String matchString = fileMap.getMatchString(i);
        final String replaceString = fileMap.getReplaceString(i);
        out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specfmapmatch_" + Integer.toString(i) + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString) + "\"/>\n"
            + "<input type=\"hidden\" name=\"" + seqPrefix + "specfmapreplace_" + Integer.toString(i) + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString)
            + "\"/>\n");
        i++;
      }
    }

    if (tabName.equals(Messages.getString(locale, "SharedDriveConnector.URLMapping")) && connectionSequenceNumber == actualSequenceNumber) {
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specumapcount\" value=\"" + Integer.toString(uriMap.getMatchCount()) + "\"/>\n" + "<input type=\"hidden\" name=\"" + seqPrefix
          + "specumapop\" value=\"\"/>\n" + "<table class=\"displaytable\">\n" + "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n");
      i = 0;
      while (i < uriMap.getMatchCount()) {
        final String matchString = uriMap.getMatchString(i);
        final String replaceString = uriMap.getReplaceString(i);
        out.print("  <tr>\n" + "    <td class=\"value\">\n" + "      <input type=\"hidden\" name=\"" + seqPrefix + "specumapop_" + Integer.toString(i) + "\" value=\"\"/>\n" + "      <a name=\""
            + seqPrefix + "umap_" + Integer.toString(i) + "\">\n" + "        <input type=\"button\" onClick='Javascript:" + seqPrefix + "SpecOp(\"" + seqPrefix + "specumapop_" + Integer.toString(i)
            + "\",\"Delete\",\"" + seqPrefix + "umap_" + Integer.toString(i) + "\")' alt=\"" + Messages.getAttributeString(locale, "SharedDriveConnector.DeleteUrlMapping") + Integer.toString(i)
            + "\" value=\"Delete\"/>\n" + "      </a>\n" + "    </td>\n" + "    <td class=\"value\">\n" + "      <input type=\"hidden\" name=\"" + seqPrefix + "specumapmatch_" + Integer.toString(i)
            + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString) + "\"/>\n" + "      " + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString) + "\n"
            + "    </td>\n" + "    <td class=\"value\">==></td>\n" + "    <td class=\"value\">\n" + "      <input type=\"hidden\" name=\"" + seqPrefix + "specumapreplace_" + Integer.toString(i)
            + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString) + "\"/>\n" + "      " + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString) + "\n"
            + "    </td>\n" + "  </tr>\n");
        i++;
      }
      if (i == 0) {
        out.print("  <tr><td colspan=\"4\" class=\"message\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoURLMappingsSpecifiedWillProduceAFileIRI") + "</td></tr>\n");
      }
      out.print("  <tr><td class=\"lightseparator\" colspan=\"4\"><hr/></td></tr>\n" + "      \n" + "  <tr>\n" + "    <td class=\"value\">\n" + "      <a name=\"" + seqPrefix + "umap_"
          + Integer.toString(i) + "\">\n" + "        <input type=\"button\" onClick='Javascript:" + seqPrefix + "SpecAddUMap(\"" + seqPrefix + "umap_" + Integer.toString(i + 1) + "\")' alt=\""
          + Messages.getAttributeString(locale, "SharedDriveConnector.AddToURLMappings") + "\" value=\"Add\"/>\n" + "      </a>\n" + "    </td>\n" + "    <td class=\"value\">"
          + Messages.getBodyString(locale, "SharedDriveConnector.MatchRegexp") + "<input type=\"text\" name=\"" + seqPrefix + "specumapmatch\" size=\"32\" value=\"\"/></td>\n"
          + "    <td class=\"value\">==></td>\n" + "    <td class=\"value\">" + Messages.getBodyString(locale, "SharedDriveConnector.ReplaceString") + "<input type=\"text\" name=\"" + seqPrefix
          + "specumapreplace\" size=\"32\" value=\"\"/></td>\n" + "  </tr>\n" + "</table>\n");
    } else {
      out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specumapcount\" value=\"" + Integer.toString(uriMap.getMatchCount()) + "\"/>\n");
      i = 0;
      while (i < uriMap.getMatchCount()) {
        final String matchString = uriMap.getMatchString(i);
        final String replaceString = uriMap.getReplaceString(i);
        out.print("<input type=\"hidden\" name=\"" + seqPrefix + "specumapmatch_" + Integer.toString(i) + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString) + "\"/>\n"
            + "<input type=\"hidden\" name=\"" + seqPrefix + "specumapreplace_" + Integer.toString(i) + "\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString)
            + "\"/>\n");
        i++;
      }
    }
  }

  /**
   * Process a specification post. This method is called at the start of job's edit or view page, whenever there is a possibility that form data for a connection has been posted. Its purpose is to
   * gather form information and modify the document specification accordingly. The name of the posted form is always "editjob". The connector will be connected before this method can be called.
   *
   * @param variableContext          contains the post data, including binary file-upload information.
   * @param locale                   is the locale the output is preferred to be in.
   * @param ds                       is the current document specification for this job.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the job (and cause a redirection to an error page).
   */
  @Override
  public String processSpecificationPost(final IPostParameters variableContext, final Locale locale, final Specification ds, final int connectionSequenceNumber) throws ManifoldCFException {
    final String seqPrefix = "s" + connectionSequenceNumber + "_";

    String x = variableContext.getParameter(seqPrefix + "pathcount");
    if (x != null) {
      // Delete all path specs first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_STARTPOINT)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      // Find out how many children were sent
      final int pathCount = Integer.parseInt(x);
      // Gather up these
      i = 0;
      while (i < pathCount) {
        final String pathDescription = "_" + Integer.toString(i);
        final String pathOpName = seqPrefix + "pathop" + pathDescription;
        x = variableContext.getParameter(pathOpName);
        if (x != null && x.equals("Delete")) {
          // Skip to the next
          i++;
          continue;
        }
        // Path inserts won't happen until the very end
        final String path = variableContext.getParameter(seqPrefix + "specpath" + pathDescription);
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_STARTPOINT);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_PATH, path);

        // Now, get the number of children
        final String y = variableContext.getParameter(seqPrefix + "specchildcount" + pathDescription);
        final int childCount = Integer.parseInt(y);
        int j = 0;
        int w = 0;
        while (j < childCount) {
          final String instanceDescription = "_" + Integer.toString(i) + "_" + Integer.toString(j);
          // Look for an insert or a delete at this point
          final String instanceOp = seqPrefix + "specop" + instanceDescription;
          final String z = variableContext.getParameter(instanceOp);
          String flavor;
          String type;
          String indexable;
          String match;
          SpecificationNode sn;
          if (z != null && z.equals("Delete")) {
            // Process the deletion as we gather
            j++;
            continue;
          }
          if (z != null && z.equals("Insert Here")) {
            // Process the insertion as we gather.
            flavor = variableContext.getParameter(seqPrefix + "specfl_i" + instanceDescription);
            indexable = "";
            type = "";
            final String xxx = variableContext.getParameter(seqPrefix + "spectin_i" + instanceDescription);
            if (xxx.equals("file") || xxx.equals("directory")) {
              type = xxx;
            } else if (xxx.equals("indexable-file")) {
              indexable = "yes";
              type = "file";
            } else if (xxx.equals("unindexable-file")) {
              indexable = "no";
              type = "file";
            }

            match = variableContext.getParameter(seqPrefix + "specfile_i" + instanceDescription);
            sn = new SpecificationNode(flavor);
            if (type != null && type.length() > 0) {
              sn.setAttribute(SharedDriveConnector.ATTRIBUTE_TYPE, type);
            }
            if (indexable != null && indexable.length() > 0) {
              sn.setAttribute(SharedDriveConnector.ATTRIBUTE_INDEXABLE, indexable);
            }
            sn.setAttribute(SharedDriveConnector.ATTRIBUTE_FILESPEC, match);
            node.addChild(w++, sn);
          }
          flavor = variableContext.getParameter(seqPrefix + "specfl" + instanceDescription);
          type = variableContext.getParameter(seqPrefix + "specty" + instanceDescription);
          match = variableContext.getParameter(seqPrefix + "specfile" + instanceDescription);
          indexable = variableContext.getParameter(seqPrefix + "specin" + instanceDescription);
          sn = new SpecificationNode(flavor);
          if (type != null && type.length() > 0) {
            sn.setAttribute(SharedDriveConnector.ATTRIBUTE_TYPE, type);
          }
          if (indexable != null && indexable.length() > 0) {
            sn.setAttribute(SharedDriveConnector.ATTRIBUTE_INDEXABLE, indexable);
          }
          sn.setAttribute(SharedDriveConnector.ATTRIBUTE_FILESPEC, match);
          node.addChild(w++, sn);
          j++;
        }
        if (x != null && x.equals("Add")) {
          // Process adds to the end of the rules in-line
          final String match = variableContext.getParameter(seqPrefix + "specfile" + pathDescription);
          String indexable = "";
          String type = "";
          final String xxx = variableContext.getParameter(seqPrefix + "spectin" + pathDescription);
          if (xxx.equals("file") || xxx.equals("directory")) {
            type = xxx;
          } else if (xxx.equals("indexable-file")) {
            indexable = "yes";
            type = "file";
          } else if (xxx.equals("unindexable-file")) {
            indexable = "no";
            type = "file";
          }

          final String flavor = variableContext.getParameter(seqPrefix + "specfl" + pathDescription);
          final SpecificationNode sn = new SpecificationNode(flavor);
          if (type != null && type.length() > 0) {
            sn.setAttribute(SharedDriveConnector.ATTRIBUTE_TYPE, type);
          }
          if (indexable != null && indexable.length() > 0) {
            sn.setAttribute(SharedDriveConnector.ATTRIBUTE_INDEXABLE, indexable);
          }
          sn.setAttribute(SharedDriveConnector.ATTRIBUTE_FILESPEC, match);
          node.addChild(w, sn);
        }

        ds.addChild(ds.getChildCount(), node);
        i++;
      }

      // See if there's a global add operation
      final String op = variableContext.getParameter(seqPrefix + "pathop");
      if (op != null && op.equals("Add")) {
        final String path = variableContext.getParameter(seqPrefix + "specpath");
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_STARTPOINT);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_PATH, path);
        ds.addChild(ds.getChildCount(), node);

        // Now add in the defaults; these will be "include all directories" and
        // "include all indexable files".
        SpecificationNode sn = new SpecificationNode(SharedDriveConnector.NODE_INCLUDE);
        sn.setAttribute(SharedDriveConnector.ATTRIBUTE_TYPE, "file");
        sn.setAttribute(SharedDriveConnector.ATTRIBUTE_INDEXABLE, "yes");
        sn.setAttribute(SharedDriveConnector.ATTRIBUTE_FILESPEC, "*");
        node.addChild(node.getChildCount(), sn);
        sn = new SpecificationNode(SharedDriveConnector.NODE_INCLUDE);
        sn.setAttribute(SharedDriveConnector.ATTRIBUTE_TYPE, "directory");
        sn.setAttribute(SharedDriveConnector.ATTRIBUTE_FILESPEC, "*");
        node.addChild(node.getChildCount(), sn);
      } else if (op != null && op.equals("Up")) {
        // Strip off end
        String path = variableContext.getParameter(seqPrefix + "specpath");
        final int k = path.lastIndexOf("/");
        if (k == -1) {
          path = "";
        } else {
          path = path.substring(0, k);
        }
        currentContext.save(seqPrefix + "specpath", path);
      } else if (op != null && op.equals("AddToPath")) {
        String path = variableContext.getParameter(seqPrefix + "specpath");
        final String addon = variableContext.getParameter(seqPrefix + "pathaddon");
        final String typein = variableContext.getParameter(seqPrefix + "pathtypein");
        if (addon != null && addon.length() > 0) {
          if (path.length() == 0) {
            path = addon;
          } else {
            path += "/" + addon;
          }
        } else if (typein != null && typein.length() > 0) {
          String trialPath = path;
          if (trialPath.length() == 0) {
            trialPath = typein;
          } else {
            trialPath += "/" + typein;
          }
          // Validate trial path
          try {
            trialPath = validateFolderName(trialPath);
            if (trialPath != null) {
              path = trialPath;
            }
          } catch (final ManifoldCFException e) {
            // Effectively, this just means we can't add a typein to the path
            // right now.
          }
        }
        currentContext.save(seqPrefix + "specpath", path);
      }
    }

    // Include file filters
    x = variableContext.getParameter(seqPrefix + "includefilefilter_count");
    if (x != null && x.length() > 0) {
      // About to gather the includefilter nodes, so get rid of the old ones.
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode node = ds.getChild(i);
        if (node.getType().equals(SharedDriveConnector.NODE_INCLUDEFILEFILTER)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = seqPrefix + "includefilefilter_";
        final String suffix = "_" + Integer.toString(i);
        final String op = variableContext.getParameter(prefix + "op" + suffix);
        if (op == null || !op.equals("Delete")) {
          // Gather the includefilters etc.
          final String regex = variableContext.getParameter(prefix + SharedDriveConnector.ATTRIBUTE_REGEX + suffix);
          final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_INCLUDEFILEFILTER);
          node.setAttribute(SharedDriveConnector.ATTRIBUTE_REGEX, regex);
          ds.addChild(ds.getChildCount(), node);
        }
        i++;
      }

      final String addop = variableContext.getParameter(seqPrefix + "includefilefilter_op");
      if (addop != null && addop.equals("Add")) {
        final String regex = variableContext.getParameter(seqPrefix + "includefilefilter_regex");
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_INCLUDEFILEFILTER);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REGEX, regex);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    // Include folder filters
    x = variableContext.getParameter(seqPrefix + "includefolderfilter_count");
    if (x != null && x.length() > 0) {
      // About to gather the includefolderfilter nodes, so get rid of the old ones.
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode node = ds.getChild(i);
        if (node.getType().equals(SharedDriveConnector.NODE_INCLUDEFOLDERFILTER)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = seqPrefix + "includefolderfilter_";
        final String suffix = "_" + Integer.toString(i);
        final String op = variableContext.getParameter(prefix + "op" + suffix);
        if (op == null || !op.equals("Delete")) {
          // Gather the includefolderfilters etc.
          final String regex = variableContext.getParameter(prefix + SharedDriveConnector.ATTRIBUTE_REGEX + suffix);
          final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_INCLUDEFOLDERFILTER);
          node.setAttribute(SharedDriveConnector.ATTRIBUTE_REGEX, regex);
          ds.addChild(ds.getChildCount(), node);
        }
        i++;
      }

      final String addop = variableContext.getParameter(seqPrefix + "includefolderfilter_op");
      if (addop != null && addop.equals("Add")) {
        final String regex = variableContext.getParameter(seqPrefix + "includefolderfilter_regex");
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_INCLUDEFOLDERFILTER);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REGEX, regex);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    // Exclude filters
    x = variableContext.getParameter(seqPrefix + "excludefilter_count");
    if (x != null && x.length() > 0) {
      // About to gather the excludefilter nodes, so get rid of the old ones.
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode node = ds.getChild(i);
        if (node.getType().equals(SharedDriveConnector.NODE_EXCLUDEFILTER)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = seqPrefix + "excludefilter_";
        final String suffix = "_" + Integer.toString(i);
        final String op = variableContext.getParameter(prefix + "op" + suffix);
        if (op == null || !op.equals("Delete")) {
          // Gather the excludefilters etc.
          final String regex = variableContext.getParameter(prefix + SharedDriveConnector.ATTRIBUTE_REGEX + suffix);
          final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_EXCLUDEFILTER);
          node.setAttribute(SharedDriveConnector.ATTRIBUTE_REGEX, regex);
          ds.addChild(ds.getChildCount(), node);
        }
        i++;
      }

      final String addop = variableContext.getParameter(seqPrefix + "excludefilter_op");
      if (addop != null && addop.equals("Add")) {
        final String regex = variableContext.getParameter(seqPrefix + "excludefilter_regex");
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_EXCLUDEFILTER);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REGEX, regex);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    x = variableContext.getParameter(seqPrefix + "specmaxlength");
    if (x != null) {
      // Delete max length entry
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_MAXLENGTH)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }
      if (x.length() > 0) {
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_MAXLENGTH);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_VALUE, x);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    x = variableContext.getParameter(seqPrefix + "specminlength");
    if (x != null) {
      // Delete min length entry
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_MINLENGTH)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }
      if (x.length() > 0) {
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_MINLENGTH);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_VALUE, x);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    x = variableContext.getParameter(seqPrefix + "specsecurity");
    if (x != null) {
      // Delete all security entries first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_SECURITY)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_SECURITY);
      node.setAttribute(SharedDriveConnector.ATTRIBUTE_VALUE, x);
      ds.addChild(ds.getChildCount(), node);

    }

    x = variableContext.getParameter(seqPrefix + "tokencount");
    if (x != null) {
      // Delete all file specs first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_ACCESS)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      final int accessCount = Integer.parseInt(x);
      i = 0;
      while (i < accessCount) {
        final String accessDescription = "_" + Integer.toString(i);
        final String accessOpName = seqPrefix + "accessop" + accessDescription;
        x = variableContext.getParameter(accessOpName);
        if (x != null && x.equals("Delete")) {
          // Next row
          i++;
          continue;
        }
        // Get the stuff we need
        final String accessSpec = variableContext.getParameter(seqPrefix + "spectoken" + accessDescription);
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_ACCESS);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_TOKEN, accessSpec);
        ds.addChild(ds.getChildCount(), node);
        i++;
      }

      final String op = variableContext.getParameter(seqPrefix + "accessop");
      if (op != null && op.equals("Add")) {
        final String accessspec = variableContext.getParameter(seqPrefix + "spectoken");
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_ACCESS);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_TOKEN, accessspec);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    x = variableContext.getParameter(seqPrefix + "specsharesecurity");
    if (x != null) {
      // Delete all security entries first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_SHARESECURITY)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_SHARESECURITY);
      node.setAttribute(SharedDriveConnector.ATTRIBUTE_VALUE, x);
      ds.addChild(ds.getChildCount(), node);

    }

    x = variableContext.getParameter(seqPrefix + "specparentfoldersecurity");
    if (x != null) {
      // Delete all security entries first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_PARENTFOLDERSECURITY)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_PARENTFOLDERSECURITY);
      node.setAttribute(SharedDriveConnector.ATTRIBUTE_VALUE, x);
      ds.addChild(ds.getChildCount(), node);

    }

    String xc = variableContext.getParameter(seqPrefix + "specpathnameattribute");
    if (xc != null) {
      // Delete old one
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_PATHNAMEATTRIBUTE)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }
      if (xc.length() > 0) {
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_PATHNAMEATTRIBUTE);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_VALUE, xc);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    xc = variableContext.getParameter(seqPrefix + "specmappingcount");
    if (xc != null) {
      // Delete old spec
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_PATHMAP)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      // Now, go through the data and assemble a new list.
      final int mappingCount = Integer.parseInt(xc);

      // Gather up these
      i = 0;
      while (i < mappingCount) {
        final String pathDescription = "_" + Integer.toString(i);
        final String pathOpName = seqPrefix + "specmappingop" + pathDescription;
        xc = variableContext.getParameter(pathOpName);
        if (xc != null && xc.equals("Delete")) {
          // Skip to the next
          i++;
          continue;
        }
        // Inserts won't happen until the very end
        final String match = variableContext.getParameter(seqPrefix + "specmatch" + pathDescription);
        final String replace = variableContext.getParameter(seqPrefix + "specreplace" + pathDescription);
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_PATHMAP);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_MATCH, match);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REPLACE, replace);
        ds.addChild(ds.getChildCount(), node);
        i++;
      }

      // Check for add
      xc = variableContext.getParameter(seqPrefix + "specmappingop");
      if (xc != null && xc.equals("Add")) {
        final String match = variableContext.getParameter(seqPrefix + "specmatch");
        final String replace = variableContext.getParameter(seqPrefix + "specreplace");
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_PATHMAP);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_MATCH, match);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REPLACE, replace);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    xc = variableContext.getParameter(seqPrefix + "specfmapcount");
    if (xc != null) {
      // Delete old spec
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_FILEMAP)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      // Now, go through the data and assemble a new list.
      final int mappingCount = Integer.parseInt(xc);

      // Gather up these
      i = 0;
      while (i < mappingCount) {
        final String pathDescription = "_" + Integer.toString(i);
        final String pathOpName = seqPrefix + "specfmapop" + pathDescription;
        xc = variableContext.getParameter(pathOpName);
        if (xc != null && xc.equals("Delete")) {
          // Skip to the next
          i++;
          continue;
        }
        // Inserts won't happen until the very end
        final String match = variableContext.getParameter(seqPrefix + "specfmapmatch" + pathDescription);
        final String replace = variableContext.getParameter(seqPrefix + "specfmapreplace" + pathDescription);
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_FILEMAP);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_MATCH, match);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REPLACE, replace);
        ds.addChild(ds.getChildCount(), node);
        i++;
      }

      // Check for add
      xc = variableContext.getParameter(seqPrefix + "specfmapop");
      if (xc != null && xc.equals("Add")) {
        final String match = variableContext.getParameter(seqPrefix + "specfmapmatch");
        final String replace = variableContext.getParameter(seqPrefix + "specfmapreplace");
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_FILEMAP);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_MATCH, match);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REPLACE, replace);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    xc = variableContext.getParameter(seqPrefix + "specumapcount");
    if (xc != null) {
      // Delete old spec
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(SharedDriveConnector.NODE_URIMAP)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      // Now, go through the data and assemble a new list.
      final int mappingCount = Integer.parseInt(xc);

      // Gather up these
      i = 0;
      while (i < mappingCount) {
        final String pathDescription = "_" + Integer.toString(i);
        final String pathOpName = seqPrefix + "specumapop" + pathDescription;
        xc = variableContext.getParameter(pathOpName);
        if (xc != null && xc.equals("Delete")) {
          // Skip to the next
          i++;
          continue;
        }
        // Inserts won't happen until the very end
        final String match = variableContext.getParameter(seqPrefix + "specumapmatch" + pathDescription);
        final String replace = variableContext.getParameter(seqPrefix + "specumapreplace" + pathDescription);
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_URIMAP);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_MATCH, match);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REPLACE, replace);
        ds.addChild(ds.getChildCount(), node);
        i++;
      }

      // Check for add
      xc = variableContext.getParameter(seqPrefix + "specumapop");
      if (xc != null && xc.equals("Add")) {
        final String match = variableContext.getParameter(seqPrefix + "specumapmatch");
        final String replace = variableContext.getParameter(seqPrefix + "specumapreplace");
        final SpecificationNode node = new SpecificationNode(SharedDriveConnector.NODE_URIMAP);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_MATCH, match);
        node.setAttribute(SharedDriveConnector.ATTRIBUTE_REPLACE, replace);
        ds.addChild(ds.getChildCount(), node);
      }
    }
    return null;
  }

  /**
   * View specification. This method is called in the body section of a job's view page. Its purpose is to present the document specification information to the user. The coder can presume that the
   * HTML that is output from this configuration will be within appropriate <html> and <body> tags. The connector will be connected before this method can be called.
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the locale the output is preferred to be in.
   * @param ds                       is the current document specification for this job.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   */
  @Override
  public void viewSpecification(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber) throws ManifoldCFException, IOException {
    out.print("<table class=\"displaytable\">\n");
    int i = 0;
    boolean seenAny = false;

    // Paths
    while (i < ds.getChildCount()) {
      final SpecificationNode spn = ds.getChild(i++);
      if (spn.getType().equals(SharedDriveConnector.NODE_STARTPOINT)) {
        if (seenAny == false) {
          seenAny = true;
        }
        out.print("  <tr>\n" + "    <td class=\"description\">\n" + "      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(spn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_PATH)) + ":"
            + "</nobr>\n" + "    </td>\n" + "    <td class=\"value\">\n");
        int j = 0;
        while (j < spn.getChildCount()) {
          final SpecificationNode sn = spn.getChild(j++);
          // This is "include" or "exclude"
          final String nodeFlavor = sn.getType();
          // This is the file/directory name match
          final String filespec = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_FILESPEC);
          // This has a value of null, "", "file", or "directory".
          String nodeType = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_TYPE);
          if (nodeType == null) {
            nodeType = "";
          }
          // This has a value of null, "", "yes", or "no".
          String ingestableFlag = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_INDEXABLE);
          if (ingestableFlag == null) {
            ingestableFlag = "";
          }
          out.print("      <nobr>\n" + "        " + Integer.toString(j) + ".\n" + "        " + (nodeFlavor.equals(SharedDriveConnector.NODE_INCLUDE) ? "Include" : "") + "\n" + "        "
              + (nodeFlavor.equals(SharedDriveConnector.NODE_EXCLUDE) ? "Exclude" : "") + "\n" + "        " + (ingestableFlag.equals("yes") ? "&nbsp;indexable" : "") + "\n" + "        "
              + (ingestableFlag.equals("no") ? "&nbsp;un-indexable" : "") + "\n" + "        " + (nodeType.equals(SharedDriveConnector.VALUE_FILE) ? "&nbsp;file(s)" : "") + "\n" + "        "
              + (nodeType.equals(SharedDriveConnector.VALUE_DIRECTORY) ? "&nbsp;directory(s)" : "") + "\n" + "        " + (nodeType.equals("") ? "&nbsp;file(s)&nbsp;or&nbsp;directory(s)" : "")
              + "&nbsp;matching&nbsp;\n" + "        " + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(filespec) + "\n" + "      </nobr>\n" + "      <br/>\n");
        }
        out.print("    </td>\n" + "  </tr>\n");
      }
    }
    if (seenAny == false) {
      out.print("  <tr><td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoDocumentsSpecified") + "</td></tr>\n");
    }
    out.print("  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "\n");

    // Global filters
    // Fill in the field mapping tab data
    final Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));
    fillInGlobalFiltersSpecification(paramMap, ds);
    Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_GLOBAL_FILTERS_HTML, paramMap);

//    final List<String> includeFilters = new ArrayList<>();
//    final List<String> excludeFilters = new ArrayList<>();
//    i = 0;
//    while (i < ds.getChildCount()) {
//      final SpecificationNode sn = ds.getChild(i++);
//      if (sn.getType().equals(SharedDriveConnector.NODE_INCLUDEFILEFILTER)) {
//        final String includeFilter = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REGEX);
//        if (includeFilter != null) {
//          includeFilters.add(includeFilter);
//        }
//      } else if (sn.getType().equals(SharedDriveConnector.NODE_EXCLUDEFILTER)) {
//        final String excludeFilter = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REGEX);
//        if (excludeFilter != null) {
//          excludeFilters.add(excludeFilter);
//        }
//      }
//    }
//    out.print("\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Messages.getBodyString(locale, "SharedDriveConnector.GlobalFilters")) + " "
//        + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Messages.getBodyString(locale, "SharedDriveConnector.IncludeFilters")) + "</nobr></td>\n");
//    out.print("<td class=\"boxcell\">\n" + "      <table class=\"formtable\">\n" + "        <tr class=\"formheaderrow\">\n" + "          <td class=\"formcolumnheader\"><nobr>"
//        + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Messages.getBodyString(locale, "SharedDriveConnector.RegularExpression")) + "</nobr></td>\n" + "        </tr>\n");
//    if (includeFilters.size() == 0) {
//      out.print("<tr class=\"formrow\"><td class=\"formmessage\" colspan=\"3\">"
//          + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Messages.getBodyString(locale, "SharedDriveConnector.NoIncludeFilterSpecified")) + "</td></tr>\n");
//    } else {
//      int includeCounter = 0;
//      for (final String includeFilter : includeFilters) {
//        if (includeCounter % 2 == 0) {
//          out.print("<tr class=\"evenformrow\">\n");
//        } else {
//          out.print("<tr class=\"oddformrow\">\n");
//        }
//        out.print("<td class=\"formcolumncell\">\n" + "            <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(includeFilter) + "</nobr>\n" + "          </td>\n" + "        </tr>\n");
//        includeCounter++;
//      }
//    }
//    out.print("</table>\n" + "    </td>\n" + "  </tr>\n" + "  <tr>\n" + "    <td class=\"description\">\n" + "      <nobr>"
//        + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Messages.getBodyString(locale, "SharedDriveConnector.GlobalFilters")) + " "
//        + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Messages.getBodyString(locale, "SharedDriveConnector.ExcludeFilters")) + "</nobr>\n" + "    </td>\n" + "    <td class=\"boxcell\">\n"
//        + "      <table class=\"formtable\">\n" + "        <tr class=\"formheaderrow\">\n" + "          <td class=\"formcolumnheader\"><nobr>"
//        + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Messages.getBodyString(locale, "SharedDriveConnector.RegularExpression")) + "</nobr></td>\n" + "        </tr>\n");
//    if (excludeFilters.size() == 0) {
//      out.print("<tr class=\"formrow\"><td class=\"formmessage\" colspan=\"3\">"
//          + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Messages.getBodyString(locale, "SharedDriveConnector.NoExcludeFilterSpecified")) + "</td></tr>\n");
//    } else {
//      int excludeCounter = 0;
//      for (final String excludeFilter : excludeFilters) {
//        if (excludeCounter % 2 == 0) {
//          out.print("<tr class=\"evenformrow\">\n");
//        } else {
//          out.print("<tr class=\"oddformrow\">\n");
//        }
//        out.print("<td class=\"formcolumncell\">\n" + "            <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(excludeFilter) + "</nobr>\n" + "          </td>\n" + "        </tr>\n");
//        excludeCounter++;
//      }
//    }
//    out.print("</table>\n" + "    </td>\n" + "  </tr>\n");

    // Find whether security is on or off
    i = 0;
    boolean securityOn = true;
    boolean shareSecurityOn = true;
    boolean parentFolderSecurityOn = false;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_SECURITY)) {
        final String securityValue = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
        if (securityValue.equals("off")) {
          securityOn = false;
        } else if (securityValue.equals("on")) {
          securityOn = true;
        }
      }
      if (sn.getType().equals(SharedDriveConnector.NODE_SHARESECURITY)) {
        final String securityValue = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
        if (securityValue.equals("off")) {
          shareSecurityOn = false;
        } else if (securityValue.equals("on")) {
          shareSecurityOn = true;
        }
      }
      if (sn.getType().equals(SharedDriveConnector.NODE_PARENTFOLDERSECURITY)) {
        final String securityValue = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
        if (securityValue.equals("off")) {
          parentFolderSecurityOn = false;
        } else if (securityValue.equals("on")) {
          parentFolderSecurityOn = true;
        }
      }
    }
    out.print("\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.FileSecurity") + "</nobr></td>\n" + "    <td class=\"value\"><nobr>"
        + (securityOn ? Messages.getBodyString(locale, "SharedDriveConnector.Enabled") : Messages.getBodyString(locale, "SharedDriveConnector.Disabled")) + "</nobr></td>\n" + "  </tr>\n" + "\n"
        + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n");
    // Go through looking for access tokens
    seenAny = false;
    i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_ACCESS)) {
        if (seenAny == false) {
          out.print("  <tr><td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.FileAccessTokens") + "</nobr></td>\n" + "    <td class=\"value\">\n");
          seenAny = true;
        }
        final String token = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_TOKEN);
        out.print("      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(token) + "</nobr><br/>\n");
      }
    }

    if (seenAny) {
      out.print("    </td>\n" + "  </tr>\n");
    } else {
      out.print("  <tr><td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoFileAccessTokensSpecified") + "</td></tr>\n");
    }
    out.print("  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "    \n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
        + Messages.getBodyString(locale, "SharedDriveConnector.ShareSecurity") + "</nobr></td>\n" + "    <td class=\"value\"><nobr>"
        + (shareSecurityOn ? Messages.getBodyString(locale, "SharedDriveConnector.Enabled") : Messages.getBodyString(locale, "SharedDriveConnector.Disabled")) + "</nobr></td>\n" + "  </tr>\n" + "\n"
        + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "    \n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
        + Messages.getBodyString(locale, "SharedDriveConnector.ParentFolderSecurity") + "</nobr></td>\n" + "    <td class=\"value\"><nobr>"
        + (parentFolderSecurityOn ? Messages.getBodyString(locale, "SharedDriveConnector.Enabled") : Messages.getBodyString(locale, "SharedDriveConnector.Disabled")) + "</nobr></td>\n" + "  </tr>\n"
        + "\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n");
    // Find the path-name metadata attribute name
    i = 0;
    String pathNameAttribute = "";
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_PATHNAMEATTRIBUTE)) {
        pathNameAttribute = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
      }
    }
    out.print("  <tr>\n");
    if (pathNameAttribute.length() > 0) {
      out.print("    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.PathNameMetadataAttribute") + "</nobr></td>\n" + "    <td class=\"value\"><nobr>"
          + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(pathNameAttribute) + "</nobr></td>\n");
    } else {
      out.print("    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoPathNameMetadataAttributeSpecified") + "</td>\n");
    }
    out.print("  </tr>\n" + "\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "\n" + "  <tr>\n" + "\n");

    // Find the path-value mapping data
    i = 0;
    final MatchMap matchMap = new MatchMap();
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_PATHMAP)) {
        final String pathMatch = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_MATCH);
        final String pathReplace = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REPLACE);
        matchMap.appendMatchPair(pathMatch, pathReplace);
      }
    }
    if (matchMap.getMatchCount() > 0) {
      out.print("    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.PathValueMapping") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <table class=\"displaytable\">\n");
      i = 0;
      while (i < matchMap.getMatchCount()) {
        final String matchString = matchMap.getMatchString(i);
        final String replaceString = matchMap.getReplaceString(i);
        out.print(
            "        <tr>\n" + "          <td class=\"value\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString) + "</nobr></td>\n" + "          <td class=\"value\">==></td>\n"
                + "          <td class=\"value\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString) + "</nobr></td>\n" + "        </tr>\n");
        i++;
      }
      out.print("      </table>\n" + "    </td>\n");
    } else {
      out.print("    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoMappingsSpecified") + "</td>\n");
    }
    out.print("  </tr>\n" + "\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "\n" + "  <tr>\n");
    // Find the file name mapping data
    i = 0;
    final MatchMap fileMap = new MatchMap();
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_FILEMAP)) {
        final String pathMatch = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_MATCH);
        final String pathReplace = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REPLACE);
        fileMap.appendMatchPair(pathMatch, pathReplace);
      }
    }
    if (fileMap.getMatchCount() > 0) {
      out.print("    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.FileNameMapping") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <table class=\"displaytable\">\n");
      i = 0;
      while (i < fileMap.getMatchCount()) {
        final String matchString = fileMap.getMatchString(i);
        final String replaceString = fileMap.getReplaceString(i);
        out.print(
            "        <tr>\n" + "          <td class=\"value\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString) + "</nobr></td>\n" + "          <td class=\"value\">==></td>\n"
                + "          <td class=\"value\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString) + "</nobr></td>\n" + "        </tr>\n");
        i++;
      }
      out.print("      </table>\n" + "    </td>\n");
    } else {
      out.print("    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoFileNameMappingsSpecified") + "</td>\n");
    }
    out.print("  </tr>\n" + "\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "\n" + "  <tr>\n");

    // Find the url mapping data
    i = 0;
    final MatchMap uriMap = new MatchMap();
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_URIMAP)) {
        final String pathMatch = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_MATCH);
        final String pathReplace = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_REPLACE);
        uriMap.appendMatchPair(pathMatch, pathReplace);
      }
    }
    if (uriMap.getMatchCount() > 0) {
      out.print("    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "SharedDriveConnector.URLMappingColon") + "</nobr></td>\n" + "    <td class=\"value\">\n"
          + "      <table class=\"displaytable\">\n");
      i = 0;
      while (i < uriMap.getMatchCount()) {
        final String matchString = uriMap.getMatchString(i);
        final String replaceString = uriMap.getReplaceString(i);
        out.print(
            "        <tr>\n" + "          <td class=\"value\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString) + "</nobr></td>\n" + "          <td class=\"value\">==></td>\n"
                + "          <td class=\"value\"><nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString) + "</nobr></td>\n" + "        </tr>\n");
        i++;
      }
      out.print("      </table>\n" + "    </td>\n");
    } else {
      out.print("    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale, "SharedDriveConnector.NoURLMappingsSpecifiedWillProduceAFileIRI") + "</td>\n");
    }
    out.print("  </tr>\n" + "\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
        + Messages.getBodyString(locale, "SharedDriveConnector.MaximumDocumentLength") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <nobr>\n");
    // Find the path-value mapping data
    i = 0;
    String maxLength = null;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_MAXLENGTH)) {
        maxLength = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
      }
    }
    if (maxLength == null || maxLength.length() == 0) {
      maxLength = "Unlimited";
    }
    out.print("        " + maxLength + "\n" + "      </nobr>\n" + "    </td>\n");

    out.print("  </tr>\n" + "\n" + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n" + "\n" + "  <tr>\n" + "    <td class=\"description\"><nobr>"
        + Messages.getBodyString(locale, "SharedDriveConnector.MinimumDocumentLength") + "</nobr></td>\n" + "    <td class=\"value\">\n" + "      <nobr>\n");
    i = 0;
    String minLength = null;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(SharedDriveConnector.NODE_MINLENGTH)) {
        minLength = sn.getAttributeValue(SharedDriveConnector.ATTRIBUTE_VALUE);
      }
    }
    if (minLength == null || minLength.length() == 0) {
      minLength = "No limit";
    }
    out.print("        " + minLength + "\n" + "      </nobr>\n" + "    </td>\n" + "  </tr>\n" + "</table>\n");
  }

  /**
   * Test if there is at least one regular expression that match with the provided sting
   *
   * @param regexList the list of regular expressions
   * @param str       the string to test
   * @return the first matching regex found or null if no matching regex
   */
  private String matchingRegex(final List<String> regexList, final String str) throws RegexException {
    for (final String regex : regexList) {
      try {
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
          return regex;
        }
      } catch (final PatternSyntaxException e) {
        throw new RegexException(regex, "Invalid regular expression");
      }
    }
    return null;
  }

  /* The following are additional methods used by the UI */

  /**
   * given a server uri, return all shares
   *
   * @param serverURI -
   * @return an array of SmbFile
   */
  public SmbFile[] getShareNames(final String serverURI) throws ManifoldCFException {
    getSession();
    SmbFile server = null;
    try {
      server = new SmbFile(serverURI, ctx);
    } catch (final MalformedURLException e1) {
      throw new ManifoldCFException("MalformedURLException tossed", e1);
    }
    SmbFile[] shares = null;
    try {
      // check to make sure it's a server
      if (getFileType(server) == SmbFile.TYPE_SERVER) {
        shares = fileListFiles(server, new ShareFilter());
      }
    } catch (final SmbException e) {
      throw new ManifoldCFException("SmbException tossed: " + e.getMessage(), e);
    }
    return shares;
  }

  /**
   * Given a folder path, determine if the folder is in fact legal and accessible (and is a folder).
   *
   * @param folder is the relative folder from the network root
   * @return the canonical folder name if valid, or null if not.
   * @throws ManifoldCFException
   */
  public String validateFolderName(final String folder) throws ManifoldCFException {
    getSession();
    // create new connection by appending to the old connection
    final String smburi = smbconnectionPath;
    String uri = smburi;
    if (folder.length() > 0) {
      uri = smburi + folder + "/";
    }

    SmbFile currentDirectory = null;
    try {
      try {
        currentDirectory = new SmbFile(uri, ctx);
      } catch (final MalformedURLException e1) {
        throw new ManifoldCFException("validateFolderName: Can't get parent file: " + uri, e1);
      }

      try {
        currentDirectory.connect();
        if (fileIsDirectory(currentDirectory) == false) {
          return null;
        }
        final String newCanonicalPath = currentDirectory.getCanonicalPath();
        String rval = newCanonicalPath.substring(smburi.length());
        if (rval.endsWith("/")) {
          rval = rval.substring(0, rval.length() - 1);
        }
        return rval;
      } catch (final SmbException se) {
        try {
          processSMBException(se, folder, "checking folder", "getting canonical path");
          return null;
        } catch (final ServiceInterruption si) {
          throw new ManifoldCFException("Service interruption: " + si.getMessage(), si);
        }
      } catch (final MalformedURLException e) {
        throw new ManifoldCFException("MalformedURLException tossed: " + e.getMessage(), e);
      } catch (final java.net.SocketTimeoutException e) {
        throw new ManifoldCFException("IOException tossed: " + e.getMessage(), e);
      } catch (final InterruptedIOException e) {
        throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
      } catch (final IOException e) {
        throw new ManifoldCFException("IOException tossed: " + e.getMessage(), e);
      } catch (final Exception e) {
        throw new ManifoldCFException("Exception tossed: " + e.getMessage(), e);
      }
    } finally {
      if (currentDirectory != null) {
        currentDirectory.close();
      }
    }

  }

  /**
   * given a smb uri, return all children directories
   *
   * @param folder is the relative folder from the network root
   * @return array of child folder names
   * @throws ManifoldCFException
   */
  public String[] getChildFolderNames(final String folder) throws ManifoldCFException {
    getSession();
    // create new connection by appending to the old connection
    final String smburi = smbconnectionPath;
    String uri = smburi;
    if (folder.length() > 0) {
      uri = smburi + folder + "/";
    }

    SmbFile currentDirectory = null;
    try {
      try {
        currentDirectory = new SmbFile(uri, ctx);
      } catch (final MalformedURLException e1) {
        throw new ManifoldCFException("getChildFolderNames: Can't get parent file: " + uri, e1);
      }

      // add DFS support
      SmbFile[] children = null;
      try {
        currentDirectory.connect();
        children = currentDirectory.listFiles(new DirectoryFilter());
      } catch (final SmbException se) {
        try {
          processSMBException(se, folder, "getting child folder names", "listing files");
          children = new SmbFile[0];
        } catch (final ServiceInterruption si) {
          throw new ManifoldCFException("Service interruption: " + si.getMessage(), si);
        }
      } catch (final MalformedURLException e) {
        throw new ManifoldCFException("MalformedURLException tossed: " + e.getMessage(), e);
      } catch (final java.net.SocketTimeoutException e) {
        throw new ManifoldCFException("IOException tossed: " + e.getMessage(), e);
      } catch (final InterruptedIOException e) {
        throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
      } catch (final IOException e) {
        throw new ManifoldCFException("IOException tossed: " + e.getMessage(), e);
      }

      // populate a String array
      final String[] directories = new String[children.length];
      for (int i = 0; i < children.length; i++) {
        String directoryName = children[i].getName();
        // strip the trailing slash
        directoryName = directoryName.replaceAll("/", "");
        directories[i] = directoryName;
      }

      java.util.Arrays.sort(directories);
      return directories;
    } finally {
      if (currentDirectory != null) {
        currentDirectory.close();
      }
    }

  }

  /**
   * inner class which returns only shares. used by listfiles(SmbFileFilter)
   *
   * @author James Maupin
   */

  class ShareFilter implements SmbFileFilter {
    /*
     * (non-Javadoc)
     *
     * @see jcifs.smb.SmbFileFilter#accept(jcifs.smb.SmbFile)
     */
    @Override
    public boolean accept(final SmbFile arg0) throws SmbException {
      if (getFileType(arg0) == SmbFile.TYPE_SHARE) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * inner class which returns only directories. used by listfiles(SmbFileFilter)
   *
   * @author James Maupin
   */

  class DirectoryFilter implements SmbFileFilter {
    /*
     * (non-Javadoc)
     *
     * @see jcifs.smb.SmbFileFilter#accept(jcifs.smb.SmbFile)
     */
    @Override
    public boolean accept(final SmbFile arg0) throws SmbException {
      final int type = getFileType(arg0);
      if (type == SmbFile.TYPE_SHARE || type == SmbFile.TYPE_FILESYSTEM && fileIsDirectory(arg0)) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * This is the filter class that actually receives the files in batches. We do it this way so that the client won't run out of memory loading a huge directory.
   */
  protected class ProcessDocumentsFilter implements SmbFileFilter {

    /**
     * This is the activities object, where matching references will be logged
     */
    protected final IProcessActivity activities;
    /** Document specification */
    protected final Specification spec;
    /** Parent Folder match the include filter **/
    protected final boolean matchFolder;

    protected final GlobalFilters gf;
    /**
     * Exceptions that we saw. These are saved here so that they can be rethrown when done
     */
    protected ManifoldCFException lcfException = null;
    protected ServiceInterruption serviceInterruption = null;

    /** Constructor */
    public ProcessDocumentsFilter(final IProcessActivity activities, final Specification spec, final boolean matchFolder) {
      this.activities = activities;
      this.spec = spec;
      this.matchFolder = matchFolder;
      this.gf = new GlobalFilters(spec);
    }

    /**
     * Decide if we accept the file. This is where we will actually do the work.
     */
    @Override
    public boolean accept(final SmbFile f) throws SmbException {
      if (lcfException != null || serviceInterruption != null) {
        return false;
      }

      try {
        final int type = f.getType();
        if (type != SmbFile.TYPE_SERVER && type != SmbFile.TYPE_FILESYSTEM && type != SmbFile.TYPE_SHARE) {
          return false;
        }
        final String canonicalPath = getFileCanonicalPath(f);
        if (canonicalPath != null) {
          // manipulate path to include the DFS alias, not the literal path
          // String newPath = matchPrefix +
          // canonicalPath.substring(matchReplace.length());
          final String newPath = canonicalPath;

          // Check against the current specification. This is a nicety to avoid
          // queuing
          // documents that we will immediately turn around and remove. However,
          // if this
          // check was not here, everything should still function, provided the
          // getDocumentVersions()
          // method does the right thing.
          final boolean fileIsDirectory = fileIsDirectory(f);
          if (checkInclude(fileIsDirectory, newPath, spec)) {

            if (fileIsDirectory) {
              // First of all check if document match global filters
              try {
                // Check if the file is included
                // there must be declared folder filters AND the current folder must be different from a folder configured in the job specs
                if (!gf.includeFolderFilters.isEmpty()) {
                  final String matchingRegex = matchingRegex(gf.includeFolderFilters, newPath);
                  if (matchingRegex == null) {
                    activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, newPath, "EXCLUDED", "Does not match any global include folder filters", null);
                    return false;
                  }
                }
              } catch (final RegexException e) {
                Logging.connectors.warn("Global include folder filter regular expression syntax error: " + e.getRegex());
              }
              if (Logging.connectors.isDebugEnabled()) {
                Logging.connectors.debug("JCIFS: Recorded path is '" + newPath + "' and is included.");
              }
              activities.addDocumentReference(newPath);
            } else if (matchFolder) { // Reference file only if its parent folder is included in the crawl

              // First of all check if document match global filters
              try {
                // Check if the file is included
                if (!gf.includeFileFilters.isEmpty()) {
                  final String matchingRegex = matchingRegex(gf.includeFileFilters, newPath);
                  if (matchingRegex == null) {
                    activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, newPath, "EXCLUDED", "Does not match any global include filters", null);
                    return false;
                  }
                }
              } catch (final RegexException e) {
                Logging.connectors.warn("Global include filter regular expression syntax error: " + e.getRegex());
              }

              try {
                // Check if the file has to be excluded
                if (!gf.excludeFilters.isEmpty()) {
                  final String matchingRegex = matchingRegex(gf.excludeFilters, newPath);
                  if (matchingRegex != null) {
                    activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, newPath, "EXCLUDED", "Has been excluded by the following global exclude filter: " + matchingRegex, null);
                    return false;
                  }
                }
              } catch (final RegexException e) {
                Logging.connectors.warn("Global exclude filter regular expression syntax error: " + e.getRegex());
              }

              final long fileLength = fileLength(f);
              if (checkIncludeFile(fileLength, newPath, spec, activities)) {
                if (Logging.connectors.isDebugEnabled()) {
                  Logging.connectors.debug("JCIFS: Recorded path is '" + newPath + "' and is included.");
                }
                activities.addDocumentReference(newPath);
              } else {
                if (Logging.connectors.isDebugEnabled()) {
                  Logging.connectors.debug("JCIFS: Recorded path '" + newPath + "' is excluded!");
                }
              }
            }
          } else {
            if (Logging.connectors.isDebugEnabled()) {
              Logging.connectors.debug("JCIFS: Recorded path '" + newPath + "' is excluded!");
            }
            activities.recordActivity(System.currentTimeMillis(), ACTIVITY_ACCESS, 0L, newPath, "EXCLUDED", "Has been excluded by filters", null);
          }
        } else {
          Logging.connectors.debug("JCIFS: Excluding a child file because canonical path is null");
        }

        return false;
      } catch (final ManifoldCFException e) {
        if (lcfException == null) {
          lcfException = e;
        }
        return false;
      } catch (final ServiceInterruption e) {
        if (serviceInterruption == null) {
          serviceInterruption = e;
        }
        return false;
      }
    }

    /** Check for exception, and throw if there is one */
    public void checkAndThrow() throws ServiceInterruption, ManifoldCFException {
      if (lcfException != null) {
        throw lcfException;
      }
      if (serviceInterruption != null) {
        throw serviceInterruption;
      }
    }
  }

  protected static class GlobalFilters {

    private final List<String> includeFileFilters = new ArrayList<>();
    private final List<String> includeFolderFilters = new ArrayList<>();
    private final List<String> excludeFilters = new ArrayList<>();

    public GlobalFilters(final Specification os) {
      for (int i = 0; i < os.getChildCount(); i++) {
        final SpecificationNode sn = os.getChild(i);

        if (sn.getType().equals(NODE_INCLUDEFILEFILTER)) {
          final String regex = sn.getAttributeValue(ATTRIBUTE_REGEX);
          includeFileFilters.add(regex);
        }

        if (sn.getType().equals(NODE_INCLUDEFOLDERFILTER)) {
          final String regex = sn.getAttributeValue(ATTRIBUTE_REGEX);
          includeFolderFilters.add(regex);
        }

        if (sn.getType().equals(NODE_EXCLUDEFILTER)) {
          final String regex = sn.getAttributeValue(ATTRIBUTE_REGEX);
          excludeFilters.add(regex);
        }
      }

      if (includeFileFilters.isEmpty()) {
        includeFileFilters.add(INCLUDEFILTER_DEFAULT);
      }

      if (includeFolderFilters.isEmpty()) {
        includeFolderFilters.add(INCLUDEFILTER_DEFAULT);
      }
    }

    public String toPackedString() {
      final StringBuilder sb = new StringBuilder();

      packList(sb, includeFileFilters, '+');
      packList(sb, includeFolderFilters, '+');
      packList(sb, excludeFilters, '+');

      return sb.toString();
    }

  }

}
