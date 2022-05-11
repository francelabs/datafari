/* $Id: WebcrawlerConnector.java 995042 2010-09-08 13:10:06Z kwright $ */

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
package com.francelabs.datafari.connectors.webcrawler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.connectorcommon.fuzzyml.Parser;
import org.apache.manifoldcf.connectorcommon.fuzzyml.XMLFuzzyHierarchicalParseState;
import org.apache.manifoldcf.connectorcommon.fuzzyml.XMLParsingContext;
import org.apache.manifoldcf.connectorcommon.fuzzyml.XMLStringParsingContext;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;
import org.apache.manifoldcf.connectorcommon.interfaces.KeystoreManagerFactory;
import org.apache.manifoldcf.core.interfaces.ConfigNode;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.DBInterfaceFactory;
import org.apache.manifoldcf.core.interfaces.IDBInterface;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.crawler.interfaces.IExistingVersions;
import org.apache.manifoldcf.crawler.interfaces.IFingerprintActivity;
import org.apache.manifoldcf.crawler.interfaces.IHistoryActivity;
import org.apache.manifoldcf.crawler.interfaces.INamingActivity;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.crawler.system.ManifoldCF;

/**
 * This is the Web Crawler implementation of the IRepositoryConnector interface. This connector may be superceded by one that calls out to python, or by a entirely python Connector Framework,
 * depending on how the winds blow.
 *
 */
public class WebcrawlerConnector extends org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector {
  public static final String _rcsid = "@(#)$Id: WebcrawlerConnector.java 995042 2010-09-08 13:10:06Z kwright $";

  private static Level DOCPROCESSLEVEL = Level.forName("DOCPROCESS", 450);
  private static final Logger LOGGER = LogManager.getLogger(WebcrawlerConnector.class.getName());

  // First, a couple of very important points.
  // The canonical document identifier is simply a legal URL. It may have been processed in some way to
  // map it to a "base" form, but that's all.
  //
  // A document's version is mostly calculated using a checksum technique. It is conceivable that we
  // will eventually want the version to be more cleverly calculated, perhaps by recognizing the document
  // type and extracting more definitive information, so it will be constructed that way, but right now
  // there's only going to be the basic checksum done.
  //
  // This very basic crawler will extract links only from text, html, or xml - where links can readily be
  // found. More robust extraction technology will be plugged in eventually, so the architecture will be
  // designed for additional plugins.
  //
  // The documents that actually get ingested will have to pass the "fingerprint" test, which will be lifted
  // wholesale from the jcifs connector. This is to avoid overwhelming the ingestion system with crap.

  // Next, a note about "bins".
  // This crawler places each URL into a set of bins, based on the host that the URL goes to.
  // For example, the URL "http://foo.metacarta.com/stuff.htm" would have the following bins
  // associated with it: "foo.metacarta.com", ".metacarta.com", ".com", and "". The idea for this
  // is to make it straightforward to group together different URLs into sets, for throttling and
  // secure access purposes.

  // These are the signals used by some methods in this class to communicate results
  protected static final int RESULTSTATUS_FALSE = 0;
  protected static final int RESULTSTATUS_TRUE = 1;
  protected static final int RESULTSTATUS_NOTYETDETERMINED = 2;

  /**
   * This represents a list of the mime types that this connector knows how to extract links from. Documents that are indexable are described by the output connector.
   */
  protected static final String[] interestingMimeTypeArray = new String[] { "application/rtf", "application/xls", "text/html", "text/rtf", "application/x-excel", "application/x-msexcel", "application/x-mspowerpoint", "application/x-msword-doc",
      "application/x-msword", "application/x-word", "text/xml", "application/xml", "no-type", "text/plain", "application/x-rtf", "application/x-pdf", "text/richtext", "Text", "Text/html" };

  protected static final Set<String> interestingMimeTypeMap;
  static {
    interestingMimeTypeMap = new HashSet<>();
    int i = 0;
    while (i < interestingMimeTypeArray.length) {
      final String type = interestingMimeTypeArray[i++];
      interestingMimeTypeMap.add(type);
    }
  }

  protected static final Set<String> understoodProtocols;
  static {
    understoodProtocols = new HashSet<>();
    understoodProtocols.add("http");
    understoodProtocols.add("https");
  }

  // Usage flag values
  protected static final int ROBOTS_NONE = 0;
  protected static final int ROBOTS_DATA = 1;
  protected static final int ROBOTS_ALL = 2;

  protected static final int META_ROBOTS_NONE = 0;
  protected static final int META_ROBOTS_ALL = 1;

  // Relationship types
  public final static String REL_LINK = "link";
  public final static String REL_REDIRECT = "redirect";

  // Activity types
  public final static String ACTIVITY_FETCH = "fetch";
  public final static String ACTIVITY_PROCESS = "process";
  public final static String ACTIVITY_ROBOTSPARSE = "robots parse";
  public final static String ACTIVITY_LOGON_START = "begin logon";
  public final static String ACTIVITY_LOGON_END = "end logon";

  // Fetch types
  protected final static String FETCH_ROBOTS = "ROBOTS";
  protected final static String FETCH_STANDARD = "URL";
  protected final static String FETCH_LOGIN = "LOGIN";

  // Reserved headers
  protected final static Set<String> reservedHeaders;
  static {
    reservedHeaders = new HashSet<>();
    reservedHeaders.add("age");
    reservedHeaders.add("www-authenticate");
    reservedHeaders.add("proxy-authenticate");
    reservedHeaders.add("date");
    reservedHeaders.add("set-cookie");
    reservedHeaders.add("via");
  }

  // Potentially excluded headers
  protected final static List<String> potentiallyExcludedHeaders;
  static {
    potentiallyExcludedHeaders = new ArrayList<>();
    potentiallyExcludedHeaders.add("last-modified");
  }

  /** Robots usage flag */
  protected int robotsUsage = ROBOTS_ALL;
  /** Meta robots tag usage flag */
  protected int metaRobotsTagsUsage = META_ROBOTS_ALL;
  /** The user-agent for this connector instance */
  protected String userAgent = null;
  /** The email address for this connector instance */
  protected String from = null;
  /** Connection timeout, milliseconds. */
  protected int connectionTimeoutMilliseconds = 60000;
  /** Socket timeout, milliseconds */
  protected int socketTimeoutMilliseconds = 300000;
  /** Throttle group name */
  protected String throttleGroupName = null;

  // Canonicalization enabling/disabling. Eventually this will probably need to be by regular expression.

  /** The throttle description */
  protected ThrottleDescription throttleDescription = null;
  /** The credentials description */
  protected CredentialsDescription credentialsDescription = null;
  /** The trusts description */
  protected TrustsDescription trustsDescription = null;

  /** The robots manager currently used by this instance */
  protected RobotsManager robotsManager = null;
  /** The DNS manager currently used by this instance */
  protected DNSManager dnsManager = null;
  /** The cookie manager used by this instance */
  protected CookieManager cookieManager = null;

  /** This flag is set when the instance has been initialized */
  protected boolean isInitialized = false;

  /** This is where we keep data around between the getVersions() phase and the processDocuments() phase. */
  protected static DataCache cache = new DataCache();

  /** Proxy host */
  protected String proxyHost = null;

  /** Proxy port */
  protected int proxyPort = -1;

  /** Proxy auth domain */
  protected String proxyAuthDomain = null;

  /** Proxy auth user name */
  protected String proxyAuthUsername = null;

  /** Proxy auth password */
  protected String proxyAuthPassword = null;

  /** Deny access token for default authority */
  private final static String defaultAuthorityDenyToken = "DEAD_AUTHORITY";

  /**
   * Constructor.
   */
  public WebcrawlerConnector() {
  }

  /**
   * Tell the world what model this connector uses for getDocumentIdentifiers(). This must return a model value as specified above.
   *
   * @return the model type value.
   */
  @Override
  public int getConnectorModel() {
    // We return all seeds every time.
    return MODEL_ALL;
  }

  /**
   * Install the connector. This method is called to initialize persistent storage for the connector, such as database tables etc. It is called when the connector is registered.
   *
   * @param threadContext
   *          is the current thread context.
   */
  @Override
  public void install(final IThreadContext threadContext) throws ManifoldCFException {
    // Install
    final IDBInterface mainDatabase = DBInterfaceFactory.make(threadContext, ManifoldCF.getMasterDatabaseName(), ManifoldCF.getMasterDatabaseUsername(), ManifoldCF.getMasterDatabasePassword());

    final RobotsManager rm = new RobotsManager(threadContext, mainDatabase);
    final DNSManager dns = new DNSManager(threadContext, mainDatabase);
    final CookieManager cm = new CookieManager(threadContext, mainDatabase);
    mainDatabase.beginTransaction();
    try {
      rm.install();
      dns.install();
      cm.install();
    } catch (final ManifoldCFException e) {
      mainDatabase.signalRollback();
      throw e;
    } catch (final Error e) {
      mainDatabase.signalRollback();
      throw e;
    } finally {
      mainDatabase.endTransaction();
    }
  }

  /**
   * Uninstall the connector. This method is called to remove persistent storage for the connector, such as database tables etc. It is called when the connector is deregistered.
   *
   * @param threadContext
   *          is the current thread context.
   */
  @Override
  public void deinstall(final IThreadContext threadContext) throws ManifoldCFException {
    // Uninstall
    final IDBInterface mainDatabase = DBInterfaceFactory.make(threadContext, ManifoldCF.getMasterDatabaseName(), ManifoldCF.getMasterDatabaseUsername(), ManifoldCF.getMasterDatabasePassword());

    final RobotsManager rm = new RobotsManager(threadContext, mainDatabase);
    final DNSManager dns = new DNSManager(threadContext, mainDatabase);
    final CookieManager cm = new CookieManager(threadContext, mainDatabase);
    mainDatabase.beginTransaction();
    try {
      cm.deinstall();
      rm.deinstall();
      dns.deinstall();
    } catch (final ManifoldCFException e) {
      mainDatabase.signalRollback();
      throw e;
    } catch (final Error e) {
      mainDatabase.signalRollback();
      throw e;
    } finally {
      mainDatabase.endTransaction();
    }
  }

  /**
   * Return the list of activities that this connector supports (i.e. writes into the log).
   *
   * @return the list.
   */
  @Override
  public String[] getActivitiesList() {
    return new String[] { ACTIVITY_FETCH, ACTIVITY_PROCESS, ACTIVITY_ROBOTSPARSE, ACTIVITY_LOGON_START, ACTIVITY_LOGON_END };
  }

  /**
   * Return the list of relationship types that this connector recognizes.
   *
   * @return the list.
   */
  @Override
  public String[] getRelationshipTypes() {
    return new String[] { REL_LINK, REL_REDIRECT };
  }

  /**
   * Clear out any state information specific to a given thread. This method is called when this object is returned to the connection pool.
   */
  @Override
  public void clearThreadContext() {
    super.clearThreadContext();
    robotsManager = null;
    dnsManager = null;
    cookieManager = null;
  }

  /** Start a session */
  protected void getSession() throws ManifoldCFException {
    // Handle the stuff that requires a thread context
    if (robotsManager == null || dnsManager == null || cookieManager == null) {
      final IDBInterface databaseHandle = DBInterfaceFactory.make(currentContext, ManifoldCF.getMasterDatabaseName(), ManifoldCF.getMasterDatabaseUsername(), ManifoldCF.getMasterDatabasePassword());

      robotsManager = new RobotsManager(currentContext, databaseHandle);
      dnsManager = new DNSManager(currentContext, databaseHandle);
      cookieManager = new CookieManager(currentContext, databaseHandle);
    }

    // Handle everything else
    if (!isInitialized) {
      // Either set this from the connection name, or just have one. Right now, we have one.
      final String throttleGroupName = "";

      final String emailAddress = params.getParameter(WebcrawlerConfig.PARAMETER_EMAIL);
      if (emailAddress == null)
        throw new ManifoldCFException("Missing email address");
      userAgent = "Mozilla/5.0 (ApacheManifoldCFWebCrawler; " + emailAddress + ")";
      from = emailAddress;

      final String robotsTxt = params.getParameter(WebcrawlerConfig.PARAMETER_ROBOTSUSAGE);
      robotsUsage = ROBOTS_ALL;
      if (robotsTxt == null || robotsTxt.length() == 0 || robotsTxt.equals("all"))
        robotsUsage = ROBOTS_ALL;
      else if (robotsTxt.equals("none"))
        robotsUsage = ROBOTS_NONE;
      else if (robotsTxt.equals("data"))
        robotsUsage = ROBOTS_DATA;

      final String metaRobots = params.getParameter(WebcrawlerConfig.PARAMETER_META_ROBOTS_TAGS_USAGE);
      if (metaRobots == null || metaRobots.length() == 0 || metaRobots.equals("all"))
        metaRobotsTagsUsage = META_ROBOTS_ALL;
      else if (metaRobots.equals("none"))
        metaRobotsTagsUsage = META_ROBOTS_NONE;

      throttleDescription = new ThrottleDescription(params);
      credentialsDescription = new CredentialsDescription(params);
      trustsDescription = new TrustsDescription(params);

      proxyHost = params.getParameter(WebcrawlerConfig.PARAMETER_PROXYHOST);
      final String proxyPortString = params.getParameter(WebcrawlerConfig.PARAMETER_PROXYPORT);
      proxyAuthDomain = params.getParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHDOMAIN);
      proxyAuthUsername = params.getParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHUSERNAME);
      proxyAuthPassword = params.getObfuscatedParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHPASSWORD);

      proxyPort = -1;
      if (proxyPortString != null && proxyPortString.length() > 0) {
        try {
          proxyPort = Integer.parseInt(proxyPortString);
        } catch (final NumberFormatException e) {
          throw new ManifoldCFException(e.getMessage(), e);
        }
      }

      isInitialized = true;
    }
  }

  // All methods below this line will ONLY be called if a connect() call succeeded
  // on this instance!

  /**
   * This method is periodically called for all connectors that are connected but not in active use.
   */
  @Override
  public void poll() throws ManifoldCFException {
    ThrottledFetcher.flushIdleConnections(currentContext);
  }

  /**
   * Check status of connection.
   */
  @Override
  public String check() throws ManifoldCFException {
    getSession();
    return super.check();
  }

  /**
   * Close the connection. Call this before discarding the repository connector.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    throttleGroupName = null;
    throttleDescription = null;
    credentialsDescription = null;
    trustsDescription = null;
    userAgent = null;
    from = null;
    proxyHost = null;
    proxyPort = -1;
    proxyAuthDomain = null;
    proxyAuthUsername = null;
    proxyAuthPassword = null;

    isInitialized = false;

    super.disconnect();
  }

  /**
   * Get the bin name string for a document identifier. The bin name describes the queue to which the document will be assigned for throttling purposes. Throttling controls the rate at which items in
   * a given queue are fetched; it does not say anything about the overall fetch rate, which may operate on multiple queues or bins. For example, if you implement a web crawler, a good choice of bin
   * name would be the server name, since that is likely to correspond to a real resource that will need real throttle protection.
   *
   * @param documentIdentifier
   *          is the document identifier.
   * @return the bin name.
   */
  @Override
  public String[] getBinNames(final String documentIdentifier) {
    try {
      final java.net.URI uri = new java.net.URI(documentIdentifier);
      return new String[] { uri.getHost() };
    } catch (final URISyntaxException e) {
      return new String[] { "" };
    }

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
   * @param activities
   *          is the interface this method should use to perform whatever framework actions are desired.
   * @param spec
   *          is a document specification (that comes from the job).
   * @param seedTime
   *          is the end of the time range of documents to consider, exclusive.
   * @param lastSeedVersion
   *          is the last seeding version string for this job, or null if the job has no previous seeding version string.
   * @param jobMode
   *          is an integer describing how the job is being run, whether continuous or once-only.
   * @return an updated seeding version string, to be stored with the job.
   */
  @Override
  public String addSeedDocuments(final ISeedingActivity activities, final Specification spec, final String lastSeedVersion, final long seedTime, final int jobMode) throws ManifoldCFException, ServiceInterruption {
    getSession();

    final DocumentURLFilter filter = new DocumentURLFilter(spec);

    // This is the call that's used to seed everything.
    // We just find the current seeds, and create the appropriate iterator.
    String seeds = "";
    int i = 0;
    while (i < spec.getChildCount()) {
      final SpecificationNode sn = spec.getChild(i++);
      if (sn.getType().equals(WebcrawlerConfig.NODE_SEEDS))
        seeds = sn.getValue();
    }

    // Break up the seeds string and iterate over the results.
    final List<String> list = stringToArray(seeds);
    // We must only return valid urls here!!!
    int index = 0;
    while (index < list.size()) {
      final String urlCandidate = list.get(index++);
      final String documentIdentifier = makeDocumentIdentifier(null, urlCandidate, filter, activities);
      if (documentIdentifier == null) {
        // Bad seed. Log it, and continue!
        Logging.connectors.warn("WEB: Illegal seed URL '" + urlCandidate + "'");
        continue;
      }
      activities.addSeedDocument(documentIdentifier, calculateDocumentEvents(activities, documentIdentifier));
    }
    return "";
  }

  // Session login states (so we can use the same fetch logic multiple times)

  /** Normal fetch of content document. (For all we know, we're logged in already). */
  protected static final int SESSIONSTATE_NORMAL = 0;
  /** We're in 'login mode' */
  protected static final int SESSIONSTATE_LOGIN = 1;

  // Result signals
  protected static final int RESULT_NO_DOCUMENT = 0;
  protected static final int RESULT_NO_VERSION = 1;
  protected static final int RESULT_VERSION_NEEDED = 2;
  protected static final int RESULT_RETRY_DOCUMENT = 3;

  /**
   * Process a set of documents. This is the method that should cause each document to be fetched, processed, and the results either added to the queue of documents for the current job, and/or entered
   * into the incremental ingestion manager. The document specification allows this class to filter what is done based on the job. The connector will be connected before this method can be called.
   *
   * @param documentIdentifiers
   *          is the set of document identifiers to process.
   * @param statuses
   *          are the currently-stored document versions for each document in the set of document identifiers passed in above.
   * @param activities
   *          is the interface this method should use to queue up new document references and ingest documents.
   * @param jobMode
   *          is an integer describing how the job is being run, whether continuous or once-only.
   * @param usesDefaultAuthority
   *          will be true only if the authority in use for these documents is the default one.
   */
  @Override
  public void processDocuments(final String[] documentIdentifiers, final IExistingVersions statuses, final Specification spec, final IProcessActivity activities, final int jobMode, final boolean usesDefaultAuthority)
      throws ManifoldCFException, ServiceInterruption {
    getSession();

    // Forced acls
    final String[] acls = getAcls(spec);
    // Sort it,
    java.util.Arrays.sort(acls);

    // Get the excluded headers
    final Set<String> excludedHeaders = findExcludedHeaders(spec);

    // Since document specifications can change, we need to look at each url and filter it as part of the
    // process of getting version strings. To do that, we need to compile the DocumentSpecification into
    // an object that knows how to do this.
    final DocumentURLFilter filter = new DocumentURLFilter(spec);

    final String filterVersion = filter.getVersionString();

    // There are two ways to handle any document that's not available. The first is to remove it. The second is to keep it, but mark it with an empty version string.
    // With the web crawler, the major concern with simply removing the document is that it might be referred to from multiple places - and in addition
    // it will get requeued every time the parent document is processed. This is not optimal because it represents churn.
    // On the other hand, keeping the document in the queue causes the queue to bloat, which is also not optimal, and it makes the crawler basically
    // incapable of deleting documents.
    // Since the primary use of the crawler is expected to be repeated intranet crawls, I've thus chosen to optimize the crawler for accuracy rather than performance
    // - if the document is gone, I just remove it, and expect churn when recrawling activities occur.
    for (final String documentIdentifier : documentIdentifiers) {
      LOGGER.log(DOCPROCESSLEVEL, "DOC_PROCESS_START|WEBCRAWLER|" + documentIdentifier);
      try {
        // Verify that the url is legal
        if (!filter.isDocumentAndHostLegal(documentIdentifier, activities)) {
          if (Logging.connectors.isDebugEnabled())
            Logging.connectors.debug("WEB: Removing url '" + documentIdentifier + "' because it's not in the set of allowed ones");
          // Use null because we should have already filtered when we queued.
          activities.deleteDocument(documentIdentifier);
          continue;
        }

        try {
          // The first thing we need to know is whether this url is part of a session-protected area. We'll use that information
          // later to detect redirection to login.
          final SequenceCredentials sessionCredential = getSequenceCredential(documentIdentifier);

          if (Logging.connectors.isDebugEnabled()) {
            if (sessionCredential != null)
              Logging.connectors.debug("Web: For document identifier '" + documentIdentifier + "' found session credential key '" + sessionCredential.getSequenceKey() + "'");
          }

          // Set up the initial state and state variables.
          // Fetch status
          final FetchStatus fetchStatus = new FetchStatus();

          // Calculate an event name; we'll need this to control sequencing.
          String globalSequenceEvent;
          if (sessionCredential != null) {
            final String sequenceKey = sessionCredential.getSequenceKey();
            globalSequenceEvent = makeSessionLoginEventName(activities, sequenceKey);
          } else
            globalSequenceEvent = null;

          // This is the main 'state loop'. The code is structured to use the finally clause from the following try to clean up any
          // events that were created within the loop. The loop itself has two parts: document fetch, and logic to figure out what state to transition
          // to (e.g. how to process the fetched document). A signal variable is used to signal the desired outcome.
          // We need to be sure we clean up the sequence event in case there's an error, so put a try/finally around everything.
          try {

            loginAndFetch(fetchStatus, activities, documentIdentifier, sessionCredential, globalSequenceEvent);

            switch (fetchStatus.resultSignal) {
            case RESULT_NO_DOCUMENT:
              if (Logging.connectors.isDebugEnabled())
                Logging.connectors.debug("WEB: Removing url '" + documentIdentifier + "'" + ((fetchStatus.contextMessage != null) ? " because " + fetchStatus.contextMessage : ""), fetchStatus.contextException);
              activities.deleteDocument(documentIdentifier);
              break;
            case RESULT_NO_VERSION:
              if (Logging.connectors.isDebugEnabled())
                Logging.connectors.debug("WEB: Ignoring url '" + documentIdentifier + "'" + ((fetchStatus.contextMessage != null) ? " because " + fetchStatus.contextMessage : ""), fetchStatus.contextException);

              // We get here when a document didn't fetch.
              // No version
              activities.noDocument(documentIdentifier, "");
              break;
            case RESULT_VERSION_NEEDED:
              // Calculate version from document data, which is presumed to be present.
              final StringBuilder sb = new StringBuilder();

              // Acls
              packList(sb, acls, '+');
              if (acls.length > 0) {
                sb.append('+');
                pack(sb, defaultAuthorityDenyToken, '+');
              } else
                sb.append('-');

              // Now, do the metadata.
              final Map<String, Set<String>> metaHash = new HashMap<>();

              final String[] fixedListStrings = new String[2];
              // They're all folded into the same part of the version string.
              int headerCount = 0;
              Iterator<String> headerIterator = fetchStatus.headerData.keySet().iterator();
              while (headerIterator.hasNext()) {
                final String headerName = headerIterator.next();
                final String lowerHeaderName = headerName.toLowerCase(Locale.ROOT);
                if (!reservedHeaders.contains(lowerHeaderName) && !excludedHeaders.contains(lowerHeaderName))
                  headerCount += fetchStatus.headerData.get(headerName).size();
              }
              final String[] fullMetadata = new String[headerCount];
              headerCount = 0;
              headerIterator = fetchStatus.headerData.keySet().iterator();
              while (headerIterator.hasNext()) {
                final String headerName = headerIterator.next();
                final String lowerHeaderName = headerName.toLowerCase(Locale.ROOT);
                if (!reservedHeaders.contains(lowerHeaderName) && !excludedHeaders.contains(lowerHeaderName)) {
                  Set<String> valueSet = metaHash.get(headerName);
                  if (valueSet == null) {
                    valueSet = new HashSet<>();
                    metaHash.put(headerName, valueSet);
                  }
                  final List<String> headerValues = fetchStatus.headerData.get(headerName);
                  for (final String headerValue : headerValues) {
                    valueSet.add(headerValue);
                    fixedListStrings[0] = "header-" + headerName;
                    fixedListStrings[1] = headerValue;
                    final StringBuilder newsb = new StringBuilder();
                    packFixedList(newsb, fixedListStrings, '=');
                    fullMetadata[headerCount++] = newsb.toString();
                  }
                }
              }
              java.util.Arrays.sort(fullMetadata);

              packList(sb, fullMetadata, '+');
              // Done with the parseable part! Add the checksum.
              sb.append(fetchStatus.checkSum);
              // Add the filter version
              sb.append("+");
              sb.append(filterVersion);

              final String versionString = sb.toString();

              // Now, extract links.
              // We'll call the "link extractor" series, so we can plug more stuff in over time.
              final boolean indexDocument = extractLinks(documentIdentifier, activities, filter);

              // If scanOnly is set, we never ingest. But all else is the same.
              if (!activities.checkDocumentNeedsReindexing(documentIdentifier, versionString))
                continue;

              processDocument(activities, documentIdentifier, versionString, indexDocument, metaHash, acls, filter);
              break;
            case RESULT_RETRY_DOCUMENT:
              // Document could not be processed right now.
              if (Logging.connectors.isDebugEnabled())
                Logging.connectors.debug("WEB: Retrying url '" + documentIdentifier + "' later" + ((fetchStatus.contextMessage != null) ? " because " + fetchStatus.contextMessage : ""), fetchStatus.contextException);
              activities.retryDocumentProcessing(documentIdentifier);
              break;
            default:
              throw new IllegalStateException("Unexpected value for result signal: " + Integer.toString(fetchStatus.resultSignal));
            }
          } finally {
            // Clean up event, if there is one.
            if (fetchStatus.sessionState == SESSIONSTATE_LOGIN && globalSequenceEvent != null) {
              // Terminate the event
              activities.completeEventSequence(globalSequenceEvent);
            }
          }
        } finally {
          cache.deleteData(documentIdentifier);
        }
      } catch (final Throwable e) {
        if (e instanceof ManifoldCFException || e instanceof ServiceInterruption) {
          throw e;
        } else {
          activities.recordActivity(System.currentTimeMillis(), ACTIVITY_PROCESS, 0L, documentIdentifier, "KO", "Encountered unhandled exception: " + e.getMessage(), null);
          Logging.connectors.error("Encountered unhandled exception: " + e.getMessage(), e);
        }
      } finally {
        LOGGER.log(DOCPROCESSLEVEL, "DOC_PROCESS_END|WEBCRAWLER|" + documentIdentifier);
      }
    }
  }

  protected void loginAndFetch(final FetchStatus fetchStatus, final IProcessActivity activities, final String documentIdentifier, final SequenceCredentials sessionCredential, final String globalSequenceEvent)
      throws ManifoldCFException, ServiceInterruption {
    final long currentTime = System.currentTimeMillis();
    // Here's the maximum number of connections we are going to allow.
    final int connectionLimit = 200;

    String currentURI = documentIdentifier;

    // Login pages are special in that I *don't* require them to do a robots check. The reason why is because it is conceivable that a
    // site may inadvertantly exclude them via robots, and yet allow content pages to be scanned. This would effectively exclude session login
    // for that site if we adhered to the strict policy. Since login pages have to be exclusively identified as being special, explicit
    // permission is effectively granted by the user in any case.

    // The result code to be activity logging, or null if no activity logging desired.
    String activityResultCode = null;
    // Form data
    FormData formData = null;

    while (true) {
      URL url;
      try {
        // Do the mapping from the current host name to the IP address
        url = new URL(currentURI);
      } catch (final MalformedURLException e) {
        // currentURI is malformed.
        // If the document was the primary, we should remove it from the queue. But if it's part of a login sequence, we'd better just retry later.
        fetchStatus.contextMessage = "was not a valid URL: " + e.getMessage();
        fetchStatus.contextException = e;
        activityResultCode = "-12";
        fetchStatus.resultSignal = RESULT_NO_DOCUMENT;
        break;
      }

      final String hostName = url.getHost();
      final StringBuilder ipAddressBuffer = new StringBuilder();
      final int ipAddressStatus = lookupIPAddress(currentURI, activities, hostName, currentTime, ipAddressBuffer);
      if (ipAddressStatus == RESULTSTATUS_TRUE) {
        final String ipAddress = ipAddressBuffer.toString();
        final String protocol = url.getProtocol();
        int port = url.getPort();
        if (port == -1)
          port = url.getDefaultPort();

        // Try to fetch the document. We'll need its bin names first.
        final String[] binNames = getBinNames(currentURI);

        // Get the credentials for this document (if any)
        final PageCredentials credential = getPageCredential(currentURI);
        IKeystoreManager trustStore;
        // Save effort - only bother to get a trust store if this is https
        if (protocol.equalsIgnoreCase("https"))
          // null return is possible here; indicates "trust everything"
          trustStore = getTrustStore(currentURI);
        else
          trustStore = KeystoreManagerFactory.make("");
        // Check robots, if enabled, and if we're fetching the primary document identifier. See comment above.
        int robotsStatus = RESULTSTATUS_TRUE;
        if (!documentIdentifier.equals(currentURI) || robotsUsage < ROBOTS_DATA || (robotsStatus = checkFetchAllowed(documentIdentifier, protocol, ipAddress, port, credential, trustStore, hostName, binNames, currentTime, url.getFile(), activities,
            connectionLimit, proxyHost, proxyPort, proxyAuthDomain, proxyAuthUsername, proxyAuthPassword)) == RESULTSTATUS_TRUE) {
          // Passed the robots check!

          // Find whatever login parameters apply. This will be null if currentURI is not a login page, and will contain
          // interesting information if it is.
          LoginCookies lc = null;
          if (sessionCredential != null) {
            lc = cookieManager.readCookies(sessionCredential.getSequenceKey());
          }

          // Prepare to perform the fetch, and decide what to do with the document.
          //
          final IThrottledConnection connection = ThrottledFetcher.getConnection(currentContext, throttleGroupName, protocol, ipAddress, port, credential, trustStore, throttleDescription, binNames, connectionLimit, proxyHost, proxyPort,
              proxyAuthDomain, proxyAuthUsername, proxyAuthPassword, socketTimeoutMilliseconds, connectionTimeoutMilliseconds, activities);
          try {
            connection.beginFetch((fetchStatus.sessionState == SESSIONSTATE_LOGIN) ? FETCH_LOGIN : FETCH_STANDARD);
            try {
              // Execute the fetch!
              connection.executeFetch(url.getFile(), userAgent, from, false, hostName, formData, lc);
              final int response = connection.getResponseCode();

              if (response == 200 || response == 302 || response == 301) {
                // If this was part of the login sequence, update the cookies regardless of what else happens
                if (fetchStatus.sessionState == SESSIONSTATE_LOGIN) {
                  // Update the cookies
                  final LoginCookies lastFetchCookies = connection.getLastFetchCookies();
                  cookieManager.updateCookies(sessionCredential.getSequenceKey(), lastFetchCookies);
                }

                // Decide whether to exclude this document based on what we see here.
                // Basically, we want to get rid of everything that we (a) don't know what
                // to do with in the ingestion system, and (b) we can't get useful links from.

                final String contentType = extractContentType(connection.getResponseHeader("Content-Type"));

                if (isContentInteresting(activities, currentURI, response, contentType)) {
                  // Treat it as real, and cache it.
                  fetchStatus.checkSum = cache.addData(activities, currentURI, connection);
                  fetchStatus.headerData = connection.getResponseHeaders();
                  fetchStatus.resultSignal = RESULT_VERSION_NEEDED;
                  activityResultCode = null;
                } else {
                  fetchStatus.contextMessage = "it had the wrong content type ('" + contentType + "')";
                  fetchStatus.resultSignal = RESULT_NO_DOCUMENT;
                  activityResultCode = activities.EXCLUDED_MIMETYPE;
                }
              } else {
                // We got some kind of http error code.
                // We don't want to remove it from the queue entirely, because that would cause us to lose track of the item, and therefore lose
                // control of all scheduling around it. Instead, we leave it on the queue and give it an empty version string; that will lead it to be
                // reprocessed without fail on the next scheduled check.
                // Decode response body to the extent we can
                final String contentType = extractContentType(connection.getResponseHeader("Content-Type"));
                String encoding = extractEncoding(contentType);
                if (encoding == null)
                  encoding = StandardCharsets.UTF_8.name();
                String decodedResponse = "undecodable";
                try {
                  decodedResponse = "'" + connection.getLimitedResponseBody(1024, encoding) + "'";
                } catch (final ManifoldCFException e) {
                  // Eat this exception unless it is an interrupt
                  if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
                    throw e;
                  connection.noteInterrupted(e);
                } catch (final ServiceInterruption e) {
                  // Eat this exception too
                  connection.noteInterrupted(e);
                }
                fetchStatus.contextMessage = "it failed to fetch (status=" + Integer.toString(response) + ", message=" + decodedResponse + ")";
                fetchStatus.resultSignal = RESULT_NO_VERSION;
                activityResultCode = null;
              }
            } catch (final ManifoldCFException e) {
              connection.noteInterrupted(e);
              throw e;
            } catch (final ServiceInterruption e) {
              connection.noteInterrupted(e);
              throw e;
            } finally {
              connection.doneFetch(activities);
            }
          } finally {
            connection.close();
          }

          // State transition logic. If the result indicates a successful fetch so far, we need to decide where to go next.
          // This happens AFTER we've released all the connections, because it's conceivable that processing here might be
          // significant, and we don't want to tie things up unnecessarily.
          String preferredLink = null;
          String preferredRedirection = null;
          formData = null;
          String contentLink = null;
          if (fetchStatus.resultSignal == RESULT_VERSION_NEEDED) {
            // If we get here, we know:
            // (a) There's a cached version of the page on disk we can read as many times as necessary;
            // (b) The saved cookies have not been updated yet, so we'll need to do that where appropriate.

            // The way we determine if we're in the login sequence for a site is by TWO criteria:
            // (1) The URI must match the specified regular expression, and
            // (2) The data from that URI must contain the specified form or link information.
            // We use the same criteria to look for the exit from a sequence. So, in essence, we're *always* going to need to know whether we're
            // officially in the sequence, or not, so we evaluate it always.
            boolean isLoginPage = false;
            if (sessionCredential != null) {
              final Iterator iterMatches = sessionCredential.findLoginParameters(currentURI);
              boolean seenAnything = false;
              boolean seenFormError = false;
              boolean seenLinkError = false;
              boolean seenRedirectionError = false;
              boolean seenContentError = false;
              while (iterMatches.hasNext()) {
                seenAnything = true;
                final LoginParameters lp = (LoginParameters) iterMatches.next();
                // Note that more than one of the rules may match.
                // In that case, a clear order of precedence applies between form-style rules and link-style: form has priority.
                // If more than one of the same kind of rule is seen, then all bets are off, a warning is displayed, and nothing is
                // matched.

                // Parse the page; it had better match up! Otherwise we get null back.
                final FormData newFormData = findHTMLForm(currentURI, lp);
                if (newFormData != null) {
                  if (formData != null) {
                    // Oops, more than one matching form rule. Complain.
                    seenFormError = true;
                    formData = null;
                  } else if (!seenFormError) {
                    // A form overrides links, redirection, or content
                    formData = newFormData;
                    preferredLink = null;
                    preferredRedirection = null;
                  }
                } else {
                  // Look for the preferred link instead.
                  final String newPreferredLink = findHTMLLinkURI(currentURI, lp);
                  if (newPreferredLink != null) {
                    if (preferredLink != null) {
                      // Oops
                      seenLinkError = true;
                      preferredLink = null;
                    } else if (!seenLinkError && !seenFormError && formData == null) {
                      // Link overrides redirection and content
                      preferredLink = newPreferredLink;
                      preferredRedirection = null;
                    }
                  } else {
                    // Look for the preferred redirection.
                    final String newPreferredRedirection = findPreferredRedirectionURI(currentURI, lp);
                    if (newPreferredRedirection != null) {
                      if (preferredRedirection != null) {
                        seenRedirectionError = true;
                        preferredRedirection = null;
                      } else if (!seenRedirectionError && !seenLinkError && !seenFormError && formData == null && preferredLink == null) {
                        preferredRedirection = newPreferredRedirection;
                      }
                    } else {
                      // Look for the content in the page. The link returned may be an empty string, if matching content
                      // is discovered but there is no override. It will be null of the content is not found.
                      final String newContentLink = findSpecifiedContent(currentURI, lp);
                      if (newContentLink != null) {
                        if (contentLink != null) {
                          seenContentError = true;
                          contentLink = null;
                        } else if (!seenContentError && !seenRedirectionError && !seenLinkError && !seenFormError && formData == null && preferredLink == null && preferredRedirection == null) {
                          contentLink = newContentLink;
                        }
                      }
                    }
                  }
                }
              }

              // Now, evaluate all the data and pick the right rule
              if (formData != null) {
                // We found the right form! And, we filled it in. So now we enter the "login sequence".
                if (Logging.connectors.isDebugEnabled())
                  Logging.connectors.debug("WEB: Document '" + currentURI + "' matches form, so determined to be login page for sequence '" + sessionCredential.getSequenceKey() + "'");
                isLoginPage = true;
              } else if (preferredLink != null) {
                if (Logging.connectors.isDebugEnabled())
                  Logging.connectors.debug("WEB: Document '" + currentURI + "' matches preferred link, so determined to be login page for sequence '" + sessionCredential.getSequenceKey() + "'");
                isLoginPage = true;
              } else if (preferredRedirection != null) {
                if (Logging.connectors.isDebugEnabled())
                  Logging.connectors.debug("WEB: Document '" + currentURI + "' matches preferred redirection, so determined to be login page for sequence '" + sessionCredential.getSequenceKey() + "'");
                isLoginPage = true;
              } else if (contentLink != null) {
                if (Logging.connectors.isDebugEnabled())
                  Logging.connectors.debug("WEB: Document '" + currentURI + "' matches content, so determined to be login page for sequence '" + sessionCredential.getSequenceKey() + "'");
                isLoginPage = true;
              } else {
                if (seenAnything && Logging.connectors.isDebugEnabled())
                  Logging.connectors.debug("WEB: Document '" + currentURI + "' did not match expected form, link, redirection, or content for sequence '" + sessionCredential.getSequenceKey() + "'");
              }
            }

            // Should we do a state transition into the "logging in" state?
            if (fetchStatus.sessionState == SESSIONSTATE_NORMAL && isLoginPage) {
              // Entering the login sequence. Make sure we actually can do this...
              if (activities.beginEventSequence(globalSequenceEvent)) {
                if (Logging.connectors.isDebugEnabled())
                  Logging.connectors.debug("WEB: For document '" + documentIdentifier + "', beginning login sequence '" + sessionCredential.getSequenceKey() + "'");

                activities.recordActivity(null, WebcrawlerConnector.ACTIVITY_LOGON_START, null, sessionCredential.getSequenceKey(), "OK", null, null);

                // Transition to the right state, etc.
                fetchStatus.sessionState = SESSIONSTATE_LOGIN;
              } else {
                if (Logging.connectors.isDebugEnabled())
                  Logging.connectors.debug("WEB: For document '" + documentIdentifier + "', login sequence '" + sessionCredential.getSequenceKey() + "' was already in progress.");

                // Didn't make it in. Retry the main URI when the proper conditions are met.
                // We don't want the cached data anymore.
                cache.deleteData(currentURI);
                fetchStatus.contextMessage = "login sequence already in progress";
                fetchStatus.resultSignal = RESULT_RETRY_DOCUMENT;
                activityResultCode = null;
              }
            } else if (fetchStatus.sessionState == SESSIONSTATE_LOGIN && isLoginPage == false) {
              // == Exit login mode ==
              activities.completeEventSequence(globalSequenceEvent);
              activities.recordActivity(null, WebcrawlerConnector.ACTIVITY_LOGON_END, null, sessionCredential.getSequenceKey(), "OK", null, null);
              fetchStatus.sessionState = SESSIONSTATE_NORMAL;
              // Make sure we go back and try the original document again, if we happened to have been directed somewhere else
              if (!currentURI.equals(documentIdentifier)) {
                cache.deleteData(currentURI);
                currentURI = documentIdentifier;
                continue;
              }
              // Otherwise, the last fetch stands on its own. Fall through, and allow processing and link extraction
            }

            // Now, based on the session state and the document contents, decide how to proceed
            if (fetchStatus.resultSignal == RESULT_VERSION_NEEDED && fetchStatus.sessionState == SESSIONSTATE_LOGIN) {
              // We are dealing with a login page!

              // We need to (a) figure out what the next URI should be, and (b) record form information that it might need.
              // This is a bit dicey because there's really
              // no good way to *guarantee* that we pick the right one, if there's more than one available.
              // What we do is the following:
              //
              // (a) We look for matching forms. If we found one, we submit it.
              // (b) Look for redirections.
              // (c) If there are links that vector within the login sequence, we pick one of those preferentially.
              // (d) If there are no links that vector within the login sequence, we pick one of the other links.
              //
              // Note well that it's probably going to be pretty easy to get this code stuck in an infinite login sequence.
              // While that won't be a problem performance-wise (because everything is appropriately throttled), it
              // is obviously not ideal, and furthermore, it will not be possible to crawl a site for which this occurs.
              //
              // Longer time (and with higher complexity) we can solve this problem by allowing the user to *specify*
              // which link they want us to pick for a page. Hopefully this would not be necessary.

              // Locate the next target URI.
              String targetURI;
              if (formData != null)
                targetURI = formData.getActionURI();
              else if (preferredLink != null)
                targetURI = preferredLink;
              else if (preferredRedirection != null)
                targetURI = preferredRedirection;
              else /* if (contentLink != null) */
                targetURI = contentLink;

              // Definitely we don't want the cached data anymore
              cache.deleteData(currentURI);

              // If the target URI is null, it means we could not find a suitable link. If target URI is "",
              // it means that we found a designated logon page but the description did not include a link we
              // could chase. Either way, treat this exactly the same
              // way as if the link found exited login mode.
              if (targetURI == null || targetURI.length() == 0) {
                // == Exiting login mode ==
                activities.completeEventSequence(globalSequenceEvent);
                activities.recordActivity(null, WebcrawlerConnector.ACTIVITY_LOGON_END, null, sessionCredential.getSequenceKey(), "NEXTLINKNOTFOUND", "Could not find a usable link to the next page: " + fetchStatus.contextMessage, null);
                fetchStatus.sessionState = SESSIONSTATE_NORMAL;
                // Make sure we go back and try the original document again, no matter where we got directed to
                currentURI = documentIdentifier;
              } else {
                currentURI = targetURI;
              }
              continue;
            } else if (fetchStatus.resultSignal != RESULT_VERSION_NEEDED && fetchStatus.sessionState == SESSIONSTATE_LOGIN) {
              // The next URL we fetched in the logon sequence turned out to be unsuitable.
              // That means that the logon sequence is fundamentally wrong. The session thus ends,
              // and of course it will retry, but that's neither here nor there.
              // == Exiting login mode ==
              activities.completeEventSequence(globalSequenceEvent);
              activities.recordActivity(null, WebcrawlerConnector.ACTIVITY_LOGON_END, null, sessionCredential.getSequenceKey(), "LINKTARGETUNSUITABLE", "Page was unsuitable for a login sequence because: " + fetchStatus.contextMessage, null);
              fetchStatus.sessionState = SESSIONSTATE_NORMAL;
              // Fall through, leaving everything else alone.
            }
          }

        } else if (robotsStatus == RESULTSTATUS_FALSE) {
          activityResultCode = "-11";
          fetchStatus.contextMessage = "robots.txt says so";
          fetchStatus.resultSignal = RESULT_NO_DOCUMENT;
        } else {
          // Robots prerequisite in progress
          activityResultCode = null;
          fetchStatus.resultSignal = RESULT_RETRY_DOCUMENT;
          fetchStatus.contextMessage = "robots prerequisite already in progress";
        }
      } else if (ipAddressStatus == RESULTSTATUS_FALSE) {
        activityResultCode = "-10";
        fetchStatus.contextMessage = "ip address not found";
        fetchStatus.resultSignal = RESULT_NO_DOCUMENT;
      } else {
        // DNS prerequisite in progress
        activityResultCode = null;
        fetchStatus.contextMessage = "dns prerequisite already in progress";
        fetchStatus.resultSignal = RESULT_RETRY_DOCUMENT;
      }

      // If we fail on a document that's not the primary, the result should be to retry the primary later.
      if (!currentURI.equals(documentIdentifier)) {
        activityResultCode = null;
        if (fetchStatus.contextMessage != null)
          fetchStatus.contextMessage = "for login sequence url '" + currentURI + "': " + fetchStatus.contextMessage;
        if (fetchStatus.resultSignal != RESULT_VERSION_NEEDED)
          fetchStatus.resultSignal = RESULT_RETRY_DOCUMENT;
      }

      break;
    }

    // Now, look at the result signal, and set up the version appropriately.
    if (activityResultCode != null)
      activities.recordActivity(null, ACTIVITY_FETCH, null, documentIdentifier, activityResultCode, fetchStatus.contextMessage, null);

  }

  protected void processDocument(final IProcessActivity activities, final String documentIdentifier, final String versionString, final boolean indexDocument, final Map<String, Set<String>> metaHash, final String[] acls,
      final DocumentURLFilter filter) throws ManifoldCFException, ServiceInterruption {
    // Consider this document for ingestion.
    String errorCode = null;
    String errorDesc = null;
    Long fileLengthLong = null;
    final long startTime = System.currentTimeMillis();

    try {
      // We can exclude it if it does not seem to be a kind of document that the ingestion system knows
      // about.

      if (!indexDocument) {
        errorCode = "CONTENTNOTINDEXABLE";
        errorDesc = "Content not indexable";
        activities.noDocument(documentIdentifier, versionString);
        return;
      }

      final int responseCode = cache.getResponseCode(documentIdentifier);
      if (responseCode != 200) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("Web: For document '" + documentIdentifier + "', not indexing because response code not indexable: " + responseCode);
        errorCode = "RESPONSECODENOTINDEXABLE";
        errorDesc = "HTTP response code not indexable (" + responseCode + ")";
        activities.noDocument(documentIdentifier, versionString);
        return;
      }

      final long dataLength = cache.getDataLength(documentIdentifier);
      if (!activities.checkLengthIndexable(dataLength)) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("Web: For document '" + documentIdentifier + "', not indexing because pipeline thinks length " + dataLength + " is not acceptable");
        errorCode = activities.EXCLUDED_LENGTH;
        errorDesc = "Rejected due to length (" + dataLength + ")";
        activities.noDocument(documentIdentifier, versionString);
        return;
      }

      if (activities.checkURLIndexable(documentIdentifier) == false) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("Web: For document '" + documentIdentifier + "', not indexing because output connector does not want URL");
        errorCode = activities.EXCLUDED_URL;
        errorDesc = "Rejected due to URL ('" + documentIdentifier + "')";
        activities.noDocument(documentIdentifier, versionString);
        return;
      }

      final String ingestURL = filter.isDocumentIndexable(documentIdentifier, activities);
      if (ingestURL == null) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("Web: For document '" + documentIdentifier + "', not indexing because document does not match web job constraints");
        errorCode = "JOBRESTRICTION";
        errorDesc = "Rejected because job excludes this URL ('" + documentIdentifier + "')";
        activities.noDocument(documentIdentifier, versionString);
        return;
      }

      // Check if it's a recognized content type
      String contentType = cache.getContentType(documentIdentifier);

      // Some sites have multiple content types. We just look at the LAST one in that case.
      if (contentType != null) {
        final String[] contentTypes = contentType.split(",");
        if (contentTypes.length > 0)
          contentType = contentTypes[contentTypes.length - 1].trim();
        else
          contentType = null;
      }

      if (contentType != null) {
        final int pos = contentType.indexOf(";");
        if (pos != -1)
          contentType = contentType.substring(0, pos);
        contentType = contentType.trim();
      }

      if (!activities.checkMimeTypeIndexable(contentType)) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("Web: For document '" + documentIdentifier + "', not indexing because output connector does not want mime type '" + contentType + "'");
        errorCode = activities.EXCLUDED_MIMETYPE;
        errorDesc = "Rejected because of mime type (" + contentType + ")";
        activities.noDocument(documentIdentifier, versionString);
        return;
      }

      if (!filter.isDocumentContentIndexable(documentIdentifier)) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("Web: For document '" + documentIdentifier + "', not indexing because document content matched document content exclusion rule");
        errorCode = activities.EXCLUDED_CONTENT;
        errorDesc = "Rejected due to content exclusion rule";
        activities.noDocument(documentIdentifier, versionString);
        return;
      }
      // Ingest the document
      if (Logging.connectors.isDebugEnabled())
        Logging.connectors.debug("WEB: Decided to ingest '" + documentIdentifier + "'");

      final RepositoryDocument rd = new RepositoryDocument();

      // Set the file name
      String fileName = "";
      try {
        fileName = documentIdentifiertoFileName(documentIdentifier);
      } catch (final URISyntaxException e1) {
        fileName = "";
      }
      if (fileName.length() > 0) {
        rd.setFileName(fileName);
      }

      // Set the content type
      final String mimeType = cache.getContentType(documentIdentifier);
      if (mimeType != null)
        rd.setMimeType(mimeType);

      // Turn into acls and add into description
      String[] denyAcls;
      if (acls == null)
        denyAcls = null;
      else {
        if (acls.length > 0)
          denyAcls = new String[] { defaultAuthorityDenyToken };
        else
          denyAcls = new String[0];
      }

      if (acls != null && denyAcls != null)
        rd.setSecurity(RepositoryDocument.SECURITY_TYPE_DOCUMENT, acls, denyAcls);

      // Grab metadata
      for (final String key : metaHash.keySet()) {
        final Set<String> metaList = metaHash.get(key);
        final String[] values = new String[metaList.size()];
        int k = 0;
        for (final String value : metaList) {
          values[k++] = value;
        }
        rd.addField(key, values);
      }

      final InputStream is = cache.getData(documentIdentifier);

      if (is != null) {
        try {
          rd.setBinary(is, dataLength);
          try {
            activities.ingestDocumentWithException(documentIdentifier, versionString, ingestURL, rd);
            errorCode = "OK";
            fileLengthLong = new Long(dataLength);
          } catch (final IOException e) {
            errorCode = e.getClass().getSimpleName().toUpperCase(Locale.ROOT);
            errorDesc = e.getMessage();
            handleIOException(e, "reading data");
          }
        } finally {
          try {
            is.close();
          } catch (final IOException e) {
            errorCode = e.getClass().getSimpleName().toUpperCase(Locale.ROOT);
            errorDesc = e.getMessage();
            handleIOException(e, "closing stream");
          }
        }
      } else
        Logging.connectors.error("WEB: Expected a cached document for '" + documentIdentifier + "', but none present!");

    } catch (final ManifoldCFException e) {
      if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
        errorCode = null;
      throw e;
    } finally {
      if (errorCode != null)
        activities.recordActivity(new Long(startTime), ACTIVITY_PROCESS, fileLengthLong, documentIdentifier, errorCode, errorDesc, null);
    }

  }

  protected static String extractContentType(String contentType) {
    // Some sites have multiple content types. We just look at the LAST one in that case.
    if (contentType != null) {
      final String[] contentTypes = contentType.split(",");
      if (contentTypes.length > 0)
        contentType = contentTypes[contentTypes.length - 1].trim();
      else
        contentType = null;
    }
    return contentType;
  }

  protected static String extractEncoding(final String contentType) {
    if (contentType == null)
      return null;
    final int semiIndex = contentType.indexOf(";");
    if (semiIndex == -1)
      return null;
    String suffix = contentType.substring(semiIndex + 1);
    suffix = suffix.trim();
    if (suffix.startsWith("charset=")) {
      final String trialSuffix = suffix.substring("charset=".length());
      final int semi = trialSuffix.indexOf(";");
      if (semi == -1) {
        return trialSuffix;
      }
      // Strip off additional crap some websites now add
      return trialSuffix.substring(0, semi).trim();
    }
    return null;
  }

  protected static String extractMimeType(String contentType) {
    if (contentType == null)
      return null;
    final int semiIndex = contentType.indexOf(";");
    if (semiIndex != -1)
      contentType = contentType.substring(0, semiIndex);
    contentType = contentType.trim();
    return contentType;
  }

  protected static void handleIOException(final IOException e, final String context) throws ManifoldCFException, ServiceInterruption {
    if (e instanceof java.net.SocketException)
      throw new ManifoldCFException("Socket timeout error " + context + ": " + e.getMessage(), e);
    else if (e instanceof ConnectTimeoutException)
      throw new ManifoldCFException("Socket connect timeout error " + context + ": " + e.getMessage(), e);
    else if (e instanceof InterruptedIOException)
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
    else
      throw new ManifoldCFException("IO error " + context + ": " + e.getMessage(), e);
  }

  /**
   * Get the maximum number of documents to amalgamate together into one batch, for this connector.
   *
   * @return the maximum number. 0 indicates "unlimited".
   */
  @Override
  public int getMaxDocumentRequest() {
    // The web in general does not batch well. Multiple chunks have no advantage over one-at-a-time requests.
    return 1;
  }

  // UI support methods.
  //
  // These support methods come in two varieties. The first bunch is involved in setting up connection configuration information. The second bunch
  // is involved in presenting and editing document specification information for a job. The two kinds of methods are accordingly treated differently,
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
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Email"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Robots"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Bandwidth"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.AccessCredentials"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Certificates"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Proxy"));

    final Map<String, Object> velocityContext = new HashMap<>();
    Messages.outputResourceWithVelocity(out, locale, "editConfiguration.js.vm", velocityContext);
  }

  private void fillInEmailTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) {
    String email = parameters.getParameter(WebcrawlerConfig.PARAMETER_EMAIL);
    if (email == null)
      email = "";

    velocityContext.put("EMAIL", email);
  }

  private void fillInRobotsTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) {
    String robotsUsage = parameters.getParameter(WebcrawlerConfig.PARAMETER_ROBOTSUSAGE);
    if (robotsUsage == null)
      robotsUsage = "all";
    String metaRobotsTagsUsage = parameters.getParameter(WebcrawlerConfig.PARAMETER_META_ROBOTS_TAGS_USAGE);
    if (metaRobotsTagsUsage == null)
      metaRobotsTagsUsage = "all";

    velocityContext.put("ROBOTSUSAGE", robotsUsage);
    velocityContext.put("METAROBOTSTAGSUSAGE", metaRobotsTagsUsage);
  }

  private void fillInBandwidthTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) {
    int i = 0;
    int binCounter = 0;
    final List<Map<String, String>> throttlesMapList = new ArrayList<>();
    while (i < parameters.getChildCount()) {
      final ConfigNode cn = parameters.getChild(i++);
      if (cn.getType().equals(WebcrawlerConfig.NODE_BINDESC)) {
        final Map<String, String> throttleMap = new HashMap<>();
        // A bin description node! Look for all its parameters.
        String regexp = cn.getAttributeValue(WebcrawlerConfig.ATTR_BINREGEXP);
        String isCaseInsensitive = cn.getAttributeValue(WebcrawlerConfig.ATTR_INSENSITIVE);
        String maxConnections = null;
        String maxKBPerSecond = null;
        String maxFetchesPerMinute = null;
        int j = 0;
        while (j < cn.getChildCount()) {
          final ConfigNode childNode = cn.getChild(j++);
          if (childNode.getType().equals(WebcrawlerConfig.NODE_MAXCONNECTIONS))
            maxConnections = childNode.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
          else if (childNode.getType().equals(WebcrawlerConfig.NODE_MAXKBPERSECOND))
            maxKBPerSecond = childNode.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
          else if (childNode.getType().equals(WebcrawlerConfig.NODE_MAXFETCHESPERMINUTE))
            maxFetchesPerMinute = childNode.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
        }
        if (maxConnections == null)
          maxConnections = "";
        if (maxKBPerSecond == null)
          maxKBPerSecond = "";
        if (maxFetchesPerMinute == null)
          maxFetchesPerMinute = "";
        if (regexp == null)
          regexp = "";

        if (isCaseInsensitive == null || isCaseInsensitive.length() == 0)
          isCaseInsensitive = "false";

        throttleMap.put("regexp", regexp);
        throttleMap.put("isCaseInsensitive", isCaseInsensitive);
        throttleMap.put("maxConnections", maxConnections);
        throttleMap.put("maxKBPerSecond", maxKBPerSecond);
        throttleMap.put("maxFetchesPerMinute", maxFetchesPerMinute);
        throttlesMapList.add(throttleMap);
        binCounter++;
      }
    }
    if (parameters.getChildCount() == 0) {
      velocityContext.put("BRANDNEW", true);
    }
    velocityContext.put("THROTTLESMAPLIST", throttlesMapList);
  }

  private void fillInProxyTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) {
    String proxyHost = parameters.getParameter(WebcrawlerConfig.PARAMETER_PROXYHOST);
    if (proxyHost == null)
      proxyHost = "";
    String proxyPort = parameters.getParameter(WebcrawlerConfig.PARAMETER_PROXYPORT);
    if (proxyPort == null)
      proxyPort = "";
    String proxyAuthDomain = parameters.getParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHDOMAIN);
    if (proxyAuthDomain == null)
      proxyAuthDomain = "";
    String proxyAuthUsername = parameters.getParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHUSERNAME);
    if (proxyAuthUsername == null)
      proxyAuthUsername = "";
    String proxyAuthPassword = parameters.getObfuscatedParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHPASSWORD);
    if (proxyAuthPassword == null)
      proxyAuthPassword = "";
    else
      proxyAuthPassword = out.mapPasswordToKey(proxyAuthPassword);

    velocityContext.put("PROXYHOST", proxyHost);
    velocityContext.put("PROXYPORT", proxyPort);
    velocityContext.put("PROXYAUTHDOMAIN", proxyAuthDomain);
    velocityContext.put("PROXYAUTHUSERNAME", proxyAuthUsername);
    velocityContext.put("PROXYAUTHPASSWORD", proxyAuthPassword);
  }

  private void fillInCertificatesTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) throws ManifoldCFException {
    int i = 0;
    final List<Map<String, String>> trustMapList = new ArrayList<>();
    while (i < parameters.getChildCount()) {
      final ConfigNode cn = parameters.getChild(i++);
      if (cn.getType().equals(WebcrawlerConfig.NODE_TRUST)) {
        final Map<String, String> trustMap = new HashMap<>();

        // A bin description node! Look for all its parameters.
        final String regexp = cn.getAttributeValue(WebcrawlerConfig.ATTR_URLREGEXP);
        String trustEverything = cn.getAttributeValue(WebcrawlerConfig.ATTR_TRUSTEVERYTHING);

        if (trustEverything == null) {
          trustEverything = "false";
        }

        trustMap.put("trustEverything", trustEverything);
        trustMap.put("regexp", regexp);

        if (trustEverything == "false") {
          final String trustStore = cn.getAttributeValue(WebcrawlerConfig.ATTR_TRUSTSTORE);
          IKeystoreManager localTruststore;
          if (trustStore == null) {
            localTruststore = KeystoreManagerFactory.make("");
          } else {
            localTruststore = KeystoreManagerFactory.make("", trustStore);
          }
          final String[] truststoreContents = localTruststore.getContents();

          // Each trust store will have only at most one cert in it at this level. These individual certs are assembled into the proper trust store
          // for each individual url at fetch time.

          if (truststoreContents.length == 1) {
            final String alias = truststoreContents[0];
            final String description = localTruststore.getDescription(alias);
            String shortenedDescription = description;
            if (shortenedDescription.length() > 100)
              shortenedDescription = shortenedDescription.substring(0, 100) + "...";

            trustMap.put("trustStore", trustStore);
            trustMap.put("shortenedDescription", shortenedDescription);
          }
        }
        trustMapList.add(trustMap);
      }
    }
    velocityContext.put("TRUSTMAPLIST", trustMapList);
  }

  private void fillInAccessTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) throws ManifoldCFException {
    int i = 0;
    final List<Map<String, String>> pageAccessMapList = new ArrayList<>();
    while (i < parameters.getChildCount()) {
      final ConfigNode cn = parameters.getChild(i++);
      if (cn.getType().equals(WebcrawlerConfig.NODE_ACCESSCREDENTIAL)) {
        final Map<String, String> pageAccessMap = new HashMap<>();

        // A bin description node! Look for all its parameters.
        final String type = cn.getAttributeValue(WebcrawlerConfig.ATTR_TYPE);
        if (!type.equals(WebcrawlerConfig.ATTRVALUE_SESSION)) {
          String regexp = cn.getAttributeValue(WebcrawlerConfig.ATTR_URLREGEXP);
          if (regexp == null)
            regexp = "";
          String domain = cn.getAttributeValue(WebcrawlerConfig.ATTR_DOMAIN);
          if (domain == null)
            domain = "";
          final String userName = cn.getAttributeValue(WebcrawlerConfig.ATTR_USERNAME);
          final String password = out.mapPasswordToKey(ManifoldCF.deobfuscate(cn.getAttributeValue(WebcrawlerConfig.ATTR_PASSWORD)));

          pageAccessMap.put("regexp", regexp);
          pageAccessMap.put("domain", domain);
          pageAccessMap.put("userName", userName);
          pageAccessMap.put("password", password);
          pageAccessMap.put("type", type);

          pageAccessMapList.add(pageAccessMap);
        }
      }
    }
    velocityContext.put("PAGEACCESSMAPLIST", pageAccessMapList);

    i = 0;
    final List<Map<String, Object>> sessionAccessMapList = new ArrayList<>();
    while (i < parameters.getChildCount()) {
      final ConfigNode cn = parameters.getChild(i++);
      if (cn.getType().equals(WebcrawlerConfig.NODE_ACCESSCREDENTIAL)) {
        // A bin description node! Look for all its parameters.
        final String type = cn.getAttributeValue(WebcrawlerConfig.ATTR_TYPE);
        if (type.equals(WebcrawlerConfig.ATTRVALUE_SESSION)) {
          final Map<String, Object> sessionAccessMap = new HashMap<>();
          String regexp = cn.getAttributeValue(WebcrawlerConfig.ATTR_URLREGEXP);
          if (regexp == null)
            regexp = "";
          sessionAccessMap.put("regexp", regexp);

          int q = 0;
          final List<Map<String, Object>> authPageMapList = new ArrayList<>();
          while (q < cn.getChildCount()) {
            final ConfigNode authPageNode = cn.getChild(q++);
            if (authPageNode.getType().equals(WebcrawlerConfig.NODE_AUTHPAGE)) {
              final Map<String, Object> authPageMap = new HashMap<>();

              final String pageRegexp = authPageNode.getAttributeValue(WebcrawlerConfig.ATTR_URLREGEXP);
              final String pageType = authPageNode.getAttributeValue(WebcrawlerConfig.ATTR_TYPE);
              String matchRegexp = authPageNode.getAttributeValue(WebcrawlerConfig.ATTR_MATCHREGEXP);
              if (matchRegexp == null)
                matchRegexp = "";
              String overrideTargetURL = authPageNode.getAttributeValue(WebcrawlerConfig.ATTR_OVERRIDETARGETURL);
              if (overrideTargetURL == null)
                overrideTargetURL = "";

              authPageMap.put("pageRegexp", pageRegexp);
              authPageMap.put("pageType", pageType);
              authPageMap.put("matchRegexp", matchRegexp);
              authPageMap.put("overrideTargetURL", overrideTargetURL);

              if (pageType.equals(WebcrawlerConfig.ATTRVALUE_FORM)) {
                int z = 0;
                final List<Map<String, String>> authPageParamMapList = new ArrayList<>();
                while (z < authPageNode.getChildCount()) {
                  final ConfigNode paramNode = authPageNode.getChild(z++);
                  if (paramNode.getType().equals(WebcrawlerConfig.NODE_AUTHPARAMETER)) {
                    final Map<String, String> authPageParamMap = new HashMap<>();

                    String param = paramNode.getAttributeValue(WebcrawlerConfig.ATTR_NAMEREGEXP);
                    if (param == null)
                      param = "";
                    String value = paramNode.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
                    if (value == null)
                      value = "";
                    String password = paramNode.getAttributeValue(WebcrawlerConfig.ATTR_PASSWORD);
                    if (password == null)
                      password = "";
                    else
                      password = out.mapPasswordToKey(ManifoldCF.deobfuscate(password));

                    authPageParamMap.put("param", param);
                    authPageParamMap.put("value", value);
                    authPageParamMap.put("password", password);

                    authPageParamMapList.add(authPageParamMap);
                  }
                }
                authPageMap.put("authPageParamMapList", authPageParamMapList);
              }
              authPageMapList.add(authPageMap);
            }
          }
          sessionAccessMap.put("authPageMapList", authPageMapList);
          sessionAccessMapList.add(sessionAccessMap);
        }
      }
    }
    velocityContext.put("SESSIONACCESSMAPLIST", sessionAccessMapList);
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

    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("TABNAME", tabName);

    fillInEmailTab(velocityContext, out, parameters);
    fillInRobotsTab(velocityContext, out, parameters);
    fillInBandwidthTab(velocityContext, out, parameters);
    fillInAccessTab(velocityContext, out, parameters);
    fillInCertificatesTab(velocityContext, out, parameters);
    fillInProxyTab(velocityContext, out, parameters);

    // Email tab
    Messages.outputResourceWithVelocity(out, locale, "editConfiguration_Email.html.vm", velocityContext);
    // Robots tab
    Messages.outputResourceWithVelocity(out, locale, "editConfiguration_Robots.html.vm", velocityContext);
    // Bandwidth tab
    Messages.outputResourceWithVelocity(out, locale, "editConfiguration_Bandwidth.html.vm", velocityContext);
    // Access Credentials tab
    Messages.outputResourceWithVelocity(out, locale, "editConfiguration_Access.html.vm", velocityContext);
    // Certificates tab
    Messages.outputResourceWithVelocity(out, locale, "editConfiguration_Certificates.html.vm", velocityContext);
    // Proxy tab
    Messages.outputResourceWithVelocity(out, locale, "editConfiguration_Proxy.html.vm", velocityContext);

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
    final String email = variableContext.getParameter("email");
    if (email != null)
      parameters.setParameter(WebcrawlerConfig.PARAMETER_EMAIL, email);
    final String robotsUsage = variableContext.getParameter("robotsusage");
    if (robotsUsage != null)
      parameters.setParameter(WebcrawlerConfig.PARAMETER_ROBOTSUSAGE, robotsUsage);
    final String obeyMetaRobotsTags = variableContext.getParameter("metarobotstagsusage");
    if (obeyMetaRobotsTags != null)
      parameters.setParameter(WebcrawlerConfig.PARAMETER_META_ROBOTS_TAGS_USAGE, obeyMetaRobotsTags);
    final String proxyHost = variableContext.getParameter("proxyhost");
    if (proxyHost != null)
      parameters.setParameter(WebcrawlerConfig.PARAMETER_PROXYHOST, proxyHost);
    final String proxyPort = variableContext.getParameter("proxyport");
    if (proxyPort != null)
      parameters.setParameter(WebcrawlerConfig.PARAMETER_PROXYPORT, proxyPort);
    final String proxyAuthDomain = variableContext.getParameter("proxyauthdomain");
    if (proxyAuthDomain != null)
      parameters.setParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHDOMAIN, proxyAuthDomain);
    final String proxyAuthUsername = variableContext.getParameter("proxyauthusername");
    if (proxyAuthUsername != null)
      parameters.setParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHUSERNAME, proxyAuthUsername);
    final String proxyAuthPassword = variableContext.getParameter("proxyauthpassword");
    if (proxyAuthPassword != null)
      parameters.setObfuscatedParameter(WebcrawlerConfig.PARAMETER_PROXYAUTHPASSWORD, variableContext.mapKeyToPassword(proxyAuthPassword));

    String x = variableContext.getParameter("bandwidth_count");
    if (x != null && x.length() > 0) {
      // About to gather the bandwidth nodes, so get rid of the old ones.
      int i = 0;
      while (i < parameters.getChildCount()) {
        final ConfigNode node = parameters.getChild(i);
        if (node.getType().equals(WebcrawlerConfig.NODE_BINDESC))
          parameters.removeChild(i);
        else
          i++;
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = "bandwidth_" + Integer.toString(i);
        final String op = variableContext.getParameter("op_" + prefix);
        if (op == null || !op.equals("Delete")) {
          // Gather the regexp etc.
          final String regexp = variableContext.getParameter("regexp_" + prefix);
          final String isCaseInsensitive = variableContext.getParameter("insensitive_" + prefix);
          final String maxConnections = variableContext.getParameter("connections_" + prefix);
          final String rate = variableContext.getParameter("rate_" + prefix);
          final String fetches = variableContext.getParameter("fetches_" + prefix);
          final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_BINDESC);
          node.setAttribute(WebcrawlerConfig.ATTR_BINREGEXP, regexp);
          if (isCaseInsensitive != null && isCaseInsensitive.length() > 0)
            node.setAttribute(WebcrawlerConfig.ATTR_INSENSITIVE, isCaseInsensitive);
          if (maxConnections != null && maxConnections.length() > 0) {
            final ConfigNode child = new ConfigNode(WebcrawlerConfig.NODE_MAXCONNECTIONS);
            child.setAttribute(WebcrawlerConfig.ATTR_VALUE, maxConnections);
            node.addChild(node.getChildCount(), child);
          }
          if (rate != null && rate.length() > 0) {
            final ConfigNode child = new ConfigNode(WebcrawlerConfig.NODE_MAXKBPERSECOND);
            child.setAttribute(WebcrawlerConfig.ATTR_VALUE, rate);
            node.addChild(node.getChildCount(), child);
          }
          if (fetches != null && fetches.length() > 0) {
            final ConfigNode child = new ConfigNode(WebcrawlerConfig.NODE_MAXFETCHESPERMINUTE);
            child.setAttribute(WebcrawlerConfig.ATTR_VALUE, fetches);
            node.addChild(node.getChildCount(), child);
          }
          parameters.addChild(parameters.getChildCount(), node);
        }
        i++;
      }
      final String addop = variableContext.getParameter("bandwidth_op");
      if (addop != null && addop.equals("Add")) {
        final String regexp = variableContext.getParameter("regexp_bandwidth");
        final String isCaseInsensitive = variableContext.getParameter("insensitive_bandwidth");
        final String maxConnections = variableContext.getParameter("connections_bandwidth");
        final String rate = variableContext.getParameter("rate_bandwidth");
        final String fetches = variableContext.getParameter("fetches_bandwidth");
        final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_BINDESC);
        node.setAttribute(WebcrawlerConfig.ATTR_BINREGEXP, regexp);
        if (isCaseInsensitive != null && isCaseInsensitive.length() > 0)
          node.setAttribute(WebcrawlerConfig.ATTR_INSENSITIVE, isCaseInsensitive);
        if (maxConnections != null && maxConnections.length() > 0) {
          final ConfigNode child = new ConfigNode(WebcrawlerConfig.NODE_MAXCONNECTIONS);
          child.setAttribute(WebcrawlerConfig.ATTR_VALUE, maxConnections);
          node.addChild(node.getChildCount(), child);
        }
        if (rate != null && rate.length() > 0) {
          final ConfigNode child = new ConfigNode(WebcrawlerConfig.NODE_MAXKBPERSECOND);
          child.setAttribute(WebcrawlerConfig.ATTR_VALUE, rate);
          node.addChild(node.getChildCount(), child);
        }
        if (fetches != null && fetches.length() > 0) {
          final ConfigNode child = new ConfigNode(WebcrawlerConfig.NODE_MAXFETCHESPERMINUTE);
          child.setAttribute(WebcrawlerConfig.ATTR_VALUE, fetches);
          node.addChild(node.getChildCount(), child);
        }
        parameters.addChild(parameters.getChildCount(), node);
      }
    }

    x = variableContext.getParameter("acredential_count");
    if (x != null && x.length() > 0) {
      // About to gather the access credential nodes, so get rid of the old ones.
      int i = 0;
      while (i < parameters.getChildCount()) {
        final ConfigNode node = parameters.getChild(i);
        if (node.getType().equals(WebcrawlerConfig.NODE_ACCESSCREDENTIAL) && !node.getAttributeValue(WebcrawlerConfig.ATTR_TYPE).equals(WebcrawlerConfig.ATTRVALUE_SESSION))
          parameters.removeChild(i);
        else
          i++;
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = "acredential_" + Integer.toString(i);
        final String op = variableContext.getParameter("op_" + prefix);
        if (op == null || !op.equals("Delete")) {
          // Gather the regexp etc.
          final String regexp = variableContext.getParameter("regexp_" + prefix);
          final String type = variableContext.getParameter("type_" + prefix);
          String domain = variableContext.getParameter("domain_" + prefix);
          if (domain == null)
            domain = "";
          final String userName = variableContext.getParameter("username_" + prefix);
          final String password = variableContext.getParameter("password_" + prefix);
          final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_ACCESSCREDENTIAL);
          node.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, regexp);
          node.setAttribute(WebcrawlerConfig.ATTR_TYPE, type);
          node.setAttribute(WebcrawlerConfig.ATTR_DOMAIN, domain);
          node.setAttribute(WebcrawlerConfig.ATTR_USERNAME, userName);
          node.setAttribute(WebcrawlerConfig.ATTR_PASSWORD, ManifoldCF.obfuscate(variableContext.mapKeyToPassword(password)));
          parameters.addChild(parameters.getChildCount(), node);
        }
        i++;
      }
      final String addop = variableContext.getParameter("acredential_op");
      if (addop != null && addop.equals("Add")) {
        final String regexp = variableContext.getParameter("regexp_acredential");
        final String type = variableContext.getParameter("type_acredential");
        final String domain = variableContext.getParameter("domain_acredential");
        final String userName = variableContext.getParameter("username_acredential");
        final String password = variableContext.getParameter("password_acredential");
        final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_ACCESSCREDENTIAL);
        node.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, regexp);
        node.setAttribute(WebcrawlerConfig.ATTR_TYPE, type);
        node.setAttribute(WebcrawlerConfig.ATTR_DOMAIN, domain);
        node.setAttribute(WebcrawlerConfig.ATTR_USERNAME, userName);
        node.setAttribute(WebcrawlerConfig.ATTR_PASSWORD, ManifoldCF.obfuscate(password));
        parameters.addChild(parameters.getChildCount(), node);
      }
    }

    x = variableContext.getParameter("scredential_count");
    if (x != null && x.length() > 0) {
      // About to gather the access credential nodes, so get rid of the old ones.
      int i = 0;
      while (i < parameters.getChildCount()) {
        final ConfigNode node = parameters.getChild(i);
        if (node.getType().equals(WebcrawlerConfig.NODE_ACCESSCREDENTIAL) && node.getAttributeValue(WebcrawlerConfig.ATTR_TYPE).equals(WebcrawlerConfig.ATTRVALUE_SESSION))
          parameters.removeChild(i);
        else
          i++;
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = "scredential_" + Integer.toString(i);
        final String op = variableContext.getParameter(prefix + "_op");
        if (op == null || !op.equals("Delete")) {
          // Gather the regexp etc.
          final String regexp = variableContext.getParameter(prefix + "_regexp");
          final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_ACCESSCREDENTIAL);
          node.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, regexp);
          node.setAttribute(WebcrawlerConfig.ATTR_TYPE, WebcrawlerConfig.ATTRVALUE_SESSION);
          // How many login pages are there?
          final int loginPageCount = Integer.parseInt(variableContext.getParameter(prefix + "_loginpagecount"));
          int q = 0;
          while (q < loginPageCount) {
            final String authpagePrefix = prefix + "_" + Integer.toString(q);
            final String authpageOp = variableContext.getParameter(authpagePrefix + "_op");
            if (authpageOp == null || !authpageOp.equals("Delete")) {
              final String pageRegexp = variableContext.getParameter(authpagePrefix + "_regexp");
              final String pageType = variableContext.getParameter(authpagePrefix + "_type");
              String matchRegexp = variableContext.getParameter(authpagePrefix + "_matchregexp");
              if (matchRegexp == null)
                matchRegexp = "";
              final String overrideTargetURL = variableContext.getParameter(authpagePrefix + "_overridetargeturl");
              final ConfigNode authPageNode = new ConfigNode(WebcrawlerConfig.NODE_AUTHPAGE);
              authPageNode.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, pageRegexp);
              authPageNode.setAttribute(WebcrawlerConfig.ATTR_TYPE, pageType);
              authPageNode.setAttribute(WebcrawlerConfig.ATTR_MATCHREGEXP, matchRegexp);
              if (overrideTargetURL != null && overrideTargetURL.length() > 0)
                authPageNode.setAttribute(WebcrawlerConfig.ATTR_OVERRIDETARGETURL, overrideTargetURL);
              if (pageType.equals(WebcrawlerConfig.ATTRVALUE_FORM)) {
                // How many parameters are there?
                final int paramCount = Integer.parseInt(variableContext.getParameter(authpagePrefix + "_loginparamcount"));
                int z = 0;
                while (z < paramCount) {
                  final String paramPrefix = authpagePrefix + "_" + Integer.toString(z);
                  final String paramOp = variableContext.getParameter(paramPrefix + "_op");
                  if (paramOp == null || !paramOp.equals("Delete")) {
                    final String name = variableContext.getParameter(paramPrefix + "_param");
                    final String value = variableContext.getParameter(paramPrefix + "_value");
                    final String password = variableContext.getParameter(paramPrefix + "_password");
                    final ConfigNode paramNode = new ConfigNode(WebcrawlerConfig.NODE_AUTHPARAMETER);
                    paramNode.setAttribute(WebcrawlerConfig.ATTR_NAMEREGEXP, name);
                    if (value != null && value.length() > 0)
                      paramNode.setAttribute(WebcrawlerConfig.ATTR_VALUE, value);
                    if (password != null && password.length() > 0)
                      paramNode.setAttribute(WebcrawlerConfig.ATTR_PASSWORD, ManifoldCF.obfuscate(variableContext.mapKeyToPassword(password)));
                    authPageNode.addChild(authPageNode.getChildCount(), paramNode);
                  }
                  z++;
                }

                // Look for add op
                final String paramAddOp = variableContext.getParameter(authpagePrefix + "_loginparamop");
                if (paramAddOp != null && paramAddOp.equals("Add")) {
                  final String name = variableContext.getParameter(authpagePrefix + "_loginparamname");
                  final String value = variableContext.getParameter(authpagePrefix + "_loginparamvalue");
                  final String password = variableContext.getParameter(authpagePrefix + "_loginparampassword");
                  final ConfigNode paramNode = new ConfigNode(WebcrawlerConfig.NODE_AUTHPARAMETER);
                  paramNode.setAttribute(WebcrawlerConfig.ATTR_NAMEREGEXP, name);
                  if (value != null && value.length() > 0)
                    paramNode.setAttribute(WebcrawlerConfig.ATTR_VALUE, value);
                  if (password != null && password.length() > 0)
                    paramNode.setAttribute(WebcrawlerConfig.ATTR_PASSWORD, ManifoldCF.obfuscate(password));
                  authPageNode.addChild(authPageNode.getChildCount(), paramNode);
                }
              }

              node.addChild(node.getChildCount(), authPageNode);
            }
            q++;
          }
          // Look for add op
          final String authpageAddop = variableContext.getParameter(prefix + "_loginpageop");
          if (authpageAddop != null && authpageAddop.equals("Add")) {
            final String pageRegexp = variableContext.getParameter(prefix + "_loginpageregexp");
            final String pageType = variableContext.getParameter(prefix + "_loginpagetype");
            String matchRegexp = variableContext.getParameter(prefix + "_loginpagematchregexp");
            if (matchRegexp == null)
              matchRegexp = "";
            final String overrideTargetURL = variableContext.getParameter(prefix + "_loginpageoverridetargeturl");
            final ConfigNode authPageNode = new ConfigNode(WebcrawlerConfig.NODE_AUTHPAGE);
            authPageNode.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, pageRegexp);
            authPageNode.setAttribute(WebcrawlerConfig.ATTR_TYPE, pageType);
            authPageNode.setAttribute(WebcrawlerConfig.ATTR_MATCHREGEXP, matchRegexp);
            if (overrideTargetURL != null && overrideTargetURL.length() > 0)
              authPageNode.setAttribute(WebcrawlerConfig.ATTR_OVERRIDETARGETURL, overrideTargetURL);
            node.addChild(node.getChildCount(), authPageNode);
          }

          parameters.addChild(parameters.getChildCount(), node);
        }
        i++;
      }
      final String addop = variableContext.getParameter("scredential_op");
      if (addop != null && addop.equals("Add")) {
        final String regexp = variableContext.getParameter("scredential_regexp");
        final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_ACCESSCREDENTIAL);
        node.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, regexp);
        node.setAttribute(WebcrawlerConfig.ATTR_TYPE, WebcrawlerConfig.ATTRVALUE_SESSION);
        parameters.addChild(parameters.getChildCount(), node);
      }
    }

    x = variableContext.getParameter("trust_count");
    if (x != null && x.length() > 0) {
      // About to gather the trust nodes, so get rid of the old ones.
      int i = 0;
      while (i < parameters.getChildCount()) {
        final ConfigNode node = parameters.getChild(i);
        if (node.getType().equals(WebcrawlerConfig.NODE_TRUST))
          parameters.removeChild(i);
        else
          i++;
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = "trust_" + Integer.toString(i);
        final String op = variableContext.getParameter("op_" + prefix);
        if (op == null || !op.equals("Delete")) {
          // Gather the regexp etc.
          final String regexp = variableContext.getParameter("regexp_" + prefix);
          final String trustall = variableContext.getParameter("trustall_" + prefix);
          final String truststore = variableContext.getParameter("truststore_" + prefix);
          final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_TRUST);
          node.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, regexp);
          if (trustall != null && trustall.equals("true"))
            node.setAttribute(WebcrawlerConfig.ATTR_TRUSTEVERYTHING, "true");
          else
            node.setAttribute(WebcrawlerConfig.ATTR_TRUSTSTORE, truststore);
          parameters.addChild(parameters.getChildCount(), node);
        }
        i++;
      }
      final String addop = variableContext.getParameter("trust_op");
      if (addop != null && addop.equals("Add")) {
        final String regexp = variableContext.getParameter("regexp_trust");
        final String trustall = variableContext.getParameter("all_trust");
        if (trustall != null && trustall.equals("true")) {
          final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_TRUST);
          node.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, regexp);
          node.setAttribute(WebcrawlerConfig.ATTR_TRUSTEVERYTHING, "true");
          parameters.addChild(parameters.getChildCount(), node);
        } else {
          final byte[] certificateValue = variableContext.getBinaryBytes("certificate_trust");
          final IKeystoreManager mgr = KeystoreManagerFactory.make("");
          final java.io.InputStream is = new java.io.ByteArrayInputStream(certificateValue);
          String certError = null;
          try {
            mgr.importCertificate("Certificate", is);
          } catch (final Throwable e) {
            certError = e.getMessage();
          } finally {
            try {
              is.close();
            } catch (final IOException e) {
              // Ignore this
            }
          }

          if (certError != null) {
            // Redirect to error page
            return "Illegal certificate: " + certError;
          }

          final ConfigNode node = new ConfigNode(WebcrawlerConfig.NODE_TRUST);
          node.setAttribute(WebcrawlerConfig.ATTR_URLREGEXP, regexp);
          node.setAttribute(WebcrawlerConfig.ATTR_TRUSTSTORE, mgr.getString());
          parameters.addChild(parameters.getChildCount(), node);
        }
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

    final Map<String, Object> velocityContext = new HashMap<>();

    fillInEmailTab(velocityContext, out, parameters);
    fillInRobotsTab(velocityContext, out, parameters);
    fillInBandwidthTab(velocityContext, out, parameters);
    fillInAccessTab(velocityContext, out, parameters);
    fillInCertificatesTab(velocityContext, out, parameters);
    fillInProxyTab(velocityContext, out, parameters);

    Messages.outputResourceWithVelocity(out, locale, "viewConfiguration.html.vm", velocityContext);

  }

  /**
   * Output the specification header section. This method is called in the head section of a job page which has selected a repository connection of the current type. Its purpose is to add the required
   * tabs to the list, and to output any javascript methods that might be needed by the job editing HTML. The connector will be connected before this method can be called.
   *
   * @param out
   *          is the output to which any HTML should be sent.
   * @param locale
   *          is the locale the output is preferred to be in.
   * @param ds
   *          is the current document specification for this job.
   * @param connectionSequenceNumber
   *          is the unique number of this connection within the job.
   * @param tabsArray
   *          is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputSpecificationHeader(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber, final List<String> tabsArray) throws ManifoldCFException, IOException {
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Seeds"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Canonicalization"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.URLMappings"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Inclusions"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Exclusions"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Security"));
    tabsArray.add(Messages.getString(locale, "WebcrawlerConnector.Metadata"));

    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("SEQNUM", connectionSequenceNumber);

    Messages.outputResourceWithVelocity(out, locale, "editSpecification.js.vm", velocityContext);
  }

  private void fillInSeedsTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final Specification ds) {

    int i;
    String seeds = "";

    i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(WebcrawlerConfig.NODE_SEEDS)) {
        seeds = sn.getValue();
        if (seeds == null)
          seeds = "";
      }
    }

    velocityContext.put("SEEDS", seeds);

  }

  private void fillInCanonicalizationTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final Locale locale, final Specification ds) {

    int q = 0;
    final List<Map<String, String>> canonicalizationMapList = new ArrayList<>();
    while (q < ds.getChildCount()) {
      final SpecificationNode specNode = ds.getChild(q++);
      if (specNode.getType().equals(WebcrawlerConfig.NODE_URLSPEC)) {
        final Map<String, String> canonicalizationMap = new HashMap<>();
        // Ok, this node matters to us
        final String regexpString = specNode.getAttributeValue(WebcrawlerConfig.ATTR_REGEXP);
        String description = specNode.getAttributeValue(WebcrawlerConfig.ATTR_DESCRIPTION);
        if (description == null)
          description = "";
        String allowReorder = specNode.getAttributeValue(WebcrawlerConfig.ATTR_REORDER);
        String allowReorderOutput;
        if (allowReorder == null || allowReorder.length() == 0) {
          allowReorder = WebcrawlerConfig.ATTRVALUE_NO;
          allowReorderOutput = Messages.getBodyString(locale, "WebcrawlerConnector.no");
        } else
          allowReorderOutput = allowReorder.equals(WebcrawlerConfig.ATTRVALUE_NO) ? Messages.getBodyString(locale, "WebcrawlerConnector.no") : Messages.getBodyString(locale, "WebcrawlerConnector.yes");
        String allowJavaSessionRemoval = specNode.getAttributeValue(WebcrawlerConfig.ATTR_JAVASESSIONREMOVAL);
        String allowJavaSessionRemovalOutput;
        if (allowJavaSessionRemoval == null || allowJavaSessionRemoval.length() == 0) {
          allowJavaSessionRemoval = WebcrawlerConfig.ATTRVALUE_NO;
          allowJavaSessionRemovalOutput = Messages.getBodyString(locale, "WebcrawlerConnector.no");
        } else
          allowJavaSessionRemovalOutput = allowJavaSessionRemoval.equals(WebcrawlerConfig.ATTRVALUE_NO) ? Messages.getBodyString(locale, "WebcrawlerConnector.no") : Messages.getBodyString(locale, "WebcrawlerConnector.yes");
        ;
        String allowASPSessionRemoval = specNode.getAttributeValue(WebcrawlerConfig.ATTR_ASPSESSIONREMOVAL);
        String allowASPSessionRemovalOutput;
        if (allowASPSessionRemoval == null || allowASPSessionRemoval.length() == 0) {
          allowASPSessionRemoval = WebcrawlerConfig.ATTRVALUE_NO;
          allowASPSessionRemovalOutput = Messages.getBodyString(locale, "WebcrawlerConnector.no");
        } else
          allowASPSessionRemovalOutput = allowASPSessionRemoval.equals(WebcrawlerConfig.ATTRVALUE_NO) ? Messages.getBodyString(locale, "WebcrawlerConnector.no") : Messages.getBodyString(locale, "WebcrawlerConnector.yes");
        ;
        String allowPHPSessionRemoval = specNode.getAttributeValue(WebcrawlerConfig.ATTR_PHPSESSIONREMOVAL);
        String allowPHPSessionRemovalOutput;
        if (allowPHPSessionRemoval == null || allowPHPSessionRemoval.length() == 0) {
          allowPHPSessionRemoval = WebcrawlerConfig.ATTRVALUE_NO;
          allowPHPSessionRemovalOutput = Messages.getBodyString(locale, "WebcrawlerConnector.no");
        } else
          allowPHPSessionRemovalOutput = allowPHPSessionRemoval.equals(WebcrawlerConfig.ATTRVALUE_NO) ? Messages.getBodyString(locale, "WebcrawlerConnector.no") : Messages.getBodyString(locale, "WebcrawlerConnector.yes");
        ;
        String allowBVSessionRemoval = specNode.getAttributeValue(WebcrawlerConfig.ATTR_BVSESSIONREMOVAL);
        String allowBVSessionRemovalOutput;
        if (allowBVSessionRemoval == null || allowBVSessionRemoval.length() == 0) {
          allowBVSessionRemoval = WebcrawlerConfig.ATTRVALUE_NO;
          allowBVSessionRemovalOutput = Messages.getBodyString(locale, "WebcrawlerConnector.no");
        } else
          allowBVSessionRemovalOutput = allowBVSessionRemoval.equals(WebcrawlerConfig.ATTRVALUE_NO) ? Messages.getBodyString(locale, "WebcrawlerConnector.no") : Messages.getBodyString(locale, "WebcrawlerConnector.yes");
        ;
        String allowLowercasing = specNode.getAttributeValue(WebcrawlerConfig.ATTR_LOWERCASE);
        String allowLowercasingOutput;
        if (allowLowercasing == null || allowLowercasing.length() == 0) {
          allowLowercasing = WebcrawlerConfig.ATTRVALUE_NO;
          allowLowercasingOutput = Messages.getBodyString(locale, "WebcrawlerConnector.no");
        } else
          allowLowercasingOutput = allowLowercasing.equals(WebcrawlerConfig.ATTRVALUE_NO) ? Messages.getBodyString(locale, "WebcrawlerConnector.no") : Messages.getBodyString(locale, "WebcrawlerConnector.yes");
        ;

        canonicalizationMap.put("regexpString", regexpString);
        canonicalizationMap.put("description", description);
        canonicalizationMap.put("allowReorder", allowReorder);
        canonicalizationMap.put("allowReorderOutput", allowReorderOutput);
        canonicalizationMap.put("allowJavaSessionRemoval", allowJavaSessionRemoval);
        canonicalizationMap.put("allowJavaSessionRemovalOutput", allowJavaSessionRemovalOutput);
        canonicalizationMap.put("allowASPSessionRemoval", allowASPSessionRemoval);
        canonicalizationMap.put("allowASPSessionRemovalOutput", allowASPSessionRemovalOutput);
        canonicalizationMap.put("allowPHPSessionRemoval", allowPHPSessionRemoval);
        canonicalizationMap.put("allowPHPSessionRemovalOutput", allowPHPSessionRemovalOutput);
        canonicalizationMap.put("allowBVSessionRemoval", allowBVSessionRemoval);
        canonicalizationMap.put("allowBVSessionRemovalOutput", allowBVSessionRemovalOutput);
        canonicalizationMap.put("allowLowercasing", allowLowercasing);
        canonicalizationMap.put("allowLowercasingOutput", allowLowercasingOutput);

        canonicalizationMapList.add(canonicalizationMap);
      }
    }

    velocityContext.put("CANONICALIZATIONMAPLIST", canonicalizationMapList);

  }

  private void fillInMappingsTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final Specification ds) {

    int i;
    // Find the various strings
    final List<String> regexpList = new ArrayList<>();
    final List<String> matchStrings = new ArrayList<>();

    i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(WebcrawlerConfig.NODE_MAP)) {
        final String match = sn.getAttributeValue(WebcrawlerConfig.ATTR_MATCH);
        String map = sn.getAttributeValue(WebcrawlerConfig.ATTR_MAP);
        if (match != null) {
          regexpList.add(match);
          if (map == null)
            map = "";
          matchStrings.add(map);
        }
      }
    }

    velocityContext.put("REGEXPLIST", regexpList);
    velocityContext.put("MATCHSTRINGS", matchStrings);

  }

  private void fillInInclusionsTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final Specification ds) {

    int i;
    String inclusions = ".*\n";
    String inclusionsIndex = ".*\n";
    boolean includeMatching = true;

    i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(WebcrawlerConfig.NODE_INCLUDES)) {
        inclusions = sn.getValue();
        if (inclusions == null)
          inclusions = "";
      } else if (sn.getType().equals(WebcrawlerConfig.NODE_INCLUDESINDEX)) {
        inclusionsIndex = sn.getValue();
        if (inclusionsIndex == null)
          inclusionsIndex = "";
      } else if (sn.getType().equals(WebcrawlerConfig.NODE_LIMITTOSEEDS)) {
        final String value = sn.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
        if (value == null || value.equals("false"))
          includeMatching = false;
        else
          includeMatching = true;
      }
    }

    velocityContext.put("INCLUSIONS", inclusions);
    velocityContext.put("INCLUSIONSINDEX", inclusionsIndex);
    velocityContext.put("INCLUDEMATCHING", includeMatching);

  }

  private void fillInExclusionsTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final Specification ds) {

    int i;
    String exclusions = "";
    String exclusionsIndex = "";
    String exclusionsContentIndex = "";

    i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDES)) {
        exclusions = sn.getValue();
        if (exclusions == null)
          exclusions = "";
      } else if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDESINDEX)) {
        exclusionsIndex = sn.getValue();
        if (exclusionsIndex == null)
          exclusionsIndex = "";
      } else if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDESCONTENTINDEX)) {
        exclusionsContentIndex = sn.getValue();
        if (exclusionsContentIndex == null)
          exclusionsContentIndex = "";
      }
    }

    velocityContext.put("EXCLUSIONS", exclusions);
    velocityContext.put("EXCLUSIONSINDEX", exclusionsIndex);
    velocityContext.put("EXCLUSIONSCONTENTINDEX", exclusionsContentIndex);

  }

  private void fillInSecurityTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final Specification ds) {

    int i = 0;

    // Go through forced ACL
    final Set<String> tokens = new HashSet<>();
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(WebcrawlerConfig.NODE_ACCESS)) {
        final String token = sn.getAttributeValue(WebcrawlerConfig.ATTR_TOKEN);
        tokens.add(token);
      }
    }

    velocityContext.put("TOKENS", tokens);

  }

  private void fillInMetadataTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final Specification ds) {

    final Set<String> excludedHeaders = new HashSet<>();

    // Now, loop through description
    int i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDEHEADER)) {
        final String value = sn.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
        excludedHeaders.add(value);
      }
    }

    velocityContext.put("POTENTIALLYEXCLUDEDHEADERS", potentiallyExcludedHeaders);
    velocityContext.put("EXCLUDEDHEADERS", excludedHeaders);

  }

  /**
   * Output the specification body section. This method is called in the body section of a job page which has selected a repository connection of the current type. Its purpose is to present the
   * required form elements for editing. The coder can presume that the HTML that is output from this configuration will be within appropriate &lt;html&gt;, &lt;body&gt;, and &lt;form&gt; tags. The
   * name of the form is always "editjob". The connector will be connected before this method can be called.
   *
   * @param out
   *          is the output to which any HTML should be sent.
   * @param locale
   *          is the locale the output is preferred to be in.
   * @param ds
   *          is the current document specification for this job.
   * @param connectionSequenceNumber
   *          is the unique number of this connection within the job.
   * @param actualSequenceNumber
   *          is the connection within the job that has currently been selected.
   * @param tabName
   *          is the current tab name. (actualSequenceNumber, tabName) form a unique tuple within the job.
   */
  @Override
  public void outputSpecificationBody(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber, final int actualSequenceNumber, final String tabName) throws ManifoldCFException, IOException {

    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("TABNAME", tabName);
    velocityContext.put("SEQNUM", Integer.toString(connectionSequenceNumber));
    velocityContext.put("SELECTEDNUM", Integer.toString(actualSequenceNumber));

    fillInSeedsTab(velocityContext, out, ds);
    fillInCanonicalizationTab(velocityContext, out, locale, ds);
    fillInMappingsTab(velocityContext, out, ds);
    fillInInclusionsTab(velocityContext, out, ds);
    fillInExclusionsTab(velocityContext, out, ds);
    fillInSecurityTab(velocityContext, out, ds);
    fillInMetadataTab(velocityContext, out, ds);

    // Seeds tab
    Messages.outputResourceWithVelocity(out, locale, "editSpecification_Seeds.html.vm", velocityContext);
    // Canonicalization tab
    Messages.outputResourceWithVelocity(out, locale, "editSpecification_Canonicalization.html.vm", velocityContext);
    // Mappings tab
    Messages.outputResourceWithVelocity(out, locale, "editSpecification_Mappings.html.vm", velocityContext);
    // Inclusions tab
    Messages.outputResourceWithVelocity(out, locale, "editSpecification_Inclusions.html.vm", velocityContext);
    // Exclusions tab
    Messages.outputResourceWithVelocity(out, locale, "editSpecification_Exclusions.html.vm", velocityContext);
    // Security tab
    Messages.outputResourceWithVelocity(out, locale, "editSpecification_Security.html.vm", velocityContext);
    // "Metadata" tab
    Messages.outputResourceWithVelocity(out, locale, "editSpecification_Metadata.html.vm", velocityContext);

  }

  /**
   * Process a specification post. This method is called at the start of job's edit or view page, whenever there is a possibility that form data for a connection has been posted. Its purpose is to
   * gather form information and modify the document specification accordingly. The name of the posted form is always "editjob". The connector will be connected before this method can be called.
   *
   * @param variableContext
   *          contains the post data, including binary file-upload information.
   * @param locale
   *          is the locale the output is preferred to be in.
   * @param ds
   *          is the current document specification for this job.
   * @param connectionSequenceNumber
   *          is the unique number of this connection within the job.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the job (and cause a redirection to an error page).
   */
  @Override
  public String processSpecificationPost(final IPostParameters variableContext, final Locale locale, final Specification ds, final int connectionSequenceNumber) throws ManifoldCFException {
    final String seqPrefix = "s" + connectionSequenceNumber + "_";

    // Get the map
    final String value = variableContext.getParameter(seqPrefix + "rssmapcount");
    if (value != null) {
      final int mapsize = Integer.parseInt(value);

      // Clear it first
      int j = 0;
      while (j < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(j);
        if (sn.getType().equals(WebcrawlerConfig.NODE_MAP))
          ds.removeChild(j);
        else
          j++;
      }

      // Grab the map values
      j = 0;
      while (j < mapsize) {
        final String prefix = seqPrefix + "rssregexp_" + Integer.toString(j) + "_";
        final String match = variableContext.getParameter(prefix + "match");
        String map = variableContext.getParameter(prefix + "map");
        if (map == null)
          map = "";
        // Add to the specification
        final SpecificationNode node = new SpecificationNode(WebcrawlerConfig.NODE_MAP);
        node.setAttribute(WebcrawlerConfig.ATTR_MATCH, match);
        node.setAttribute(WebcrawlerConfig.ATTR_MAP, map);
        ds.addChild(ds.getChildCount(), node);

        j++;
      }
    }
    // Now, do whatever action we were told to do.
    final String rssop = variableContext.getParameter(seqPrefix + "rssop");
    if (rssop != null && rssop.equals("Add")) {
      // Add a match to the end
      final String match = variableContext.getParameter(seqPrefix + "rssmatch");
      final String map = variableContext.getParameter(seqPrefix + "rssmap");
      final SpecificationNode node = new SpecificationNode(WebcrawlerConfig.NODE_MAP);
      node.setAttribute(WebcrawlerConfig.ATTR_MATCH, match);
      node.setAttribute(WebcrawlerConfig.ATTR_MAP, map);
      ds.addChild(ds.getChildCount(), node);
    } else if (rssop != null && rssop.equals("Delete")) {
      int index = Integer.parseInt(variableContext.getParameter(seqPrefix + "rssindex"));
      int j = 0;
      while (j < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(j);
        if (sn.getType().equals(WebcrawlerConfig.NODE_MAP)) {
          if (index == 0) {
            ds.removeChild(j);
            break;
          }
          index--;
        }
        j++;
      }
    }

    // Get excluded headers
    final String excludedHeadersPresent = variableContext.getParameter(seqPrefix + "excludedheaders_present");
    if (excludedHeadersPresent != null) {
      // Delete existing excludedheader record first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDEHEADER))
          ds.removeChild(i);
        else
          i++;
      }
      final String[] excludedHeaders = variableContext.getParameterValues(seqPrefix + "excludedheaders");
      if (excludedHeaders != null) {
        for (final String excludedHeader : excludedHeaders) {
          final SpecificationNode cn = new SpecificationNode(WebcrawlerConfig.NODE_EXCLUDEHEADER);
          cn.setAttribute(WebcrawlerConfig.ATTR_VALUE, excludedHeader);
          ds.addChild(ds.getChildCount(), cn);
        }
      }
    }

    // Get the seeds
    final String seeds = variableContext.getParameter(seqPrefix + "seeds");
    if (seeds != null) {
      // Delete existing seeds record first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_SEEDS))
          ds.removeChild(i);
        else
          i++;
      }

      final SpecificationNode cn = new SpecificationNode(WebcrawlerConfig.NODE_SEEDS);
      cn.setValue(seeds);
      ds.addChild(ds.getChildCount(), cn);
    }

    // Get the inclusions
    final String inclusions = variableContext.getParameter(seqPrefix + "inclusions");
    if (inclusions != null) {
      // Delete existing inclusions record first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_INCLUDES))
          ds.removeChild(i);
        else
          i++;
      }

      final SpecificationNode cn = new SpecificationNode(WebcrawlerConfig.NODE_INCLUDES);
      cn.setValue(inclusions);
      ds.addChild(ds.getChildCount(), cn);
    }

    // Get the index inclusions
    final String inclusionsIndex = variableContext.getParameter(seqPrefix + "inclusionsindex");
    if (inclusionsIndex != null) {
      // Delete existing index inclusions record first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_INCLUDESINDEX))
          ds.removeChild(i);
        else
          i++;
      }

      final SpecificationNode cn = new SpecificationNode(WebcrawlerConfig.NODE_INCLUDESINDEX);
      cn.setValue(inclusionsIndex);
      ds.addChild(ds.getChildCount(), cn);
    }

    // Handle the seeds-only switch
    final String matchingHostsPresent = variableContext.getParameter(seqPrefix + "matchinghosts_present");
    if (matchingHostsPresent != null) {
      // Delete existing switch record first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_LIMITTOSEEDS))
          ds.removeChild(i);
        else
          i++;
      }

      final String matchingHosts = variableContext.getParameter(seqPrefix + "matchinghosts");
      final SpecificationNode cn = new SpecificationNode(WebcrawlerConfig.NODE_LIMITTOSEEDS);
      cn.setAttribute(WebcrawlerConfig.ATTR_VALUE, (matchingHosts == null || matchingHosts.equals("false")) ? "false" : "true");
      ds.addChild(ds.getChildCount(), cn);
    }

    // Get the exclusions
    final String exclusions = variableContext.getParameter(seqPrefix + "exclusions");
    if (exclusions != null) {
      // Delete existing exclusions record first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDES))
          ds.removeChild(i);
        else
          i++;
      }

      final SpecificationNode cn = new SpecificationNode(WebcrawlerConfig.NODE_EXCLUDES);
      cn.setValue(exclusions);
      ds.addChild(ds.getChildCount(), cn);
    }

    // Get the index exclusions
    final String exclusionsIndex = variableContext.getParameter(seqPrefix + "exclusionsindex");
    if (exclusionsIndex != null) {
      // Delete existing exclusions record first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDESINDEX))
          ds.removeChild(i);
        else
          i++;
      }

      final SpecificationNode cn = new SpecificationNode(WebcrawlerConfig.NODE_EXCLUDESINDEX);
      cn.setValue(exclusionsIndex);
      ds.addChild(ds.getChildCount(), cn);
    }

    // Get the content index exclusions
    final String exclusionsContentIndex = variableContext.getParameter(seqPrefix + "exclusionscontentindex");
    if (exclusionsContentIndex != null) {
      // Delete existing content exclusions record first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDESCONTENTINDEX))
          ds.removeChild(i);
        else
          i++;
      }

      final SpecificationNode cn = new SpecificationNode(WebcrawlerConfig.NODE_EXCLUDESCONTENTINDEX);
      cn.setValue(exclusionsContentIndex);
      ds.addChild(ds.getChildCount(), cn);
    }

    // Read the url specs
    final String urlRegexpCount = variableContext.getParameter(seqPrefix + "urlregexpcount");
    if (urlRegexpCount != null && urlRegexpCount.length() > 0) {
      final int regexpCount = Integer.parseInt(urlRegexpCount);
      int j = 0;
      while (j < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(j);
        if (sn.getType().equals(WebcrawlerConfig.NODE_URLSPEC))
          ds.removeChild(j);
        else
          j++;
      }

      // Grab the operation and the index (if any)
      String operation = variableContext.getParameter(seqPrefix + "urlregexpop");
      if (operation == null)
        operation = "Continue";
      int opIndex = -1;
      if (operation.equals("Delete"))
        opIndex = Integer.parseInt(variableContext.getParameter(seqPrefix + "urlregexpnumber"));

      // Reconstruct urlspec nodes
      j = 0;
      while (j < regexpCount) {
        // For each index, first look for a delete operation
        if (!operation.equals("Delete") || j != opIndex) {
          // Add the jth node
          final String regexp = variableContext.getParameter(seqPrefix + "urlregexp_" + Integer.toString(j));
          final String regexpDescription = variableContext.getParameter(seqPrefix + "urlregexpdesc_" + Integer.toString(j));
          final String reorder = variableContext.getParameter(seqPrefix + "urlregexpreorder_" + Integer.toString(j));
          final String javaSession = variableContext.getParameter(seqPrefix + "urlregexpjava_" + Integer.toString(j));
          final String aspSession = variableContext.getParameter(seqPrefix + "urlregexpasp_" + Integer.toString(j));
          final String phpSession = variableContext.getParameter(seqPrefix + "urlregexpphp_" + Integer.toString(j));
          final String bvSession = variableContext.getParameter(seqPrefix + "urlregexpbv_" + Integer.toString(j));
          final String lowercasing = variableContext.getParameter(seqPrefix + "urlregexplowercasing_" + Integer.toString(j));
          final SpecificationNode newSn = new SpecificationNode(WebcrawlerConfig.NODE_URLSPEC);
          newSn.setAttribute(WebcrawlerConfig.ATTR_REGEXP, regexp);
          if (regexpDescription != null && regexpDescription.length() > 0)
            newSn.setAttribute(WebcrawlerConfig.ATTR_DESCRIPTION, regexpDescription);
          if (reorder != null && reorder.length() > 0)
            newSn.setAttribute(WebcrawlerConfig.ATTR_REORDER, reorder);
          if (javaSession != null && javaSession.length() > 0)
            newSn.setAttribute(WebcrawlerConfig.ATTR_JAVASESSIONREMOVAL, javaSession);
          if (aspSession != null && aspSession.length() > 0)
            newSn.setAttribute(WebcrawlerConfig.ATTR_ASPSESSIONREMOVAL, aspSession);
          if (phpSession != null && phpSession.length() > 0)
            newSn.setAttribute(WebcrawlerConfig.ATTR_PHPSESSIONREMOVAL, phpSession);
          if (bvSession != null && bvSession.length() > 0)
            newSn.setAttribute(WebcrawlerConfig.ATTR_BVSESSIONREMOVAL, bvSession);
          if (lowercasing != null && lowercasing.length() > 0)
            newSn.setAttribute(WebcrawlerConfig.ATTR_LOWERCASE, lowercasing);
          ds.addChild(ds.getChildCount(), newSn);
        }
        j++;
      }
      if (operation.equals("Add")) {
        final String regexp = variableContext.getParameter(seqPrefix + "urlregexp");
        final String regexpDescription = variableContext.getParameter(seqPrefix + "urlregexpdesc");
        final String reorder = variableContext.getParameter(seqPrefix + "urlregexpreorder");
        final String javaSession = variableContext.getParameter(seqPrefix + "urlregexpjava");
        final String aspSession = variableContext.getParameter(seqPrefix + "urlregexpasp");
        final String phpSession = variableContext.getParameter(seqPrefix + "urlregexpphp");
        final String bvSession = variableContext.getParameter(seqPrefix + "urlregexpbv");
        final String lowercasing = variableContext.getParameter(seqPrefix + "urlregexplowercasing");

        // Add a new node at the end
        final SpecificationNode newSn = new SpecificationNode(WebcrawlerConfig.NODE_URLSPEC);
        newSn.setAttribute(WebcrawlerConfig.ATTR_REGEXP, regexp);
        if (regexpDescription != null && regexpDescription.length() > 0)
          newSn.setAttribute(WebcrawlerConfig.ATTR_DESCRIPTION, regexpDescription);
        if (reorder != null && reorder.length() > 0)
          newSn.setAttribute(WebcrawlerConfig.ATTR_REORDER, reorder);
        if (javaSession != null && javaSession.length() > 0)
          newSn.setAttribute(WebcrawlerConfig.ATTR_JAVASESSIONREMOVAL, javaSession);
        if (aspSession != null && aspSession.length() > 0)
          newSn.setAttribute(WebcrawlerConfig.ATTR_ASPSESSIONREMOVAL, aspSession);
        if (phpSession != null && phpSession.length() > 0)
          newSn.setAttribute(WebcrawlerConfig.ATTR_PHPSESSIONREMOVAL, phpSession);
        if (bvSession != null && bvSession.length() > 0)
          newSn.setAttribute(WebcrawlerConfig.ATTR_BVSESSIONREMOVAL, bvSession);
        if (lowercasing != null && lowercasing.length() > 0)
          newSn.setAttribute(WebcrawlerConfig.ATTR_LOWERCASE, lowercasing);
        ds.addChild(ds.getChildCount(), newSn);
      }
    }

    String xc = variableContext.getParameter(seqPrefix + "tokencount");
    if (xc != null) {
      // Delete all tokens first
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(WebcrawlerConfig.NODE_ACCESS))
          ds.removeChild(i);
        else
          i++;
      }

      final int accessCount = Integer.parseInt(xc);
      i = 0;
      while (i < accessCount) {
        final String accessDescription = "_" + Integer.toString(i);
        final String accessOpName = seqPrefix + "accessop" + accessDescription;
        xc = variableContext.getParameter(accessOpName);
        if (xc != null && xc.equals("Delete")) {
          // Next row
          i++;
          continue;
        }
        // Get the stuff we need
        final String accessSpec = variableContext.getParameter(seqPrefix + "spectoken" + accessDescription);
        final SpecificationNode node = new SpecificationNode(WebcrawlerConfig.NODE_ACCESS);
        node.setAttribute(WebcrawlerConfig.ATTR_TOKEN, accessSpec);
        ds.addChild(ds.getChildCount(), node);
        i++;
      }

      final String op = variableContext.getParameter(seqPrefix + "accessop");
      if (op != null && op.equals("Add")) {
        final String accessspec = variableContext.getParameter(seqPrefix + "spectoken");
        final SpecificationNode node = new SpecificationNode(WebcrawlerConfig.NODE_ACCESS);
        node.setAttribute(WebcrawlerConfig.ATTR_TOKEN, accessspec);
        ds.addChild(ds.getChildCount(), node);
      }
    }

    return null;
  }

  /**
   * View specification. This method is called in the body section of a job's view page. Its purpose is to present the document specification information to the user. The coder can presume that the
   * HTML that is output from this configuration will be within appropriate &lt;html&gt; and &lt;body&gt;tags. The connector will be connected before this method can be called.
   *
   * @param out
   *          is the output to which any HTML should be sent.
   * @param locale
   *          is the locale the output is preferred to be in.
   * @param ds
   *          is the current document specification for this job.
   * @param connectionSequenceNumber
   *          is the unique number of this connection within the job.
   */
  @Override
  public void viewSpecification(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber) throws ManifoldCFException, IOException {

    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("SEQNUM", Integer.toString(connectionSequenceNumber));

    fillInSeedsTab(velocityContext, out, ds);
    fillInCanonicalizationTab(velocityContext, out, locale, ds);
    fillInMappingsTab(velocityContext, out, ds);
    fillInInclusionsTab(velocityContext, out, ds);
    fillInExclusionsTab(velocityContext, out, ds);
    fillInSecurityTab(velocityContext, out, ds);
    fillInMetadataTab(velocityContext, out, ds);

    Messages.outputResourceWithVelocity(out, locale, "viewSpecification.html.vm", velocityContext);

  }

  // Protected methods and classes

  /**
   * Calculate the event name for session login.
   */
  protected String makeSessionLoginEventName(final INamingActivity activities, final String sequenceKey) {
    return activities.createGlobalString("webcrawler:session:" + sequenceKey);
  }

  /**
   * Calculate the event name for DNS access.
   */
  protected String makeDNSEventName(final INamingActivity activities, final String hostNameKey) {
    // Use the connector unique id, and use a convention that guarantees uniqueness.
    return activities.createGlobalString("webcrawler:dns:" + hostNameKey);
  }

  /**
   * Look up an ipaddress given a non-canonical host name.
   *
   * @return appropriate status.
   */
  protected int lookupIPAddress(final String documentIdentifier, final IProcessActivity activities, final String hostName, final long currentTime, final StringBuilder ipAddressBuffer) throws ManifoldCFException, ServiceInterruption {
    final String eventName = makeDNSEventName(activities, hostName);
    final DNSManager.DNSInfo info = dnsManager.lookup(hostName, currentTime);
    if (info != null) {
      final String ipAddress = info.getIPAddress();
      if (ipAddress == null)
        return RESULTSTATUS_FALSE;
      ipAddressBuffer.append(ipAddress);
      return RESULTSTATUS_TRUE;
    }
    if (activities.beginEventSequence(eventName)) {
      // We uniquely can do the lookup.
      try {
        // Fetch it using InetAddress
        InetAddress ip = null;
        try {
          ip = InetAddress.getByName(hostName);
        } catch (final UnknownHostException e) {
          // Host is unknown, so leave ipAddress as null.
        }
        String fqdn = null;
        String ipAddress = null;
        if (ip != null) {
          fqdn = ip.getCanonicalHostName();
          ipAddress = ip.getHostAddress();
        }
        // Write this to the cache - expiration time 6 hours
        dnsManager.writeDNSData(hostName, fqdn, ipAddress, currentTime + 1000 * 60 * 60 * 6);
        if (ipAddress == null)
          return RESULTSTATUS_FALSE;
        ipAddressBuffer.append(ipAddress);
        return RESULTSTATUS_TRUE;
      } finally {
        activities.completeEventSequence(eventName);
      }
    } else {
      // Abort this fetch, since it's blocked.
      return RESULTSTATUS_NOTYETDETERMINED;
    }
  }

  /**
   * Construct the robots key for a host. This is used to look up robots info in the database, and to form the corresponding event name.
   */
  protected static String makeRobotsKey(final String protocol, final String hostName, final int port) {
    String rval = hostName + ":" + port;
    // For backwards compatibility, only tack on the protocol if the protocol is not http
    if (!protocol.equalsIgnoreCase("http"))
      rval = protocol.toLowerCase(Locale.ROOT) + ":" + rval;
    return rval;
  }

  /**
   * Construct a name for the global web-connector robots event.
   */
  protected String makeRobotsEventName(final INamingActivity versionActivities, final String robotsKey) {
    // Use the connector unique id, and use a convention that guarantees uniqueness.
    return versionActivities.createGlobalString("webcrawler:robots:" + robotsKey);
  }

  /**
   * Check robots to see if fetch is allowed.
   *
   * @return appropriate resultstatus code.
   */
  protected int checkFetchAllowed(final String documentIdentifier, final String protocol, final String hostIPAddress, final int port, final PageCredentials credential, final IKeystoreManager trustStore, final String hostName, final String[] binNames,
      final long currentTime, final String pathString, final IProcessActivity versionActivities, final int connectionLimit, final String proxyHost, final int proxyPort, final String proxyAuthDomain, final String proxyAuthUsername,
      final String proxyAuthPassword) throws ManifoldCFException, ServiceInterruption {
    // hostNameAndPort is the key for looking up the robots file in the database
    final String hostNameAndPort = makeRobotsKey(protocol, hostName, port);
    final String hostIPAddressAndPort = hostIPAddress + ":" + port;

    Boolean info = robotsManager.checkFetchAllowed(userAgent, hostNameAndPort, currentTime, pathString, versionActivities);
    if (info != null) {
      if (info.booleanValue())
        return RESULTSTATUS_TRUE;
      else
        return RESULTSTATUS_FALSE;
    }

    // We need to fetch robots.txt.
    // Since this is a prerequisite for many documents, prevent queuing and processing of those documents until the robots document is fetched.

    // Assemble the name of the global web connector robots event.
    final String robotsEventName = makeRobotsEventName(versionActivities, hostNameAndPort);
    // Begin the event processing
    if (versionActivities.beginEventSequence(robotsEventName)) {
      // We've successfully obtained a lock on reading robots for this server! Now, guarantee that we'll free it, by instantiating a try/finally
      try {
        final IThrottledConnection connection = ThrottledFetcher.getConnection(currentContext, throttleGroupName, protocol, hostIPAddress, port, credential, trustStore, throttleDescription, binNames, connectionLimit, proxyHost, proxyPort,
            proxyAuthDomain, proxyAuthUsername, proxyAuthPassword, socketTimeoutMilliseconds, connectionTimeoutMilliseconds, versionActivities);
        try {
          connection.beginFetch(FETCH_ROBOTS);
          try {
            connection.executeFetch("/robots.txt", userAgent, from, true, hostName, null, null);
            final long expirationTime = currentTime + 1000 * 60 * 60 * 24;
            final int code = connection.getResponseCode();
            if (code == 200) {
              if (Logging.connectors.isDebugEnabled())
                Logging.connectors.debug("Web: Fetch of robots.txt from " + protocol + "://" + hostIPAddressAndPort + "(host='" + hostName + "') succeeded!");

              final InputStream is = connection.getResponseBodyStream();
              try {
                // Write this to the cache - expiration time 24 hours
                robotsManager.writeRobotsData(hostNameAndPort, expirationTime, is);
              } finally {
                is.close();
              }
            } else {
              if (Logging.connectors.isDebugEnabled())
                Logging.connectors.debug("Web: Fetch of robots.txt from " + protocol + "://" + hostIPAddressAndPort + "(host='" + hostName + "') failed with error " + Integer.toString(code));
              // Write this to the cache - expiration time 24 hours
              robotsManager.writeRobotsData(hostNameAndPort, expirationTime, null);
            }
          } catch (final ServiceInterruption e) {
            // A service interruption on a robots fetch should save the fact that no robots.txt is available, and leave it be for 6 hours that way.
            if (Logging.connectors.isDebugEnabled())
              Logging.connectors.debug("Web: Timeout fetching robots.txt from " + protocol + "://" + hostIPAddressAndPort + "(host='" + hostName + "'); assuming robots missing for now: " + e.getMessage(), e);
            final long expirationTime = currentTime + 1000 * 60 * 60 * 6;
            try {
              robotsManager.writeRobotsData(hostNameAndPort, expirationTime, null);
            } catch (final IOException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache");
            }
          } catch (final java.net.SocketTimeoutException e) {
            if (Logging.connectors.isDebugEnabled())
              Logging.connectors.debug("Web: Fetch of robots.txt from " + protocol + "://" + hostIPAddressAndPort + "(host='" + hostName + "') generated Socket Timeout Exception: " + e.getMessage(), e);
            // This COULD be a transient error, so we are more aggressive about retrying a fetch of robots.txt in that case
            final long expirationTime = currentTime + 1000 * 60 * 60 * 6;
            try {
              robotsManager.writeRobotsData(hostNameAndPort, expirationTime, null);
            } catch (final java.net.SocketTimeoutException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            } catch (final ConnectTimeoutException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            } catch (final InterruptedIOException e2) {
              // Logging.connectors.warn("IO interruption seen",e2);
              throw new ManifoldCFException("Interrupted: " + e2.getMessage(), e2, ManifoldCFException.INTERRUPTED);
            } catch (final IOException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            }
          } catch (final ConnectTimeoutException e) {
            if (Logging.connectors.isDebugEnabled())
              Logging.connectors.debug("Web: Fetch of robots.txt from " + protocol + "://" + hostIPAddressAndPort + "(host='" + hostName + "') generated Socket Connect Timeout Exception: " + e.getMessage(), e);
            // This COULD be a transient error, so we are more aggressive about retrying a fetch of robots.txt in that case
            final long expirationTime = currentTime + 1000 * 60 * 60 * 6;
            try {
              robotsManager.writeRobotsData(hostNameAndPort, expirationTime, null);
            } catch (final java.net.SocketTimeoutException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            } catch (final ConnectTimeoutException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            } catch (final InterruptedIOException e2) {
              // Logging.connectors.warn("IO interruption seen",e2);
              throw new ManifoldCFException("Interrupted: " + e2.getMessage(), e2, ManifoldCFException.INTERRUPTED);
            } catch (final IOException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            }
          } catch (final InterruptedIOException e) {
            // Logging.connectors.warn("IO interruption seen",e);
            throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
          } catch (final IOException e) {
            if (Logging.connectors.isDebugEnabled())
              Logging.connectors.debug("Web: Fetch of robots.txt from " + protocol + "://" + hostIPAddressAndPort + "(host='" + hostName + "') generated IO Exception: " + e.getMessage(), e);
            // This COULD be a transient error, so we are more aggressive about retrying a fetch of robots.txt in that case
            final long expirationTime = currentTime + 1000 * 60 * 60 * 6;
            try {
              robotsManager.writeRobotsData(hostNameAndPort, expirationTime, null);
            } catch (final java.net.SocketTimeoutException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            } catch (final ConnectTimeoutException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            } catch (final InterruptedIOException e2) {
              // Logging.connectors.warn("IO interruption seen",e2);
              throw new ManifoldCFException("Interrupted: " + e2.getMessage(), e2, ManifoldCFException.INTERRUPTED);
            } catch (final IOException e2) {
              Logging.connectors.warn("Web: Couldn't clear robots cache: " + e2.getMessage(), e2);
            }
          } finally {
            connection.doneFetch(versionActivities);
          }
        } finally {
          connection.close();
        }

        // Right now we have no choice but to read it again.
        info = robotsManager.checkFetchAllowed(userAgent, hostNameAndPort, currentTime, pathString, versionActivities);
        if (info.booleanValue())
          return RESULTSTATUS_TRUE;
        else
          return RESULTSTATUS_FALSE;
      } finally {
        versionActivities.completeEventSequence(robotsEventName);
      }
    } else {
      // Some other thread is reading robots.txt right now, so abort processing of the current document.
      return RESULTSTATUS_NOTYETDETERMINED;
    }
  }

  /**
   * Convert an absolute or relative URL to a document identifier. This may involve several steps at some point, but right now it does NOT involve converting the host name to a canonical host name.
   * (Doing so would destroy the ability of virtually hosted sites to do the right thing, since the original host name would be lost.) Thus, we do the conversion to IP address right before we actually
   * fetch the document.
   *
   * @param parentIdentifier
   *          the identifier of the document in which the raw url was found, or null if none.
   * @param rawURL
   *          the starting, un-normalized, un-canonicalized URL.
   * @param filter
   *          the filter object, used to remove unmatching URLs.
   * @return the canonical URL (the document identifier), or null if the url was illegal.
   */
  protected String makeDocumentIdentifier(final String parentIdentifier, final String rawURL, final DocumentURLFilter filter, final IHistoryActivity activities) throws ManifoldCFException {
    try {
      WebURL url;
      if (parentIdentifier != null) {
        final WebURL parentURL = new WebURL(parentIdentifier);
        url = parentURL.resolve(rawURL);
      } else
        url = new WebURL(rawURL);

      final String protocol = url.getScheme();
      final String host = url.getHost();

      // The new URL better darn well have a host and a protocol, and we only know how to deal with
      // http and https.
      if (protocol == null || host == null) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Can't use url '" + rawURL + "' because it has no protocol or host");
        return null;
      }
      if (!understoodProtocols.contains(protocol)) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Can't use url '" + rawURL + "' because it has an unsupported protocol '" + protocol + "'");
        return null;
      }
      if (!filter.isHostLegal(host)) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Can't use url '" + rawURL + "' because its host is not found in the seeds ('" + host + "')");
        return null;
      }

      // Canonicalization procedure.
      // The query part of the URL may contain bad parameters (session id's, for instance), or may be ordered in such a
      // way as to prevent an effectively identical URL from being matched. The anchor part of the URL should also be stripped.
      // This code performs both of these activities in a simple way; rewrites of various pieces may get more involved if we add
      // the ability to perform mappings using criteria specified in the UI. Right now we don't.
      final String id = doCanonicalization(filter, url);
      if (id == null)
        return null;

      // As a last basic legality check, go through looking for illegal characters.
      int i = 0;
      while (i < id.length()) {
        final char x = id.charAt(i++);
        // Only 7-bit ascii is allowed in URLs - and that has limits too (no control characters)
        if (x < ' ' || x > 127) {
          if (Logging.connectors.isDebugEnabled())
            Logging.connectors.debug("WEB: Can't use url '" + rawURL + "' because it has illegal characters in it");
          return null;
        }
      }

      // Check to be sure the canonicalized URL is in fact one of the ones we want to include
      if (!filter.isDocumentLegal(id, activities))
        return null;

      return id;
    } catch (final java.net.URISyntaxException e) {
      if (Logging.connectors.isDebugEnabled())
        Logging.connectors.debug("WEB: Can't use url '" + rawURL + "' because it is badly formed: " + e.getMessage());
      return null;
    } catch (final java.lang.IllegalArgumentException e) {
      return null;
    } catch (final java.lang.NullPointerException e) {
      // This gets tossed by url.toAsciiString() for reasons I don't understand, but which have to do with a malformed URL.
      return null;
    }
  }

  /**
   * Code to canonicalize a URL. If URL cannot be canonicalized (and is illegal) return null.
   */
  protected String doCanonicalization(final DocumentURLFilter filter, WebURL url) throws ManifoldCFException, java.net.URISyntaxException {
    // First, we have to figure out what the canonicalization policy is.
    // To do that, we need to do a regexp match against the COMPLETE raw url.
    // Put it back into the URL without the ref, and with the modified query and path parts.
    String pathString = url.getPath();
    String queryString = url.getRawQuery();

    final WebURL rawURI = new WebURL(url.getScheme(), url.getHost(), url.getPort(), pathString, queryString);
    final String completeRawURL = rawURI.toASCIIString();

    CanonicalizationPolicy p;
    if (completeRawURL != null)
      p = filter.getCanonicalizationPolicies().findMatch(completeRawURL);
    else
      p = null;

    // Note well: The java.net.URI class mistreats the query part of the URI, near as I can tell, in the following ways:
    // (1) It decodes the whole thing without regards to the argument interpretation, so the escaped ampersands etc in the arguments are converted
    // to non-escaped ones (ugh). This is why I changed the code below to parse the RAW query string and decode it myself.
    // (2) On reassembly of the query string, the class does not properly escape ":", "/", or a bunch of other characters the class description *says*
    // it will escape. This means it creates URI's that are illegal according to RFC 2396 - although it is true that RFC 2396 also contains
    // apparent errors.
    //
    // I've therefore opted to deal with this problem by doing much of the query string processing myself - including its final reassembly into the
    // URI at the end of the processing.
    //

    // To make the url be canonical, we need to strip off everything after the #. We also need to order the arguments in a canonical
    // way, and remove session identifiers of the types we know about.
    if (queryString != null) {
      // Rewrite the query string. To do this, we first parse it (by looking for ampersands and equal signs), and then
      // we ditch any keys that we really don't want (session identifiers particularly). Finally, we go through the
      // keys in sorted order and reassemble the query, making sure that any arguments that have the same name
      // appear in the same order.

      // I don't use the 'split' operation because I think it's a lot more oomph (and performance loss) than is needed
      // for this simple parsing task.

      // When reordering a url, the following is done:
      // (1) The individual order of all arguments with the same name is preserved
      // (2) The arguments themselves appear in sorted order, minus any arguments that should be removed because they
      // are interpreted to be session arguments.
      //
      // When a url is NOT reordered, the following is done:
      // (1) Each argument is examined IN TURN.
      // (2) If the argument is a session argument and should be excluded, it is simply skipped.

      // Canonicalization note: Broadvision
      //
      // The format of Broadvision's urls is as follows:
      // http://blah/path/path?arg|arg|arg|BVSession@@@@=xxxx&more stuff
      // The session identifier is the BVSession@@@@. In theory I could strip this away, but I've found that
      // most Broadvision sites require session even for basic navigation! So the lesser of two evils is to leave it
      // in there and suffer the eventual url duplication.

      if (p == null || p.canReorder()) {
        // Reorder arguments
        final HashMap argumentMap = new HashMap();
        int index = 0;
        while (index < queryString.length()) {
          int newIndex = queryString.indexOf("&", index);
          if (newIndex == -1)
            newIndex = queryString.length();
          final String argument = queryString.substring(index, newIndex);
          final int valueIndex = argument.indexOf("=");
          String key;
          if (valueIndex == -1)
            key = argument;
          else
            key = argument.substring(0, valueIndex);

          // If this is a disallowed argument, simply don't include it in the final map.
          boolean includeArgument = true;
          if ((p == null || p.canRemovePhpSession()) && key.equals("PHPSESSID"))
            includeArgument = false;
          // For web connector only, null policy means NO broadvision cookie removal!!
          if (p != null && p.canRemoveBvSession() && key.indexOf("BVSession@@@@") != -1)
            includeArgument = false;

          if (includeArgument) {
            ArrayList list = (ArrayList) argumentMap.get(key);
            if (list == null) {
              list = new ArrayList();
              argumentMap.put(key, list);
            }
            list.add(argument);
          }

          if (newIndex < queryString.length())
            index = newIndex + 1;
          else
            index = newIndex;
        }

        // Reassemble query string in sorted order
        final String[] sortArray = new String[argumentMap.size()];
        int i = 0;
        final Iterator iter = argumentMap.keySet().iterator();
        while (iter.hasNext()) {
          sortArray[i++] = (String) iter.next();
        }
        java.util.Arrays.sort(sortArray);

        final StringBuilder newString = new StringBuilder();
        boolean isFirst = true;
        i = 0;
        while (i < sortArray.length) {
          final String key = sortArray[i++];
          final ArrayList list = (ArrayList) argumentMap.get(key);
          int j = 0;
          while (j < list.size()) {
            if (isFirst == false) {
              newString.append("&");
            } else
              isFirst = false;
            newString.append((String) list.get(j++));
          }
        }
        queryString = newString.toString();
      } else {
        // Do not reorder!
        final StringBuilder newString = new StringBuilder();
        int index = 0;
        boolean isFirst = true;
        while (index < queryString.length()) {
          int newIndex = queryString.indexOf("&", index);
          if (newIndex == -1)
            newIndex = queryString.length();
          final String argument = queryString.substring(index, newIndex);
          final int valueIndex = argument.indexOf("=");
          String key;
          if (valueIndex == -1)
            key = argument;
          else
            key = argument.substring(0, valueIndex);

          // If this is a disallowed argument, simply don't include it in the final query.
          boolean includeArgument = true;
          if ((p == null || p.canRemovePhpSession()) && key.equals("PHPSESSID"))
            includeArgument = false;
          // For web connector only, null policy means NO broadvision cookie removal!!
          if (p != null && p.canRemoveBvSession() && key.indexOf("BVSession@@@@") != -1)
            includeArgument = false;

          if (includeArgument) {
            if (!isFirst)
              newString.append("&");
            else
              isFirst = false;
            newString.append(argument);
          }

          if (newIndex < queryString.length())
            index = newIndex + 1;
          else
            index = newIndex;
        }
        queryString = newString.toString();
      }
    }

    // Now, rewrite path to get rid of jsessionid etc.
    if (pathString != null) {
      int index = pathString.indexOf(";jsessionid=");
      if ((p == null || p.canRemoveJavaSession()) && index != -1) {
        // There's a ";jsessionid="
        // Strip the java session id
        pathString = pathString.substring(0, index);
      }
      if ((p == null || p.canRemoveAspSession()) && pathString.startsWith("/s(")) {
        // It's asp.net
        index = pathString.indexOf(")");
        if (index != -1)
          pathString = pathString.substring(index + 1);
      }

    }

    // Remove duplicate path slashes. This is gated by the "lowercase" selection, since it's also an IIS-specific problem.
    if (p != null && p.canLowercase()) {
      pathString = filterMultipleSlashes(pathString);
    }

    // Put it back into the URL without the ref, and with the modified query and path parts.
    url = new WebURL(url.getScheme(), url.getHost(), url.getPort(), pathString, queryString);
    String rval = url.toASCIIString();
    // Here is where we decide to bash to lowercase, if so indicated
    if (p != null && p.canLowercase()) {
      rval = rval.toLowerCase(Locale.ROOT);
    }
    return rval;
  }

  private static String filterMultipleSlashes(String pathString) {
    // Not terribly efficient unless there are almost never duplicate slashes
    while (true) {
      final int index = pathString.indexOf("//");
      if (index == -1) {
        return pathString;
      }
      pathString = pathString.substring(0, index) + pathString.substring(index + 1);
    }
  }

  /**
   * Code to check if data is interesting, based on response code and content type.
   */
  protected boolean isContentInteresting(final IFingerprintActivity activities, final String documentIdentifier, final int response, final String contentType) throws ServiceInterruption, ManifoldCFException {
    // Additional filtering only done if it's a 200 response
    if (response != 200)
      return true;

    // Look at the content type and decide if it's a kind we want. This is defined
    // as something we think we can either ingest, or extract links from.

    // For now, we're only going to attempt to extract links from html. This will change eventually.
    // But the check here is just what the content type is.

    if (contentType == null)
      return false;

    String strippedContentType = contentType;
    final int pos = strippedContentType.indexOf(";");
    if (pos != -1)
      strippedContentType = strippedContentType.substring(0, pos);
    strippedContentType = strippedContentType.trim();

    // There are presumably mime types we can extract links from that we can't index?
    if (interestingMimeTypeMap.contains(strippedContentType))
      return true;

    final boolean rval = activities.checkMimeTypeIndexable(contentType);
    if (rval == false && Logging.connectors.isDebugEnabled())
      Logging.connectors.debug("Web: For document '" + documentIdentifier + "', not fetching because output connector does not want mimetype '" + contentType + "'");
    return rval;
  }

  /**
   * Convert a document identifier to filename.
   *
   * @param documentIdentifier
   * @throws URISyntaxException
   */
  protected String documentIdentifiertoFileName(final String documentIdentifier) throws URISyntaxException {
    final StringBuffer path = new StringBuffer();
    URI uri = null;

    uri = new URI(documentIdentifier);

    if (uri.getRawPath() != null) {
      if (uri.getRawPath().equals("")) {
        path.append("");
      } else if (uri.getRawPath().equals("/")) {
        path.append("index.html");
      } else if (uri.getRawPath().length() != 0) {
        if (uri.getRawPath().endsWith("/")) {
          path.append("index.html");
        } else {
          final String[] names = uri.getRawPath().split("/");
          path.append(names[names.length - 1]);
        }
      }
    }

    if (path.length() > 0) {
      if (uri.getRawQuery() != null) {
        path.append("?");
        path.append(uri.getRawQuery());
      }
    }

    return path.toString();
  }

  /** Find a redirection URI, if it exists */
  protected String findRedirectionURI(final String currentURI) throws ManifoldCFException {
    final FindRedirectionHandler handler = new FindRedirectionHandler(currentURI);
    handleRedirects(currentURI, handler);
    return handler.getTargetURI();
  }

  /** Find matching HTML form data, if present. Return null if not. */
  protected FormData findHTMLForm(final String currentURI, final LoginParameters lp) throws ManifoldCFException {
    if (lp == null || lp.getFormNamePattern() == null)
      return null;

    // Use the specified loginParameters to (a) find an appropriate form, if present, and (b) override what the form's default
    // form parameters would be. This means that the override parameters are associated with the page on which the *form*
    // is found, not the page to which we are submitting the form. This is unlike (say) Heritrix, which attaches the parameters
    // to the page that's the target of the submission.
    final FindHTMLFormHandler handler = new FindHTMLFormHandler(currentURI, lp.getFormNamePattern());
    handleHTML(currentURI, handler);
    // Apply any overrides
    handler.applyFormOverrides(lp);
    return handler.getFormData();
  }

  /** Find a preferred redirection URI, if it exists */
  protected String findPreferredRedirectionURI(final String currentURI, final LoginParameters lp) throws ManifoldCFException {
    if (lp == null || lp.getPreferredRedirectionPattern() == null)
      return null;

    final FindPreferredRedirectionHandler handler = new FindPreferredRedirectionHandler(currentURI, lp.getPreferredRedirectionPattern());
    handleRedirects(currentURI, handler);
    handler.applyOverrides(lp);
    return handler.getTargetURI();
  }

  /** Find existence of specific content on the page (never finds a URL) */
  protected String findSpecifiedContent(final String currentURI, final LoginParameters lp) throws ManifoldCFException {
    if (lp == null || lp.getContentPattern() == null)
      return null;

    final FindContentHandler handler = new FindContentHandler(currentURI, lp.getContentPattern());
    handleHTML(currentURI, handler);
    handler.applyOverrides(lp);
    return handler.getTargetURI();
  }

  /** Find HTML link URI, if present, making sure specified preference is matched. */
  protected String findHTMLLinkURI(final String currentURI, final LoginParameters lp) throws ManifoldCFException {
    if (lp == null || lp.getPreferredLinkPattern() == null)
      return null;

    final FindHTMLHrefHandler handler = new FindHTMLHrefHandler(currentURI, lp.getPreferredLinkPattern());
    handleHTML(currentURI, handler);
    handler.applyOverrides(lp);
    return handler.getTargetURI();
  }

  /** Code to extract links from an already-fetched document. */
  protected boolean extractLinks(final String documentIdentifier, final IProcessActivity activities, final DocumentURLFilter filter) throws ManifoldCFException, ServiceInterruption {
    final ProcessActivityRedirectionHandler redirectHandler = new ProcessActivityRedirectionHandler(documentIdentifier, activities, filter);
    handleRedirects(documentIdentifier, redirectHandler);
    if (Logging.connectors.isDebugEnabled() && redirectHandler.shouldIndex() == false)
      Logging.connectors.debug("Web: Not indexing document '" + documentIdentifier + "' because of redirection");
    // For html, we don't want any actions, because we don't do form submission.
    final ProcessActivityHTMLHandler htmlHandler = new ProcessActivityHTMLHandler(documentIdentifier, activities, filter, metaRobotsTagsUsage);
    handleHTML(documentIdentifier, htmlHandler);
    if (Logging.connectors.isDebugEnabled() && htmlHandler.shouldIndex() == false)
      Logging.connectors.debug("Web: Not indexing document '" + documentIdentifier + "' because of HTML robots or content tags prohibiting indexing");
    final ProcessActivityXMLHandler xmlHandler = new ProcessActivityXMLHandler(documentIdentifier, activities, filter);
    handleXML(documentIdentifier, xmlHandler);
    if (Logging.connectors.isDebugEnabled() && xmlHandler.shouldIndex() == false)
      Logging.connectors.debug("Web: Not indexing document '" + documentIdentifier + "' because of XML robots or content tags prohibiting indexing");
    // May add more later for other extraction tasks.
    return htmlHandler.shouldIndex() && redirectHandler.shouldIndex() && xmlHandler.shouldIndex();
  }

  /**
   * This class is the handler for links that get added into a IProcessActivity object.
   */
  protected class ProcessActivityLinkHandler implements IDiscoveredLinkHandler {
    protected String documentIdentifier;
    protected String baseDocumentIdentifier;
    protected IProcessActivity activities;
    protected DocumentURLFilter filter;
    protected String contextDescription;
    protected String linkType;

    /** Constructor. */
    public ProcessActivityLinkHandler(final String documentIdentifier, final IProcessActivity activities, final DocumentURLFilter filter, final String contextDescription, final String linkType) {
      this.documentIdentifier = documentIdentifier;
      this.baseDocumentIdentifier = documentIdentifier;
      this.activities = activities;
      this.filter = filter;
      this.contextDescription = contextDescription;
      this.linkType = linkType;
    }

    @Override
    public void noteDiscoveredBase(final String rawURL) throws ManifoldCFException {
      final String newIdentifier = makeDocumentIdentifier(baseDocumentIdentifier, rawURL, filter, activities);
      if (newIdentifier != null)
        baseDocumentIdentifier = newIdentifier;
    }

    /**
     * Inform the world of a discovered link.
     *
     * @param rawURL
     *          is the raw discovered url. This may be relative, malformed, or otherwise unsuitable for use until final form is acheived.
     */
    @Override
    public void noteDiscoveredLink(final String rawURL) throws ManifoldCFException {
      final String newIdentifier = makeDocumentIdentifier(baseDocumentIdentifier, rawURL, filter, activities);
      if (newIdentifier != null) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: In " + contextDescription + " document '" + documentIdentifier + "', found link to '" + newIdentifier + "'");
        activities.addDocumentReference(newIdentifier, documentIdentifier, linkType, null, null, null, calculateDocumentEvents(activities, newIdentifier));
      } else {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: In " + contextDescription + " document '" + documentIdentifier + "', found an unincluded URL '" + rawURL + "'");
      }
    }

  }

  /** Class that describes redirection handling */
  protected class ProcessActivityRedirectionHandler extends ProcessActivityLinkHandler implements IRedirectionHandler {
    /** Constructor. */
    public ProcessActivityRedirectionHandler(final String documentIdentifier, final IProcessActivity activities, final DocumentURLFilter filter) {
      super(documentIdentifier, activities, filter, "redirection", REL_REDIRECT);
    }

    public boolean shouldIndex() {
      return true;
    }

  }

  /** Class that describes HTML handling */
  protected class ProcessActivityHTMLHandler extends ProcessActivityLinkHandler implements IHTMLHandler {
    boolean allowIndex = true;
    boolean allowFollow = true;
    boolean obeyMetaRobotsTags = true;

    /** Constructor. */
    public ProcessActivityHTMLHandler(final String documentIdentifier, final IProcessActivity activities, final DocumentURLFilter filter, final int metaRobotTagsUsage) {
      super(documentIdentifier, activities, filter, "html", REL_LINK);
      this.obeyMetaRobotsTags = metaRobotTagsUsage == META_ROBOTS_ALL;
    }

    /** Decide whether we should index. */
    public boolean shouldIndex() {
      return allowIndex;
    }

    /**
     * Note a character of text. Structured this way to keep overhead low for handlers that don't use text.
     */
    @Override
    public void noteTextCharacter(final char textCharacter) throws ManifoldCFException {
    }

    /** Note a meta tag */
    @Override
    public void noteMetaTag(final Map metaAttributes) throws ManifoldCFException {
      final String name = (String) metaAttributes.get("name");
      if (obeyMetaRobotsTags && name != null && name.toLowerCase(Locale.ROOT).equals("robots")) {
        String contentValue = (String) metaAttributes.get("content");
        if (contentValue != null) {
          contentValue = contentValue.toLowerCase(Locale.ROOT);
          // Parse content value
          try {
            final String[] contentValues = contentValue.split(",");
            int i = 0;
            while (i < contentValues.length) {
              final String cv = contentValues[i++].trim();
              if (cv.equals("index"))
                allowIndex = true;
              else if (cv.equals("noindex"))
                allowIndex = false;
              else if (cv.equals("none")) {
                allowFollow = false;
                allowIndex = false;
              } else if (cv.equals("follow"))
                allowFollow = true;
              else if (cv.equals("nofollow"))
                allowFollow = false;
            }
          } catch (final PatternSyntaxException e) {
            throw new ManifoldCFException(e.getMessage(), e);
          }
        }
      }
    }

    /** Note the start of a form */
    @Override
    public void noteFormStart(final Map formAttributes) throws ManifoldCFException {
    }

    /** Note an input tag */
    @Override
    public void noteFormInput(final Map inputAttributes) throws ManifoldCFException {
    }

    /** Note the end of a form */
    @Override
    public void noteFormEnd() throws ManifoldCFException {
    }

    /** Note discovered base */
    @Override
    public void noteBASEHREF(final String rawURL) throws ManifoldCFException {
      if (allowFollow)
        noteDiscoveredBase(rawURL);
    }

    /** Note discovered href */
    @Override
    public void noteAHREF(final String rawURL) throws ManifoldCFException {
      if (allowFollow)
        noteDiscoveredLink(rawURL);
    }

    /** Note discovered href */
    @Override
    public void noteLINKHREF(final String rawURL) throws ManifoldCFException {
      if (allowFollow)
        noteDiscoveredLink(rawURL);
    }

    /** Note discovered IMG SRC */
    @Override
    public void noteIMGSRC(final String rawURL) throws ManifoldCFException {
      if (allowFollow)
        noteDiscoveredLink(rawURL);
    }

    /** Note discovered FRAME SRC */
    @Override
    public void noteFRAMESRC(final String rawURL) throws ManifoldCFException {
      if (allowFollow)
        noteDiscoveredLink(rawURL);
    }

    @Override
    public void finishUp() throws ManifoldCFException {
    }

  }

  /** Class that describes XML handling */
  protected class ProcessActivityXMLHandler extends ProcessActivityLinkHandler implements IXMLHandler {
    /** Constructor. */
    public ProcessActivityXMLHandler(final String documentIdentifier, final IProcessActivity activities, final DocumentURLFilter filter) {
      super(documentIdentifier, activities, filter, "xml", REL_LINK);
    }

    public boolean shouldIndex() {
      return true;
    }

    /**
     * Inform the world of a discovered ttl value.
     *
     * @param rawTtlValue
     *          is the raw discovered ttl value. Null indicates we should set the default.
     */
    @Override
    public void noteDiscoveredTtlValue(final String rawTtlValue) throws ManifoldCFException {
      final long currentTime = System.currentTimeMillis();
      Long rescanTime = null;
      if (rawTtlValue != null) {
        try {
          final int minutes = Integer.parseInt(rawTtlValue);
          final long nextTime = currentTime + minutes * 60000L;
          rescanTime = new Long(nextTime);
          // Set the upper bound time; we want to scan the feeds aggressively.
          if (Logging.connectors.isDebugEnabled())
            Logging.connectors.debug("WEB: In " + contextDescription + " document '" + documentIdentifier + "', found a ttl value of " + rawTtlValue + "; setting refetch time accordingly");
        } catch (final NumberFormatException e) {
          Logging.connectors.warn("WEB: In " + contextDescription + " document '" + documentIdentifier + "' found illegal ttl value '" + rawTtlValue + "': " + e.getMessage(), e);
        }
      }

      if (Logging.connectors.isDebugEnabled())
        Logging.connectors.debug("WEB: In " + contextDescription + " document '" + documentIdentifier + "' setting rescan time to " + ((rescanTime == null) ? "null" : rescanTime.toString()));

      activities.setDocumentScheduleBounds(documentIdentifier, rescanTime, rescanTime, null, null);
    }

  }

  /** Handle extracting the redirect link from a redirect response. */
  protected void handleRedirects(final String documentURI, final IRedirectionHandler handler) throws ManifoldCFException {
    final int responseCode = cache.getResponseCode(documentURI);
    if (responseCode == 302 || responseCode == 301) {
      // Figure out the redirect
      String referralURI = cache.getReferralURI(documentURI);
      if (referralURI == null) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: The document '" + documentURI + "' is a redirection, but there was no URI header!");
        return;
      }
      final int index = referralURI.indexOf(" ");
      if (index != -1)
        referralURI = referralURI.substring(0, index);
      handler.noteDiscoveredLink(referralURI);
    }
  }

  /** Handle document references from XML. Right now we only understand RSS. */
  protected void handleXML(final String documentURI, final IXMLHandler handler) throws ManifoldCFException, ServiceInterruption {
    try {
      final int responseCode = cache.getResponseCode(documentURI);
      if (responseCode != 200)
        return;

      // We ONLY look for XML if the content type *says* it is XML.
      final String contentType = extractContentType(cache.getContentType(documentURI));
      final String mimeType = extractMimeType(contentType);
      final boolean isXML = mimeType.equals("text/xml") || mimeType.equals("application/rss+xml") || mimeType.equals("application/xml") || mimeType.equals("application/atom+xml") || mimeType.equals("application/xhtml+xml")
          || mimeType.equals("text/XML") || mimeType.equals("application/rdf+xml") || mimeType.equals("text/application") || mimeType.equals("XML");

      if (!isXML)
        return;

      // OK, it's XML. Now what? Well, we get the encoding, and we verify that it is text, then we try to get links
      // from it presuming it is an RSS feed.

      final String encoding = extractEncoding(contentType);

      final InputStream is = cache.getData(documentURI);
      if (is == null) {
        Logging.connectors.error("WEB: Document '" + documentURI + "' should be in cache but isn't");
        return;
      }
      try {
        // Parse the document. This will cause various things to occur, within the instantiated XMLParsingContext class.
        final Parser p = new Parser();
        final XMLFuzzyHierarchicalParseState x = new XMLFuzzyHierarchicalParseState();
        final OuterContextClass c = new OuterContextClass(x, documentURI, handler);
        x.setContext(c);
        try {
          p.parseWithCharsetDetection(encoding, is, x);
          c.checkIfValidFeed();
        } finally {
          x.cleanup();
        }
      } finally {
        is.close();
      }
    } catch (final java.net.SocketTimeoutException e) {
      throw new ManifoldCFException("Socket timeout exception: " + e.getMessage(), e);
    } catch (final ConnectTimeoutException e) {
      throw new ManifoldCFException("Socket connect timeout exception: " + e.getMessage(), e);
    } catch (final InterruptedIOException e) {
      // Logging.connectors.warn("IO interruption seen",e);

      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
    } catch (final IOException e) {
      throw new ManifoldCFException("IO error: " + e.getMessage(), e);
    }
  }

  /** This class handles the outermost XML context for the feed document. */
  protected class OuterContextClass extends XMLParsingContext {
    /** Keep track of the number of valid feed signals we saw */
    protected int outerTagCount = 0;
    /** The document uri */
    protected String documentURI;
    /** The link handler */
    protected IXMLHandler handler;

    public OuterContextClass(final XMLFuzzyHierarchicalParseState theStream, final String documentURI, final IXMLHandler handler) {
      super(theStream);
      this.documentURI = documentURI;
      this.handler = handler;
    }

    /** Check if feed was valid */
    public void checkIfValidFeed() {
      if (outerTagCount == 0) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: XML document '" + documentURI + "' does not have rss, feed, or rdf:RDF tag - not valid feed");
      }
    }

    /** Handle the tag beginning to set the correct second-level parsing context */
    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      if (localName.equals("rss")) {
        // RSS feed detected
        outerTagCount++;
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Parsed bottom-level XML for RSS document '" + documentURI + "'");
        return new RSSContextClass(theStream, namespace, localName, qName, atts, documentURI, handler);
      } else if (localName.toLowerCase(Locale.ROOT).equals("rdf")) {
        // RDF/Atom feed detected
        outerTagCount++;
        return new RDFContextClass(theStream, namespace, localName, qName, atts, documentURI, handler);
      } else if (localName.equals("feed")) {
        // Basic feed detected
        outerTagCount++;
        return new FeedContextClass(theStream, namespace, localName, qName, atts, documentURI, handler);
      } else if (localName.equals("urlset") || localName.equals("sitemapindex")) {
        // Sitemap detected
        outerTagCount++;
        return new UrlsetContextClass(theStream, namespace, localName, qName, atts, documentURI, handler);
      }

      // The default action is to establish a new default context.
      return super.beginTag(namespace, localName, qName, atts);
    }

    /** Handle the tag ending */
    @Override
    protected void endTag() throws ManifoldCFException {
      final XMLParsingContext context = theStream.getContext();
      final String tagName = context.getLocalname();
      if (tagName.toLowerCase(Locale.ROOT).equals("rdf")) {
        ((RDFContextClass) context).process();
      } else if (tagName.equals("feed")) {
        ((FeedContextClass) context).process();
      } else if (tagName.equals("urlset") || tagName.equals("sitemapindex")) {
        ((UrlsetContextClass) context).process();
      } else
        super.endTag();
    }

  }

  protected class RSSContextClass extends XMLParsingContext {
    /** The document identifier */
    protected String documentURI;
    /** Link notification interface */
    protected IXMLHandler handler;

    public RSSContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts, final String documentURI, final IXMLHandler handler) {
      super(theStream, namespace, localName, qName, atts);
      this.documentURI = documentURI;
      this.handler = handler;
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // Handle each channel
      if (localName.equals("channel")) {
        // Channel detected
        return new RSSChannelContextClass(theStream, namespace, localName, qName, atts, documentURI, handler);
      }

      // Skip everything else.
      return super.beginTag(namespace, localName, qName, atts);
    }

    @Override
    protected void endTag() throws ManifoldCFException {
      // If it's our channel tag, process global channel information
      final XMLParsingContext context = theStream.getContext();
      final String tagName = context.getLocalname();
      if (tagName.equals("channel")) {
        ((RSSChannelContextClass) context).process();
      } else
        super.endTag();
    }
  }

  protected class RSSChannelContextClass extends XMLParsingContext {
    /** The document identifier */
    protected String documentURI;
    /** Link handler */
    protected IXMLHandler handler;

    /** TTL value is set on a per-channel basis */
    protected String ttlValue = null;

    public RSSChannelContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts, final String documentURI, final IXMLHandler handler) {
      super(theStream, namespace, localName, qName, atts);
      this.documentURI = documentURI;
      this.handler = handler;
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // The tags we care about are "ttl" and "item", nothing else.
      if (localName.equals("ttl")) {
        // TTL value seen. Prepare to record it, as a string.
        return new XMLStringParsingContext(theStream, namespace, localName, qName, atts);
      } else if (localName.equals("item")) {
        // Item seen. We don't need any of the attributes etc., but we need to start a new context.
        return new RSSItemContextClass(theStream, namespace, localName, qName, atts);
      }
      // Skip everything else.
      return super.beginTag(namespace, localName, qName, atts);
    }

    @Override
    protected void endTag() throws ManifoldCFException {
      final XMLParsingContext theContext = theStream.getContext();
      final String theTag = theContext.getLocalname();
      if (theTag.equals("ttl"))
        // If the current context must be the TTL one, record its data value.
        ttlValue = ((XMLStringParsingContext) theContext).getValue();
      else if (theTag.equals("item")) {
        // It's an item.
        final RSSItemContextClass itemContext = (RSSItemContextClass) theContext;
        // Presumably, since we are done parsing, we've recorded all the information we need in the context, object including:
        // (1) File name (if any), containing dechromed content
        // (2) Link name(s)
        // (3) Pubdate
        // (4) Title
        // The job now is to pull this info out and call the activities interface appropriately.

        // NOTE: After this endTag() method is called, tagCleanup() will be called for the item context. This should clean up
        // all dangling files etc. that need to be removed.
        // If an exception or error is thrown during the parse, this endTag() method will NOT be called, but the tagCleanup()
        // method will be called regardless.
        itemContext.process(handler);
      } else
        super.endTag();
    }

    /** Process this data */
    protected void process() throws ManifoldCFException {
      // Deal with the ttlvalue, if it was found
      // Use the ttl value as a signal for when we ought to look at this feed again. If not present, use the default.
      handler.noteDiscoveredTtlValue(ttlValue);
    }
  }

  protected class RSSItemContextClass extends XMLParsingContext {
    protected String guidField = null;
    protected String linkField = null;

    public RSSItemContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts) {
      super(theStream, namespace, localName, qName, atts);
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // The tags we care about are "ttl" and "item", nothing else.
      if (localName.equals("link")) {
        // "link" tag
        return new XMLStringParsingContext(theStream, namespace, localName, qName, atts);
      } else if (localName.equals("guid")) {
        // "guid" tag
        return new XMLStringParsingContext(theStream, namespace, localName, qName, atts);
      } else {
        // Skip everything else.
        return super.beginTag(namespace, localName, qName, atts);
      }
    }

    /** Convert the individual sub-fields of the item context into their final forms */
    @Override
    protected void endTag() throws ManifoldCFException {
      final XMLParsingContext theContext = theStream.getContext();
      final String theTag = theContext.getLocalname();
      if (theTag.equals("link")) {
        linkField = ((XMLStringParsingContext) theContext).getValue();
      } else if (theTag.equals("guid")) {
        guidField = ((XMLStringParsingContext) theContext).getValue();
      } else {
        super.endTag();
      }
    }

    /** Process the data accumulated for this item */
    public void process(final IXMLHandler handler) throws ManifoldCFException {
      if (linkField == null || linkField.length() == 0)
        linkField = guidField;

      if (linkField != null && linkField.length() > 0) {
        final String[] links = linkField.split(", ");
        int l = 0;
        while (l < links.length) {
          final String rawURL = links[l++].trim();
          // Process the link
          handler.noteDiscoveredLink(rawURL);
        }
      }
    }
  }

  protected class RDFContextClass extends XMLParsingContext {
    /** The document identifier */
    protected String documentURI;
    /** XML handler */
    protected IXMLHandler handler;

    /** ttl value */
    protected String ttlValue = null;

    public RDFContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts, final String documentURI, final IXMLHandler handler) {
      super(theStream, namespace, localName, qName, atts);
      this.documentURI = documentURI;
      this.handler = handler;
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // The tags we care about are "ttl" and "item", nothing else.
      if (localName.equals("ttl")) {
        // TTL value seen. Prepare to record it, as a string.
        return new XMLStringParsingContext(theStream, namespace, localName, qName, atts);
      } else if (localName.equals("item")) {
        // Item seen. We don't need any of the attributes etc., but we need to start a new context.
        return new RDFItemContextClass(theStream, namespace, localName, qName, atts);
      }
      // Skip everything else.
      return super.beginTag(namespace, localName, qName, atts);
    }

    @Override
    protected void endTag() throws ManifoldCFException {
      final XMLParsingContext theContext = theStream.getContext();
      final String theTag = theContext.getLocalname();
      if (theTag.equals("ttl"))
        // If the current context must be the TTL one, record its data value.
        ttlValue = ((XMLStringParsingContext) theContext).getValue();
      else if (theTag.equals("item")) {
        // It's an item.
        final RDFItemContextClass itemContext = (RDFItemContextClass) theContext;
        // Presumably, since we are done parsing, we've recorded all the information we need in the context, object including:
        // (1) Link name(s)
        // The job now is to pull this info out and call the activities interface appropriately.

        // NOTE: After this endTag() method is called, tagCleanup() will be called for the item context. This should clean up
        // all dangling files etc. that need to be removed.
        // If an exception or error is thrown during the parse, this endTag() method will NOT be called, but the tagCleanup()
        // method will be called regardless.
        itemContext.process(handler);
      } else
        super.endTag();
    }

    /** Process this data */
    protected void process() throws ManifoldCFException {
      // Deal with the ttlvalue, if it was found
      handler.noteDiscoveredTtlValue(ttlValue);
    }
  }

  protected class RDFItemContextClass extends XMLParsingContext {
    protected String linkField = null;

    public RDFItemContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts) {
      super(theStream, namespace, localName, qName, atts);
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // The tags we care about are "ttl" and "item", nothing else.
      if (localName.equals("link")) {
        // "link" tag
        return new XMLStringParsingContext(theStream, namespace, localName, qName, atts);
      } else {
        // Skip everything else.
        return super.beginTag(namespace, localName, qName, atts);
      }
    }

    /** Convert the individual sub-fields of the item context into their final forms */
    @Override
    protected void endTag() throws ManifoldCFException {
      final XMLParsingContext theContext = theStream.getContext();
      final String theTag = theContext.getLocalname();
      if (theTag.equals("link")) {
        linkField = ((XMLStringParsingContext) theContext).getValue();
      } else {
        super.endTag();
      }
    }

    /** Process the data accumulated for this item */
    public void process(final IXMLHandler handler) throws ManifoldCFException {
      if (linkField != null && linkField.length() > 0) {
        final String[] links = linkField.split(", ");
        int l = 0;
        while (l < links.length) {
          final String rawURL = links[l++].trim();
          // Process the link
          handler.noteDiscoveredLink(rawURL);
        }
      }
    }
  }

  protected class FeedContextClass extends XMLParsingContext {
    /** The document identifier */
    protected String documentURI;
    /** XML handler */
    protected IXMLHandler handler;

    /** ttl value */
    protected String ttlValue = null;

    public FeedContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts, final String documentURI, final IXMLHandler handler) {
      super(theStream, namespace, localName, qName, atts);
      this.documentURI = documentURI;
      this.handler = handler;
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // The tags we care about are "ttl" and "item", nothing else.
      if (localName.equals("ttl")) {
        // TTL value seen. Prepare to record it, as a string.
        return new XMLStringParsingContext(theStream, namespace, localName, qName, atts);
      } else if (localName.equals("entry")) {
        // Item seen. We don't need any of the attributes etc., but we need to start a new context.
        return new FeedItemContextClass(theStream, namespace, localName, qName, atts);
      }
      // Skip everything else.
      return super.beginTag(namespace, localName, qName, atts);
    }

    @Override
    protected void endTag() throws ManifoldCFException {
      final XMLParsingContext theContext = theStream.getContext();
      final String theTag = theContext.getLocalname();
      if (theTag.equals("ttl"))
        // If the current context must be the TTL one, record its data value.
        ttlValue = ((XMLStringParsingContext) theContext).getValue();
      else if (theTag.equals("entry")) {
        // It's an item.
        final FeedItemContextClass itemContext = (FeedItemContextClass) theContext;
        // Presumably, since we are done parsing, we've recorded all the information we need in the context, object including:
        // (1) Link name(s)
        // The job now is to pull this info out and call the activities interface appropriately.

        // NOTE: After this endTag() method is called, tagCleanup() will be called for the item context. This should clean up
        // all dangling files etc. that need to be removed.
        // If an exception or error is thrown during the parse, this endTag() method will NOT be called, but the tagCleanup()
        // method will be called regardless.
        itemContext.process(handler);
      } else
        super.endTag();
    }

    /** Process this data */
    protected void process() throws ManifoldCFException {
      // Deal with the ttlvalue, if it was found
      // Use the ttl value as a signal for when we ought to look at this feed again. If not present, use the default.
      handler.noteDiscoveredTtlValue(ttlValue);
    }
  }

  protected class FeedItemContextClass extends XMLParsingContext {
    protected List<String> linkField = new ArrayList<>();

    public FeedItemContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts) {
      super(theStream, namespace, localName, qName, atts);
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // The tags we care about are "ttl" and "item", nothing else.
      if (localName.equals("link")) {
        // "link" tag
        final String ref = atts.get("href");
        if (ref != null && ref.length() > 0)
          linkField.add(ref);
        return super.beginTag(namespace, localName, qName, atts);
      } else {
        // Skip everything else.
        return super.beginTag(namespace, localName, qName, atts);
      }
    }

    /** Process the data accumulated for this item */
    public void process(final IXMLHandler handler) throws ManifoldCFException {
      if (linkField.size() > 0) {
        for (final String linkValue : linkField) {
          final String[] links = linkValue.split(", ");
          int l = 0;
          while (l < links.length) {
            final String rawURL = links[l++].trim();
            // Process the link
            handler.noteDiscoveredLink(rawURL);
          }
        }
      }
    }
  }

  protected class UrlsetContextClass extends XMLParsingContext {
    /** The document identifier */
    protected String documentURI;
    /** XML handler */
    protected IXMLHandler handler;

    /** ttl value */
    protected String ttlValue = null;

    public UrlsetContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts, final String documentURI, final IXMLHandler handler) {
      super(theStream, namespace, localName, qName, atts);
      this.documentURI = documentURI;
      this.handler = handler;
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // The tags we care about are "url", nothing else.
      if (localName.equals("url") || localName.equals("sitemap")) {
        // Item seen. We don't need any of the attributes etc., but we need to start a new context.
        return new UrlsetItemContextClass(theStream, namespace, localName, qName, atts);
      }
      // Skip everything else.
      return super.beginTag(namespace, localName, qName, atts);
    }

    @Override
    protected void endTag() throws ManifoldCFException {
      final XMLParsingContext theContext = theStream.getContext();
      final String theTag = theContext.getLocalname();
      if (theTag.equals("url") || theTag.equals("sitemap")) {
        // It's an item.
        final UrlsetItemContextClass itemContext = (UrlsetItemContextClass) theContext;
        // Presumably, since we are done parsing, we've recorded all the information we need in the context, object including:
        // (1) File name (if any), containing dechromed content
        // (2) Link name(s)
        // (3) Pubdate
        // (4) Title
        // The job now is to pull this info out and call the activities interface appropriately.

        // NOTE: After this endTag() method is called, tagCleanup() will be called for the item context. This should clean up
        // all dangling files etc. that need to be removed.
        // If an exception or error is thrown during the parse, this endTag() method will NOT be called, but the tagCleanup()
        // method will be called regardless.
        itemContext.process(handler);
      } else
        super.endTag();
    }

    /** Process this data */
    protected void process() throws ManifoldCFException {
      // Deal with the ttlvalue, if it was found
      // Use the ttl value as a signal for when we ought to look at this feed again. If not present, use the default.
      handler.noteDiscoveredTtlValue(ttlValue);
    }
  }

  protected class UrlsetItemContextClass extends XMLParsingContext {
    protected String linkField = null;

    public UrlsetItemContextClass(final XMLFuzzyHierarchicalParseState theStream, final String namespace, final String localName, final String qName, final Map<String, String> atts) {
      super(theStream, namespace, localName, qName, atts);
    }

    @Override
    protected XMLParsingContext beginTag(final String namespace, final String localName, final String qName, final Map<String, String> atts) throws ManifoldCFException {
      // The tags we care about are "loc", nothing else.
      if (localName.equals("loc")) {
        // "loc" tag
        return new XMLStringParsingContext(theStream, namespace, localName, qName, atts);
      } else {
        // Skip everything else.
        return super.beginTag(namespace, localName, qName, atts);
      }
    }

    /** Convert the individual sub-fields of the item context into their final forms */
    @Override
    protected void endTag() throws ManifoldCFException {
      final XMLParsingContext theContext = theStream.getContext();
      final String theTag = theContext.getLocalname();
      if (theTag.equals("loc")) {
        linkField = ((XMLStringParsingContext) theContext).getValue();
      } else {
        super.endTag();
      }
    }

    @Override
    protected void tagCleanup() throws ManifoldCFException {
    }

    /** Process the data accumulated for this item */
    public void process(final IXMLHandler handler) throws ManifoldCFException {
      if (linkField != null && linkField.length() > 0) {
        final String[] links = linkField.split(", ");
        int l = 0;
        while (l < links.length) {
          final String rawURL = links[l++].trim();
          // Process the link
          handler.noteDiscoveredLink(rawURL);
        }
      }
    }
  }

  /** Handle document references from HTML */
  protected void handleHTML(final String documentURI, final IHTMLHandler handler) throws ManifoldCFException {
    final int responseCode = cache.getResponseCode(documentURI);
    if (responseCode != 200)
      return;
    try {
      // We'll check first to see if this is textual - and then we'll try to find html links in it.
      if (!isDocumentText(documentURI)) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Document '" + documentURI + "' is not text; can't extract links");
        return;
      }
      // Grab the content-type so we know how to decode.
      final String contentType = extractContentType(cache.getContentType(documentURI));
      String encoding = extractEncoding(contentType);
      if (encoding == null)
        encoding = StandardCharsets.UTF_8.name();

      // Search for A HREF tags in the document stream. This is brain-dead link location
      final InputStream is = cache.getData(documentURI);
      if (is == null) {
        Logging.connectors.error("WEB: Document '" + documentURI + "' should be in cache but isn't");
        return;
      }

      try {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Document '" + documentURI + "' is text, with encoding '" + encoding + "'; link extraction starting");

        // Instantiate the parser, and call the right method
        final Parser p = new Parser();
        p.parseWithoutCharsetDetection(encoding, is, new FormParseState(handler));
      } catch (final UnsupportedEncodingException e) {
        // The encoding specified was crap, so don't handle this document.
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Document '" + documentURI + "' had an unrecognized encoding '" + encoding + "'");
        return;
      } finally {
        is.close();
      }
    } catch (final SocketTimeoutException e) {
      throw new ManifoldCFException("Socket timeout exception: " + e.getMessage(), e);
    } catch (final ConnectTimeoutException e) {
      throw new ManifoldCFException("Socket connect timeout exception: " + e.getMessage(), e);
    } catch (final InterruptedIOException e) {
      // Logging.connectors.warn("IO interruption seen",e);

      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
    } catch (final IOException e) {
      throw new ManifoldCFException("IO error: " + e.getMessage(), e);
    }
  }

  /** Is the document text, as far as we can tell? */
  protected boolean isDocumentText(final String documentURI) throws ManifoldCFException {
    try {
      // Look at the first 4K
      final byte[] byteBuffer = new byte[4096];
      int amt;

      // Open file for reading.
      final InputStream is = cache.getData(documentURI);
      if (is == null)
        return false;
      try {
        amt = 0;
        while (amt < byteBuffer.length) {
          final int incr = is.read(byteBuffer, amt, byteBuffer.length - amt);
          if (incr == -1)
            break;
          amt += incr;
        }
      } finally {
        is.close();
      }

      if (amt == 0)
        return false;

      return isText(byteBuffer, amt);
    } catch (final SocketTimeoutException e) {
      throw new ManifoldCFException("Socket timeout exception accessing cached document: " + e.getMessage(), e);
    } catch (final ConnectTimeoutException e) {
      throw new ManifoldCFException("Socket timeout exception accessing cached document: " + e.getMessage(), e);
    } catch (final InterruptedIOException e) {
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
    } catch (final IOException e) {
      throw new ManifoldCFException("IO exception accessing cached document: " + e.getMessage(), e);
    }
  }

  /**
   * Test to see if a document is text or not. The first n bytes are passed in, and this code returns "true" if it thinks they represent text. The code has been lifted algorithmically from
   * products/Sharecrawler/Fingerprinter.pas, which was based on "perldoc -f -T".
   */
  protected static boolean isText(final byte[] beginChunk, final int chunkLength) {
    if (chunkLength == 0)
      return true;
    int i = 0;
    int count = 0;
    while (i < chunkLength) {
      final byte x = beginChunk[i++];
      if (x == 0)
        return false;
      if (isStrange(x))
        count++;
    }
    return ((double) count) / ((double) chunkLength) < 0.30;
  }

  /** Check if character is not typical ASCII or utf-8. */
  protected static boolean isStrange(final byte x) {
    return (x >= 0 && x < 32) && (!isWhiteSpace(x));
  }

  /** Check if a byte is a whitespace character. */
  protected static boolean isWhiteSpace(final byte x) {
    return (x == 0x09 || x == 0x0a || x == 0x0d || x == 0x20);
  }

  /**
   * Read a string as a sequence of individual expressions, urls, etc.
   */
  protected static List<String> stringToArray(final String input) {
    final List<String> list = new ArrayList<>();
    try {
      final java.io.Reader str = new java.io.StringReader(input);
      try {
        final java.io.BufferedReader is = new java.io.BufferedReader(str);
        try {
          while (true) {
            String nextString = is.readLine();
            if (nextString == null)
              break;
            nextString = nextString.trim();
            if (nextString.length() == 0)
              continue;
            if (nextString.startsWith("#"))
              continue;
            list.add(nextString);
          }
        } finally {
          is.close();
        }
      } finally {
        str.close();
      }
    } catch (final java.io.IOException e) {
      // Eat the exception and exit.
    }
    return list;
  }

  /**
   * Compile all regexp entries in the passed in list, and add them to the output list.
   */
  protected static void compileList(final List<Pattern> output, final List<String> input) throws ManifoldCFException {
    int i = 0;
    while (i < input.size()) {
      final String inputString = input.get(i++);
      try {
        output.add(Pattern.compile(inputString));
      } catch (final PatternSyntaxException e) {
        throw new ManifoldCFException("Mapping regular expression '" + inputString + "' is illegal: " + e.getMessage(), e);
      }
    }
  }

  /** Get the page credentials for a given document identifier (URL) */
  protected PageCredentials getPageCredential(final String documentIdentifier) {
    return credentialsDescription.getPageCredential(documentIdentifier);
  }

  /** Get the sequence credentials for a given document identifier (URL) */
  protected SequenceCredentials getSequenceCredential(final String documentIdentifier) {
    return credentialsDescription.getSequenceCredential(documentIdentifier);
  }

  /** Get the trust store for a given document identifier (URL) */
  protected IKeystoreManager getTrustStore(final String documentIdentifier) throws ManifoldCFException {
    return trustsDescription.getTrustStore(documentIdentifier);
  }

  /**
   * Grab forced acl out of document specification.
   *
   * @param spec
   *          is the document specification.
   * @return the acls.
   */
  protected static String[] getAcls(final Specification spec) {
    final Set<String> map = new HashSet<>();
    int i = 0;
    while (i < spec.getChildCount()) {
      final SpecificationNode sn = spec.getChild(i++);
      if (sn.getType().equals(WebcrawlerConfig.NODE_ACCESS)) {
        final String token = sn.getAttributeValue(WebcrawlerConfig.ATTR_TOKEN);
        map.add(token);
      }
    }

    final String[] rval = new String[map.size()];
    final Iterator<String> iter = map.iterator();
    i = 0;
    while (iter.hasNext()) {
      rval[i++] = iter.next();
    }
    return rval;
  }

  /** Read a document specification to get a set of excluded headers */
  protected static Set<String> findExcludedHeaders(final Specification spec) throws ManifoldCFException {
    final Set<String> rval = new HashSet<>();
    int i = 0;
    while (i < spec.getChildCount()) {
      final SpecificationNode n = spec.getChild(i++);
      if (n.getType().equals(WebcrawlerConfig.NODE_EXCLUDEHEADER)) {
        final String value = n.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
        rval.add(value);
      }
    }
    return rval;
  }

  /** Calculate events that should be associated with a document. */
  protected String[] calculateDocumentEvents(final INamingActivity activities, final String documentIdentifier) {
    // All we have for events for right now are the robots and dns events. Each document has one of each.
    try {
      // Get all the appropriate parts from the document identifier
      final URL url = new URL(documentIdentifier);
      final String hostName = url.getHost();
      final String protocol = url.getProtocol();
      int port = url.getPort();
      if (port == -1)
        port = url.getDefaultPort();
      // Form the robots key
      final String robotsKey = makeRobotsKey(protocol, hostName, port);
      // Build the corresponding event name
      final String robotsEventName = makeRobotsEventName(activities, robotsKey);
      final String dnsEventName = makeDNSEventName(activities, hostName);
      // See if we're in a session-protected area
      final SequenceCredentials sequenceCredential = getSequenceCredential(documentIdentifier);
      if (sequenceCredential != null) {
        final String sessionKey = sequenceCredential.getSequenceKey();
        final String sessionEventName = makeSessionLoginEventName(activities, sessionKey);
        return new String[] { robotsEventName, hostName, sessionEventName };
      }
      return new String[] { robotsEventName, hostName };
    } catch (final MalformedURLException e) {
      Logging.connectors.warn("WEB: Could not form event names for identifier '" + documentIdentifier + "' because it was malformed: " + e.getMessage(), e);
      return null;
    }
  }

  /** Name/value class */
  protected static class NameValue {
    protected final String name;
    protected final String value;

    public NameValue(final String name, final String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Evaluator token.
   */
  protected static class EvaluatorToken {
    public final static int TYPE_GROUP = 0;
    public final static int TYPE_TEXT = 1;
    public final static int TYPE_COMMA = 2;

    public final static int GROUPSTYLE_NONE = 0;
    public final static int GROUPSTYLE_LOWER = 1;
    public final static int GROUPSTYLE_UPPER = 2;
    public final static int GROUPSTYLE_MIXED = 3;

    protected int type;
    protected int groupNumber = -1;
    protected int groupStyle = GROUPSTYLE_NONE;
    protected String textValue = null;

    public EvaluatorToken() {
      type = TYPE_COMMA;
    }

    public EvaluatorToken(final int groupNumber, final int groupStyle) {
      type = TYPE_GROUP;
      this.groupNumber = groupNumber;
      this.groupStyle = groupStyle;
    }

    public EvaluatorToken(final String text) {
      type = TYPE_TEXT;
      this.textValue = text;
    }

    public int getType() {
      return type;
    }

    public int getGroupNumber() {
      return groupNumber;
    }

    public int getGroupStyle() {
      return groupStyle;
    }

    public String getTextValue() {
      return textValue;
    }

  }

  /**
   * Token stream.
   */
  protected static class EvaluatorTokenStream {
    protected String text;
    protected int pos;
    protected EvaluatorToken token = null;

    /**
     * Constructor.
     */
    public EvaluatorTokenStream(final String text) {
      this.text = text;
      this.pos = 0;
    }

    /**
     * Get current token.
     */
    public EvaluatorToken peek() throws ManifoldCFException {
      if (token == null) {
        token = nextToken();
      }
      return token;
    }

    /**
     * Go on to next token.
     */
    public void advance() {
      token = null;
    }

    protected EvaluatorToken nextToken() throws ManifoldCFException {
      char x;
      // Fetch the next token
      while (true) {
        if (pos == text.length())
          return null;
        x = text.charAt(pos);
        if (x > ' ')
          break;
        pos++;
      }

      StringBuilder sb;

      if (x == '"') {
        // Parse text
        pos++;
        sb = new StringBuilder();
        while (true) {
          if (pos == text.length())
            break;
          x = text.charAt(pos);
          pos++;
          if (x == '"') {
            break;
          }
          if (x == '\\') {
            if (pos == text.length())
              break;
            x = text.charAt(pos++);
          }
          sb.append(x);
        }

        return new EvaluatorToken(sb.toString());
      }

      if (x == ',') {
        pos++;
        return new EvaluatorToken();
      }

      // Eat number at beginning
      sb = new StringBuilder();
      while (true) {
        if (pos == text.length())
          break;
        x = text.charAt(pos);
        if (x >= '0' && x <= '9') {
          sb.append(x);
          pos++;
          continue;
        }
        break;
      }
      final String numberValue = sb.toString();
      int groupNumber = 0;
      if (numberValue.length() > 0)
        groupNumber = new Integer(numberValue).intValue();
      // Save the next char position
      final int modifierPos = pos;
      // Go to the end of the word
      while (true) {
        if (pos == text.length())
          break;
        x = text.charAt(pos);
        if (x == ',' || x >= '0' && x <= '9' || x <= ' ' && x >= 0)
          break;
        pos++;
      }

      int style = EvaluatorToken.GROUPSTYLE_NONE;
      if (modifierPos != pos) {
        final String modifier = text.substring(modifierPos, pos);
        if (modifier.startsWith("u"))
          style = EvaluatorToken.GROUPSTYLE_UPPER;
        else if (modifier.startsWith("l"))
          style = EvaluatorToken.GROUPSTYLE_LOWER;
        else if (modifier.startsWith("m"))
          style = EvaluatorToken.GROUPSTYLE_MIXED;
        else
          throw new ManifoldCFException("Unknown style: " + modifier);
      }
      return new EvaluatorToken(groupNumber, style);
    }
  }

  /** Class representing a URL regular expression match, for the purposes of determining canonicalization policy */
  protected static class CanonicalizationPolicy {
    protected final Pattern matchPattern;
    protected final boolean reorder;
    protected final boolean removeJavaSession;
    protected final boolean removeAspSession;
    protected final boolean removePhpSession;
    protected final boolean removeBVSession;
    protected final boolean lowercasing;

    public CanonicalizationPolicy(final Pattern matchPattern, final boolean reorder, final boolean removeJavaSession, final boolean removeAspSession, final boolean removePhpSession, final boolean removeBVSession, final boolean lowercasing) {
      this.matchPattern = matchPattern;
      this.reorder = reorder;
      this.removeJavaSession = removeJavaSession;
      this.removeAspSession = removeAspSession;
      this.removePhpSession = removePhpSession;
      this.removeBVSession = removeBVSession;
      this.lowercasing = lowercasing;
    }

    public boolean checkMatch(final String url) {
      final Matcher matcher = matchPattern.matcher(url);
      return matcher.find();
    }

    public boolean canReorder() {
      return reorder;
    }

    public boolean canRemoveJavaSession() {
      return removeJavaSession;
    }

    public boolean canRemoveAspSession() {
      return removeAspSession;
    }

    public boolean canRemovePhpSession() {
      return removePhpSession;
    }

    public boolean canRemoveBvSession() {
      return removeBVSession;
    }

    public boolean canLowercase() {
      return lowercasing;
    }

  }

  /** Class representing a list of canonicalization rules */
  protected static class CanonicalizationPolicies {
    protected final List<CanonicalizationPolicy> rules = new ArrayList<>();

    public CanonicalizationPolicies() {
    }

    public void addRule(final CanonicalizationPolicy rule) {
      rules.add(rule);
    }

    public CanonicalizationPolicy findMatch(final String url) {
      int i = 0;
      while (i < rules.size()) {
        final CanonicalizationPolicy rule = rules.get(i++);
        if (rule.checkMatch(url))
          return rule;
      }
      return null;
    }
  }

  /** Class representing a mapping rule */
  protected static class MappingRule {
    protected final Pattern matchPattern;
    protected final String evalExpression;

    public MappingRule(final Pattern matchPattern, final String evalExpression) {
      this.matchPattern = matchPattern;
      this.evalExpression = evalExpression;
    }

    public boolean checkMatch(final String url) {
      final Matcher matcher = matchPattern.matcher(url);
      return matcher.matches();
    }

    public String map(final String url) throws ManifoldCFException {
      // Create a matcher, and attempt to do a match
      final Matcher matcher = matchPattern.matcher(url);
      if (!matcher.matches()) {
        return null;
      }

      // A match! Now, interpret the output expression
      if (evalExpression == null || evalExpression.length() == 0)
        return url;

      final StringBuilder sb = new StringBuilder();
      final EvaluatorTokenStream et = new EvaluatorTokenStream(evalExpression);

      while (true) {
        final EvaluatorToken t = et.peek();
        if (t == null)
          break;
        switch (t.getType()) {
        case EvaluatorToken.TYPE_COMMA:
          et.advance();
          break;
        case EvaluatorToken.TYPE_GROUP:
          et.advance();
          final String groupValue = matcher.group(t.getGroupNumber());
          switch (t.getGroupStyle()) {
          case EvaluatorToken.GROUPSTYLE_NONE:
            sb.append(groupValue);
            break;
          case EvaluatorToken.GROUPSTYLE_LOWER:
            sb.append(groupValue.toLowerCase(Locale.ROOT));
            break;
          case EvaluatorToken.GROUPSTYLE_UPPER:
            sb.append(groupValue.toUpperCase(Locale.ROOT));
            break;
          case EvaluatorToken.GROUPSTYLE_MIXED:
            if (groupValue.length() > 0) {
              sb.append(groupValue.substring(0, 1).toUpperCase(Locale.ROOT));
              sb.append(groupValue.substring(1).toLowerCase(Locale.ROOT));
            }
            break;
          default:
            throw new ManifoldCFException("Illegal group style");
          }
          break;
        case EvaluatorToken.TYPE_TEXT:
          et.advance();
          sb.append(t.getTextValue());
          break;
        default:
          throw new ManifoldCFException("Illegal token type");
        }
      }
      return sb.toString();
    }

  }

  /** Class that represents all mappings */
  protected static class MappingRules {
    protected final List<MappingRule> mappings = new ArrayList<>();

    public MappingRules() {
    }

    public void add(final MappingRule rule) {
      mappings.add(rule);
    }

    public boolean isMatch(final String url) {
      if (mappings.size() == 0)
        return true;
      for (final MappingRule p : mappings) {
        if (p.checkMatch(url))
          return true;
      }
      return false;
    }

    public String map(final String url) throws ManifoldCFException {
      if (mappings.size() == 0)
        return url;
      for (final MappingRule p : mappings) {
        final String rval = p.map(url);
        if (rval != null)
          return rval;
      }
      return null;
    }
  }

  /**
   * This class describes the url filtering information (for crawling and indexing) obtained from a digested DocumentSpecification.
   */
  protected class DocumentURLFilter {
    /** The version string */
    protected String versionString;
    /** Mapping rules */
    protected final MappingRules mappings = new MappingRules();
    /** The arraylist of include patterns */
    protected final List<Pattern> includePatterns = new ArrayList<>();
    /** The arraylist of exclude patterns */
    protected final List<Pattern> excludePatterns = new ArrayList<>();
    /** The arraylist of index include patterns */
    protected final List<Pattern> includeIndexPatterns = new ArrayList<>();
    /** The arraylist of index exclude patterns */
    protected final List<Pattern> excludeIndexPatterns = new ArrayList<>();
    /** The hash map of seed hosts, to limit urls by, if non-null */
    protected Set<String> seedHosts = null;

    /** List of content exclusion pattern */
    protected final List<Pattern> excludeContentIndexPatterns = new ArrayList<>();

    /** Canonicalization policies */
    protected final CanonicalizationPolicies canonicalizationPolicies = new CanonicalizationPolicies();

    /**
     * Process a document specification to produce a filter. Note that we EXPECT the regexp's in the document specification to be properly formed. This should be checked at save time to prevent
     * errors. Any syntax errors found here will thus cause the include or exclude regexp to be skipped.
     */
    public DocumentURLFilter(final Specification spec) throws ManifoldCFException {
      String includes = ".*";
      String excludes = "";
      String includesIndex = ".*";
      String excludesIndex = "";
      String excludesContentIndex = "";
      String seeds = "";
      final List<String> packList = new ArrayList<>();
      final String[] packStuff = new String[2];
      boolean limitToSeeds = false;
      int i = 0;
      while (i < spec.getChildCount()) {
        final SpecificationNode sn = spec.getChild(i++);
        if (sn.getType().equals(WebcrawlerConfig.NODE_MAP)) {
          final String match = sn.getAttributeValue(WebcrawlerConfig.ATTR_MATCH);
          String map = sn.getAttributeValue(WebcrawlerConfig.ATTR_MAP);
          if (match != null && match.length() > 0) {
            packStuff[0] = match;
            packStuff[1] = map;
            final StringBuilder sb = new StringBuilder();
            packList(sb, packStuff, '=');
            packList.add(sb.toString());
            Pattern p;
            try {
              p = Pattern.compile(match);
            } catch (final java.util.regex.PatternSyntaxException e) {
              throw new ManifoldCFException("Regular expression '" + match + "' is illegal: " + e.getMessage(), e);
            }
            if (map == null)
              map = "";
            mappings.add(new MappingRule(p, map));
          }
        } else if (sn.getType().equals(WebcrawlerConfig.NODE_SEEDS)) {
          // Save the seeds aside; we'll parse them only if we need to.
          seeds = sn.getValue();
          if (seeds == null)
            seeds = "";
        } else if (sn.getType().equals(WebcrawlerConfig.NODE_INCLUDES)) {
          includes = sn.getValue();
          if (includes == null)
            includes = "";
        } else if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDES)) {
          excludes = sn.getValue();
          if (excludes == null)
            excludes = "";
        } else if (sn.getType().equals(WebcrawlerConfig.NODE_INCLUDESINDEX)) {
          includesIndex = sn.getValue();
          if (includesIndex == null)
            includesIndex = "";
        } else if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDESINDEX)) {
          excludesIndex = sn.getValue();
          if (excludesIndex == null)
            excludesIndex = "";
        } else if (sn.getType().equals(WebcrawlerConfig.NODE_LIMITTOSEEDS)) {
          final String value = sn.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
          if (value == null || value.equals(WebcrawlerConfig.ATTRVALUE_FALSE))
            limitToSeeds = false;
          else
            limitToSeeds = true;
        } else if (sn.getType().equals(WebcrawlerConfig.NODE_URLSPEC)) {
          String urlRegexp = sn.getAttributeValue(WebcrawlerConfig.ATTR_REGEXP);
          if (urlRegexp == null)
            urlRegexp = "";
          final String reorder = sn.getAttributeValue(WebcrawlerConfig.ATTR_REORDER);
          boolean reorderValue;
          if (reorder == null)
            reorderValue = false;
          else {
            reorderValue = reorder.equals(WebcrawlerConfig.ATTRVALUE_YES);
          }

          final String javaSession = sn.getAttributeValue(WebcrawlerConfig.ATTR_JAVASESSIONREMOVAL);
          boolean javaSessionValue;
          if (javaSession == null)
            javaSessionValue = false;
          else {
            javaSessionValue = javaSession.equals(WebcrawlerConfig.ATTRVALUE_YES);
          }

          final String aspSession = sn.getAttributeValue(WebcrawlerConfig.ATTR_ASPSESSIONREMOVAL);
          boolean aspSessionValue;
          if (aspSession == null)
            aspSessionValue = false;
          else {
            aspSessionValue = aspSession.equals(WebcrawlerConfig.ATTRVALUE_YES);
          }

          final String phpSession = sn.getAttributeValue(WebcrawlerConfig.ATTR_PHPSESSIONREMOVAL);
          boolean phpSessionValue;
          if (phpSession == null)
            phpSessionValue = false;
          else {
            phpSessionValue = phpSession.equals(WebcrawlerConfig.ATTRVALUE_YES);
          }

          final String bvSession = sn.getAttributeValue(WebcrawlerConfig.ATTR_BVSESSIONREMOVAL);
          boolean bvSessionValue;
          if (bvSession == null)
            bvSessionValue = false;
          else {
            bvSessionValue = bvSession.equals(WebcrawlerConfig.ATTRVALUE_YES);
          }

          final String lowercasing = sn.getAttributeValue(WebcrawlerConfig.ATTR_LOWERCASE);
          boolean lowercasingValue;
          if (lowercasing == null)
            lowercasingValue = false;
          else {
            lowercasingValue = lowercasing.equals(WebcrawlerConfig.ATTRVALUE_YES);
          }

          try {
            canonicalizationPolicies.addRule(new CanonicalizationPolicy(Pattern.compile(urlRegexp), reorderValue, javaSessionValue, aspSessionValue, phpSessionValue, bvSessionValue, lowercasingValue));
          } catch (final java.util.regex.PatternSyntaxException e) {
            throw new ManifoldCFException("Canonicalization regular expression '" + urlRegexp + "' is illegal: " + e.getMessage(), e);
          }
        } else if (sn.getType().equals(WebcrawlerConfig.NODE_EXCLUDESCONTENTINDEX)) {
          excludesContentIndex = sn.getValue();
          if (excludesContentIndex == null)
            excludesContentIndex = "";
        }
      }

      // Note: format change since MCF 1.7 release
      final StringBuilder versionBuffer = new StringBuilder();
      pack(versionBuffer, includesIndex, '+');
      pack(versionBuffer, excludesIndex, '+');
      pack(versionBuffer, excludesContentIndex, '+');
      packList(versionBuffer, packList, '+');
      versionString = versionBuffer.toString();

      List<String> list;
      list = stringToArray(includes);
      compileList(includePatterns, list);
      list = stringToArray(excludes);
      compileList(excludePatterns, list);
      list = stringToArray(includesIndex);
      compileList(includeIndexPatterns, list);
      list = stringToArray(excludesIndex);
      compileList(excludeIndexPatterns, list);
      list = stringToArray(excludesContentIndex);
      compileList(excludeContentIndexPatterns, list);

      if (limitToSeeds) {
        seedHosts = new HashSet<>();
        // Parse all URLs, and put their hosts into the hash table.
        // Break up the seeds string and iterate over the results.
        list = stringToArray(seeds);
        // We must only return valid urls here!!!
        int index = 0;
        while (index < list.size()) {
          final String urlCandidate = list.get(index++);
          try {
            final java.net.URI url = new java.net.URI(urlCandidate);

            final String host = url.getHost();

            if (host != null)
              seedHosts.add(host);
          } catch (final java.net.URISyntaxException e) {
            // Skip the entry
          } catch (final java.lang.IllegalArgumentException e) {
            // Skip the entry
          }

        }
      }
    }

    /**
     * Get whatever contribution to the version string should come from this data.
     */
    public String getVersionString() {
      // In practice, this is NOT what controls the set that is spidered, but rather the set that is indexed
      return versionString;
    }

    /**
     * Check if both a document and host are legal.
     *
     * @throws ManifoldCFException
     */
    public boolean isDocumentAndHostLegal(final String url, final IHistoryActivity activities) throws ManifoldCFException {
      if (!isDocumentLegal(url, activities))
        return false;
      if (seedHosts == null)
        return true;
      try {
        final java.net.URI uri = new java.net.URI(url);
        final String host = uri.getHost();
        if (host == null)
          return false;
        return isHostLegal(host);
      } catch (final java.net.URISyntaxException e) {
        return false;
      } catch (final java.lang.IllegalArgumentException e) {
        return false;
      }

    }

    /**
     * Check if a host is legal.
     */
    public boolean isHostLegal(final String host) {
      if (seedHosts == null)
        return true;
      return seedHosts.contains(host);
    }

    /**
     * Check if the document identifier is legal.
     *
     * @throws ManifoldCFException
     */
    public boolean isDocumentLegal(final String url, final IHistoryActivity activities) throws ManifoldCFException {
      // First, verify that the url matches one of the patterns in the include list.
      int i = 0;
      while (i < includePatterns.size()) {
        final Pattern p = includePatterns.get(i);
        final Matcher m = p.matcher(url);
        if (m.find())
          break;
        i++;
      }
      if (i == includePatterns.size()) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Url '" + url + "' is illegal because no include patterns match it");
        activities.recordActivity(System.currentTimeMillis(), ACTIVITY_FETCH, null, url, "EXCLUDED", "URL has been excluded as it does not match any include filter", null);
        return false;
      }

      // Now make sure it's not in the exclude list.
      i = 0;
      while (i < excludePatterns.size()) {
        final Pattern p = excludePatterns.get(i);
        final Matcher m = p.matcher(url);
        if (m.find()) {
          if (Logging.connectors.isDebugEnabled())
            Logging.connectors.debug("WEB: Url '" + url + "' is illegal because exclude pattern '" + p.toString() + "' matched it");
          activities.recordActivity(System.currentTimeMillis(), ACTIVITY_FETCH, null, url, "EXCLUDED", "URL has been excluded as the exclude pattern " + p.toString() + " matched it", null);
          return false;
        }
        i++;
      }

      return true;
    }

    /**
     * Check if the document identifier is indexable, and return the indexing URL if found.
     *
     * @return null if the url doesn't match or should not be ingested, or the new string if it does.
     */
    public String isDocumentIndexable(final String url, final IProcessActivity activities) throws ManifoldCFException {
      // First, verify that the url matches one of the patterns in the include list.
      int i = 0;
      while (i < includeIndexPatterns.size()) {
        final Pattern p = includeIndexPatterns.get(i);
        final Matcher m = p.matcher(url);
        if (m.find())
          break;
        i++;
      }
      if (i == includeIndexPatterns.size()) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Url '" + url + "' is not indexable because no include patterns match it");
        activities.recordActivity(System.currentTimeMillis(), ACTIVITY_FETCH, null, url, "EXCLUDED", "URL has been excluded as it does not match any include filter", null);
        return null;
      }

      // Now make sure it's not in the exclude list.
      i = 0;
      while (i < excludeIndexPatterns.size()) {
        final Pattern p = excludeIndexPatterns.get(i);
        final Matcher m = p.matcher(url);
        if (m.find()) {
          if (Logging.connectors.isDebugEnabled())
            Logging.connectors.debug("WEB: Url '" + url + "' is not indexable because exclude pattern '" + p.toString() + "' matched it");
          activities.recordActivity(System.currentTimeMillis(), ACTIVITY_FETCH, null, url, "EXCLUDED", "URL has been excluded as the exclude pattern " + p.toString() + " matched it", null);
          return null;
        }
        i++;
      }

      final String rval = mappings.map(url);
      if (rval == null) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Url '" + url + "' is not indexable because it did not match a mapping rule");
        activities.recordActivity(System.currentTimeMillis(), ACTIVITY_FETCH, null, url, "EXCLUDED", "URL has been excluded because it did not match a mapping rule", null);
      }

      return rval;
    }

    /** Get canonicalization policies */
    public CanonicalizationPolicies getCanonicalizationPolicies() {
      return canonicalizationPolicies;
    }

    public boolean isDocumentContentIndexable(final String documentIdentifier) throws ManifoldCFException {
      final String content = findSpecifiedContent(documentIdentifier, excludeContentIndexPatterns);
      if (content != null) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Url '" + documentIdentifier + "' is not indexable because content exclusion pattern was matched");

        return false;
      }
      return true;
    }

    protected String findSpecifiedContent(final String currentURI, final List<Pattern> patterns) throws ManifoldCFException {
      if (excludeContentIndexPatterns.isEmpty()) {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: no content exclusion rule supplied... returning");
        return null;
      }

      final FindContentHandler handler = new FindContentHandler(currentURI, patterns);
      handleHTML(currentURI, handler);
      return handler.getTargetURI();
    }

  }

  protected static class FetchStatus {
    public int sessionState = SESSIONSTATE_NORMAL;
    public int resultSignal = RESULT_NO_DOCUMENT;
    // The result context message, which will be used for logging and activity logging if enabled.
    public String contextMessage = null;
    // The result context exception, which will be used for logging if needed.
    public Throwable contextException = null;
    // The checksum, which will be needed if resultSignal is RESULT_VERSION_NEEDED.
    public String checkSum = null;
    // The headers, which will be needed if resultSignal is RESULT_VERSION_NEEDED.
    public Map<String, List<String>> headerData = null;

  }

}
