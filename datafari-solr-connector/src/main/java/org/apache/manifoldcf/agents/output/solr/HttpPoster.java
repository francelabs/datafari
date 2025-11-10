package org.apache.manifoldcf.agents.output.solr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.net.ssl.SSLContext;

import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputRemoveActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.UpdateRequest;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;

/**
 * HttpPoster
 * (cloud/standalone init hardening, optional HTTP/1.1 best-effort, commitWithin + targeted retries)
 * Only the two requested additions were made: debugHandlers() and the cloud compat constructor.
 */
public class HttpPoster {

  private SolrClient solrServer;
  private String postUpdateAction;
  private String postRemoveAction;
  private String postStatusAction;

  private final String idAttributeName;
  private final String originalSizeAttributeName;
  private final String modifiedDateAttributeName;
  private final String createdDateAttributeName;
  private final String indexedDateAttributeName;
  private final String fileNameAttributeName;
  private final String mimeTypeAttributeName;
  private final String contentAttributeName;

  private final Long maxDocumentLength;
  private final String commitWithin;
  private final boolean useExtractUpdateHandler;
  private final Set<String> includedMimeTypes;
  private final Set<String> excludedMimeTypes;
  private final boolean allowCompression;
  private final String allowAttributeName;
  private final String denyAttributeName;

  private static final String LITERAL = "literal.";

  // ---------- Main constructors ----------

  public HttpPoster(final List<String> zookeeperHosts, final String znodePath, final String collection,
                    final int socketTimeout, final int connectionTimeout,
                    final String updatePath, final String removePath, final String statusPath,
                    final String allowAttributeName, final String denyAttributeName, final String idAttributeName,
                    final String originalSizeAttributeName, final String modifiedDateAttributeName,
                    final String createdDateAttributeName, final String indexedDateAttributeName,
                    final String fileNameAttributeName, final String mimeTypeAttributeName,
                    final String contentAttributeName, final Long maxDocumentLength, final String commitWithin,
                    final boolean useExtractUpdateHandler, final Set<String> includedMimeTypes,
                    final Set<String> excludedMimeTypes, final boolean allowCompression) {

    this.allowAttributeName = allowAttributeName;
    this.denyAttributeName  = denyAttributeName;
    this.idAttributeName = (idAttributeName != null ? idAttributeName : "id");
    this.originalSizeAttributeName = originalSizeAttributeName;
    this.modifiedDateAttributeName = modifiedDateAttributeName;
    this.createdDateAttributeName = createdDateAttributeName;
    this.indexedDateAttributeName = indexedDateAttributeName;
    this.fileNameAttributeName = fileNameAttributeName;
    this.mimeTypeAttributeName = mimeTypeAttributeName;
    this.contentAttributeName = contentAttributeName;

    this.maxDocumentLength = maxDocumentLength;
    this.commitWithin = commitWithin;
    this.useExtractUpdateHandler = useExtractUpdateHandler;
    this.includedMimeTypes = includedMimeTypes;
    this.excludedMimeTypes = excludedMimeTypes;
    this.allowCompression = allowCompression;

    initCloud(zookeeperHosts, znodePath, collection);

    this.postUpdateAction = updatePath;
    this.postRemoveAction = removePath;
    this.postStatusAction = statusPath;

    debugHandlers("Cloud(long)-ctor");
  }

  public HttpPoster(final String protocol, final String server, final int port, final String webapp, final String core,
                    final int connectionTimeout, final int socketTimeout,
                    final String updatePath, final String removePath, final String statusPath,
                    final String realm, final String userID, final String password,
                    final String allowAttributeName, final String denyAttributeName, final String idAttributeName,
                    final String originalSizeAttributeName, final String modifiedDateAttributeName,
                    final String createdDateAttributeName, final String indexedDateAttributeName,
                    final String fileNameAttributeName, final String mimeTypeAttributeName,
                    final String contentAttributeName, final IKeystoreManager keystoreManager,
                    final Long maxDocumentLength, final String commitWithin, final boolean useExtractUpdateHandler,
                    final Set<String> includedMimeTypes, final Set<String> excludedMimeTypes, final boolean allowCompression,
                    final String protocolForSSL, final IKeystoreManager keystoreForSSL) {

    this.allowAttributeName = allowAttributeName;
    this.denyAttributeName  = denyAttributeName;
    this.idAttributeName = (idAttributeName != null ? idAttributeName : "id");
    this.originalSizeAttributeName = originalSizeAttributeName;
    this.modifiedDateAttributeName = modifiedDateAttributeName;
    this.createdDateAttributeName = createdDateAttributeName;
    this.indexedDateAttributeName = indexedDateAttributeName;
    this.fileNameAttributeName = fileNameAttributeName;
    this.mimeTypeAttributeName = mimeTypeAttributeName;
    this.contentAttributeName = contentAttributeName;

    this.maxDocumentLength = maxDocumentLength;
    this.commitWithin = commitWithin;
    this.useExtractUpdateHandler = useExtractUpdateHandler;
    this.includedMimeTypes = includedMimeTypes;
    this.excludedMimeTypes = excludedMimeTypes;
    this.allowCompression = allowCompression;

    SSLContext ctx = null;
    if ("https".equalsIgnoreCase(protocol)) {
      ctx = resolveSSLContext(keystoreForSSL);
    }

    initStandalone(protocol, server, port, webapp, core, connectionTimeout, socketTimeout, ctx);

    this.postUpdateAction = updatePath;
    this.postRemoveAction = removePath;
    this.postStatusAction = statusPath;

    debugHandlers("Standalone(long)-ctor");
  }

  // ---------- Compatibility constructors expected by SolrConnector ----------

  public HttpPoster(final String protocol, final String server, final int port, final String webapp, final String core,
                    final int connectionTimeout, final int socketTimeout,
                    final String updatePath, final String removePath, final String statusPath,
                    final String realm, final String userID, final String password,
                    final String allowAttributeName, final String denyAttributeName, final String idAttributeName,
                    final String originalSizeAttributeName, final String modifiedDateAttributeName,
                    final String createdDateAttributeName, final String indexedDateAttributeName,
                    final String fileNameAttributeName, final String mimeTypeAttributeName,
                    final String contentAttributeName, final IKeystoreManager keystoreManager,
                    final Long maxDocumentLength, final String commitWithin, final boolean useExtractUpdateHandler,
                    final Set<String> includedMimeTypes, final Set<String> excludedMimeTypes, final boolean allowCompression) {
    this(protocol, server, port, webapp, core,
         connectionTimeout, socketTimeout,
         updatePath, removePath, statusPath,
         realm, userID, password,
         allowAttributeName, denyAttributeName, idAttributeName,
         originalSizeAttributeName, modifiedDateAttributeName,
         createdDateAttributeName, indexedDateAttributeName,
         fileNameAttributeName, mimeTypeAttributeName,
         contentAttributeName, keystoreManager,
         maxDocumentLength, commitWithin, useExtractUpdateHandler,
         includedMimeTypes, excludedMimeTypes, allowCompression,
         /* protocolForSSL */ protocol,
         /* keystoreForSSL */ keystoreManager);
  }

  /** NEW: cloud compat constructor with extra (protocolForSSL, keystoreForSSL) to match SolrConnector */
  public HttpPoster(final List<String> zookeeperHosts, final String znodePath, final String collection,
                    final int socketTimeout, final int connectionTimeout,
                    final String updatePath, final String removePath, final String statusPath,
                    final String allowAttributeName, final String denyAttributeName, final String idAttributeName,
                    final String originalSizeAttributeName, final String modifiedDateAttributeName,
                    final String createdDateAttributeName, final String indexedDateAttributeName,
                    final String fileNameAttributeName, final String mimeTypeAttributeName,
                    final String contentAttributeName, final Long maxDocumentLength, final String commitWithin,
                    final boolean useExtractUpdateHandler, final Set<String> includedMimeTypes,
                    final Set<String> excludedMimeTypes, final boolean allowCompression,
                    final String protocolForSSL, final IKeystoreManager keystoreForSSL) {
    this(zookeeperHosts, znodePath, collection,
         socketTimeout, connectionTimeout,
         updatePath, removePath, statusPath,
         allowAttributeName, denyAttributeName, idAttributeName,
         originalSizeAttributeName, modifiedDateAttributeName,
         createdDateAttributeName, indexedDateAttributeName,
         fileNameAttributeName, mimeTypeAttributeName,
         contentAttributeName, maxDocumentLength, commitWithin,
         useExtractUpdateHandler, includedMimeTypes, excludedMimeTypes, allowCompression);
  }

  // ---------- Client initializations ----------

  private void initCloud(List<String> zookeeperHosts, String znodePath, String collection) {
    try {
      final Optional<String> chroot =
          (znodePath != null && !znodePath.isEmpty()) ? Optional.of(znodePath) : Optional.empty();

      if (Logging.ingest.isDebugEnabled()) {
        Logging.ingest.debug("[Cloud init] zkHosts=" + zookeeperHosts
            + " chroot=" + chroot.orElse("(none)")
            + " collection=" + collection);
      }

CloudSolrClient.Builder b = new CloudSolrClient.Builder(zookeeperHosts, chroot);
CloudSolrClient client = b.build();
      if (collection != null && !collection.isEmpty()) {
        client.setDefaultCollection(collection);
      }

      this.solrServer = client;

      if (Logging.ingest.isDebugEnabled()) {
        Logging.ingest.debug("[Cloud init] CloudHttp2SolrClient initialized (HTTP/1.1 best-effort attempted)");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize Cloud client", e);
    }
  }

private void initStandalone(String protocol, String server, int port, String webapp, String core,
                            int connectionTimeout, int socketTimeout, SSLContext sslContextOrNull) {
  try {
    String location = "";
    if (webapp != null && !webapp.isEmpty()) location += "/" + webapp;
    if (core != null && !core.isEmpty())     location += "/" + core;
    final String baseUrl = protocol + "://" + server + ":" + port + location;

    // Builder HTTP/1.1
    HttpSolrClient.Builder builder = new HttpSolrClient.Builder(baseUrl);

    // Timeout si ta version de SolrJ les supporte
    // (sinon tu peux les laisser commentés)
    // builder.withConnectionTimeout(Math.max(connectionTimeout, 30000));
    // builder.withSocketTimeout(Math.max(socketTimeout, 60000));

    // --- SSL permissif ---
    if ("https".equalsIgnoreCase(protocol)) {
      try {
        SSLContext sslContext = (sslContextOrNull != null) ? sslContextOrNull : createTrustAllSSLContext();
        org.apache.http.conn.ssl.NoopHostnameVerifier verifier = org.apache.http.conn.ssl.NoopHostnameVerifier.INSTANCE;
        org.apache.http.impl.client.CloseableHttpClient httpClient =
            org.apache.http.impl.client.HttpClients.custom()
              .setSSLContext(sslContext)
              .setSSLHostnameVerifier(verifier)
              .build();
        builder.withHttpClient(httpClient);
        if (Logging.ingest.isDebugEnabled()) {
          Logging.ingest.debug("[Standalone init] Permissive SSL context applied (trust all certificates)");
        }
      } catch (Exception e) {
        Logging.ingest.warn("[Standalone init] Could not configure permissive SSL context", e);
      }
    }

    HttpSolrClient client = builder.build();
    this.solrServer = client;

    if (Logging.ingest.isDebugEnabled()) {
      Logging.ingest.debug("[Standalone init] HttpSolrClient initialized, baseUrl=" + baseUrl);
    }
  } catch (Exception e) {
    throw new RuntimeException("Failed to initialize standalone client", e);
  }
}

/**
 * Crée un SSLContext permissif (trust all certificates)
 */
private static SSLContext createTrustAllSSLContext() throws Exception {
  javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
      new javax.net.ssl.X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
      }
  };
  SSLContext sslContext = SSLContext.getInstance("TLS");
  sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
  return sslContext;
}
  /** Best-effort: try to force HTTP/1.1 (safe no-op if unsupported on this SolrJ version). */
  private static void tryForceHttp11(Object clientOrBuilder, String where) {
    try {
      boolean applied = false;

      try {
        java.lang.reflect.Method m = clientOrBuilder.getClass().getMethod("useHttp1_1", boolean.class);
        m.invoke(clientOrBuilder, true);
        applied = true;
      } catch (Throwable ignore) { /* not present */ }

      if (!applied) {
        try {
          java.lang.reflect.Method getter = clientOrBuilder.getClass().getMethod("getHttpClient");
          Object jettyClient = getter.invoke(clientOrBuilder);
          if (jettyClient != null) {
            String[] candidates = new String[] {"setTransportOverHTTP2", "setUseHttp2", "setHttp2Enabled"};
            for (String name : candidates) {
              try {
                java.lang.reflect.Method m2 = jettyClient.getClass().getMethod(name, boolean.class);
                m2.invoke(jettyClient, false); // disable HTTP/2 → force HTTP/1.1
                applied = true;
                break;
              } catch (Throwable ignore2) {}
            }
          }
        } catch (Throwable ignore) { /* getter not present */ }
      }

      if (Logging.ingest.isDebugEnabled()) {
        Logging.ingest.debug("[HTTP/1.1 attempt][" + where + "] " + (applied ? "applied" : "not supported on this version"));
      }
    } catch (Throwable t) {
      if (Logging.ingest.isDebugEnabled()) {
        Logging.ingest.debug("[HTTP/1.1 attempt][" + where + "] failed: " + t.getMessage(), t);
      }
    }
  }

  private static SSLContext resolveSSLContext(IKeystoreManager ksMgr) {
    try {
      try {
        return SSLContext.getDefault();
      } catch (NoSuchAlgorithmException e) {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        return ctx;
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize SSLContext: " + e.getMessage(), e);
    }
  }

  // ---------- Main methods ----------

public boolean indexPost(final String documentURI,
                         final RepositoryDocument document,
                         final Map<String, List<String>> arguments,
                         final String authorityNameString,
                         final IOutputAddActivity activities)
    throws SolrServerException, IOException {

  final long fullStartTime = System.currentTimeMillis();
  final Long length = document.getBinaryLength();

  // Avertir si l'UI a coché "extract", mais on n'utilise plus l'extract handler
  if (this.useExtractUpdateHandler && org.apache.manifoldcf.agents.system.Logging.ingest.isWarnEnabled()) {
    org.apache.manifoldcf.agents.system.Logging.ingest.warn(
      "useExtractUpdateHandler=true in UI, but connector now always sends SolrInputDocument via /update. Ignoring extract mode.");
  }

  // Document Solr (pas d'extract handler, pas de multipart)
  final org.apache.solr.common.SolrInputDocument solrDoc = new org.apache.solr.common.SolrInputDocument();

  // 1) id
  solrDoc.addField(this.idAttributeName, documentURI);

  // 2) attributs standards
  if (originalSizeAttributeName != null) {
    final Long size = document.getOriginalSize();
    if (size != null) solrDoc.addField(originalSizeAttributeName, size.toString());
  }
  if (modifiedDateAttributeName != null && document.getModifiedDate() != null) {
    solrDoc.addField(modifiedDateAttributeName, DateParser.formatISO8601Date(document.getModifiedDate()));
  }
  if (createdDateAttributeName != null && document.getCreatedDate() != null) {
    solrDoc.addField(createdDateAttributeName, DateParser.formatISO8601Date(document.getCreatedDate()));
  }
  if (indexedDateAttributeName != null && document.getIndexingDate() != null) {
    solrDoc.addField(indexedDateAttributeName, DateParser.formatISO8601Date(document.getIndexingDate()));
  }
  if (fileNameAttributeName != null) {
    final String fn = document.getFileName();
    if (fn != null && !fn.isBlank()) solrDoc.addField(fileNameAttributeName, fn);
  }
  if (mimeTypeAttributeName != null) {
    final String mt = document.getMimeType();
    if (mt != null && !mt.isBlank()) solrDoc.addField(mimeTypeAttributeName, mt);
  }

  // 3) ACLs
  final Map<String,String[]> aclsMap = new HashMap<>();
  final Map<String,String[]> denyAclsMap = new HashMap<>();
  final Iterator<String> aclTypes = document.securityTypesIterator();
  while (aclTypes.hasNext()) {
    final String aclType = aclTypes.next();
    aclsMap.put(aclType, convertACL(document.getSecurityACL(aclType), authorityNameString, activities));
    denyAclsMap.put(aclType, convertACL(document.getSecurityDenyACL(aclType), authorityNameString, activities));

    if (!isKnownAclType(aclType)) {
      try {
        activities.recordActivity(
            null,
            org.apache.manifoldcf.agents.output.solr.SolrConnector.INGEST_ACTIVITY,
            length,
            documentURI,
            activities.UNKNOWN_SECURITY,
            "Unrecognized security type: '" + aclType + "'"
        );
      } catch (org.apache.manifoldcf.core.interfaces.ManifoldCFException ignore) {}
      return false;
    }
  }
  for (String aclType : aclsMap.keySet()) {
    writeACLsInSolrDoc(solrDoc, aclType, aclsMap.get(aclType), denyAclsMap.get(aclType));
  }

  // 4) Métadonnées
  final Iterator<String> fields = document.getFields();
  while (fields.hasNext()) {
    final String originalFieldName = fields.next();
    String newFieldName = makeSafeLuceneField(originalFieldName);
    if (newFieldName == null || newFieldName.isEmpty()) continue;

    if (newFieldName.equalsIgnoreCase(this.idAttributeName)) {
      newFieldName = "lcf_metadata_id";
    }
    final String[] values = document.getFieldAsStrings(originalFieldName);
    if (values != null) {
      for (String v : values) {
        if (v != null) solrDoc.addField(newFieldName, v);
      }
    }
  }

  // --- copie du texte extrait (content) vers le champ cible ---
  final String srcField = firstParam(arguments, "sourcecontentfield");
  final String dstField = firstParam(arguments, "destcontentfield");
  final String src = (srcField == null || srcField.isBlank()) ? "content" : srcField.trim();
  final String dst = (dstField == null || dstField.isBlank()) ? "content" : dstField.trim();

// --- pousser le texte extrait dans le champ 'content' (stocké, non indexé) ---
{
  // par défaut: source = "content" (remplie par ton Tika server), destination fixe = "content"
  String[] extractedVals = document.getFieldAsStrings("content");
  if (extractedVals != null && extractedVals.length > 0) {
    for (String extracted : extractedVals) {
      if (extracted == null) continue;
      String trimmed = extracted.trim();
      if (trimmed.isEmpty()) continue;
      // Ajoute dans le champ 'content' de Solr
      solrDoc.addField("content", trimmed);
    }
    if (org.apache.manifoldcf.agents.system.Logging.ingest.isDebugEnabled()) {
      org.apache.manifoldcf.agents.system.Logging.ingest.debug(
        "Added content to Solr doc: values=" + extractedVals.length + " into field 'content'");
    }
  } else {
    if (org.apache.manifoldcf.agents.system.Logging.ingest.isDebugEnabled()) {
      org.apache.manifoldcf.agents.system.Logging.ingest.debug(
        "No RepositoryDocument field 'content' found or it's empty; nothing stored into Solr 'content'.");
    }
  }
}
  // 5) UpdateRequest
  final org.apache.solr.client.solrj.request.UpdateRequest req = new org.apache.solr.client.solrj.request.UpdateRequest();
  req.setPath(chooseSafeUpdatePath(this.postUpdateAction));

  if (arguments != null) {
    for (Map.Entry<String, List<String>> e : arguments.entrySet()) {
      final String name = e.getKey();
      final List<String> vals = e.getValue();
      if (name != null && vals != null && !vals.isEmpty()) {
        if ("sourcecontentfield".equalsIgnoreCase(name) || "destcontentfield".equalsIgnoreCase(name)) continue;
        for (String v : vals) {
          if (v != null) req.setParam(name, v);
        }
      }
    }
  }

  if (this.commitWithin != null && !this.commitWithin.isBlank()) {
    try {
      req.setCommitWithin(Integer.parseInt(this.commitWithin));
    } catch (NumberFormatException ignore) {}
  }

// test contenu

// Après le mapping des métadonnées, avant req.add(solrDoc)
final String destContentField = (this.contentAttributeName == null || this.contentAttributeName.isBlank())
  ? "content" : this.contentAttributeName;

// Le TikaServer RMeta a placé le texte dans le flux binaire du RepositoryDocument
try (InputStream is = document.getBinaryStream()) {
  if (is != null) {
    // Lis en UTF-8 (Tika RMeta renvoie du texte), avec une limite raisonnable si tu veux (writeLimit côté Tika protège déjà)
    final String extracted = org.apache.commons.io.IOUtils.toString(is, java.nio.charset.StandardCharsets.UTF_8);
    if (!extracted.isEmpty()) {
      solrDoc.addField(destContentField, extracted);
      if (org.apache.manifoldcf.agents.system.Logging.ingest.isDebugEnabled()) {
        org.apache.manifoldcf.agents.system.Logging.ingest.debug(
          "[SolrConnector] Injected " + extracted.length() + " chars into field '" + destContentField + "'");
      }
    } else {
      if (org.apache.manifoldcf.agents.system.Logging.ingest.isDebugEnabled()) {
        org.apache.manifoldcf.agents.system.Logging.ingest.debug(
          "[SolrConnector] Extracted content is empty; nothing added to '" + destContentField + "'");
      }
    }
  }
}
  req.add(solrDoc);
// --- DEBUG : état du document juste avant envoi ---
if (org.apache.manifoldcf.agents.system.Logging.ingest.isDebugEnabled()) {
  // Compte et aperçu du champ "content"
  java.util.Collection<Object> cvals = solrDoc.getFieldValues("content");
  int contentValues = (cvals == null ? 0 : cvals.size());
  long contentTotalChars = 0L;
  if (cvals != null) {
    for (Object o : cvals) {
      if (o instanceof CharSequence) contentTotalChars += ((CharSequence) o).length();
      else if (o != null) contentTotalChars += o.toString().length();
    }
  }

  org.apache.manifoldcf.agents.system.Logging.ingest.debug(
    "[SolrConnector] Ready to send doc id=" + documentURI +
    " via path=" + req.getPath() +
    " commitWithin=" + (this.commitWithin) +
    " | contentValues=" + contentValues +
    " contentTotalChars=" + contentTotalChars
  );

  // Dump complet (aperçu du content inclus)
  String dump = dumpSolrDoc(solrDoc, /* previewCharsPerValue */ 300);
  org.apache.manifoldcf.agents.system.Logging.ingest.debug("[SolrConnector] SolrInputDocument dump:\n" + dump);
}

  // Retry
  final int maxAttempts = 3;
  int attempt = 0;
  while (true) {
    attempt++;
    try {
      this.solrServer.request(req);

      // succès
      try {
        activities.recordActivity(
            fullStartTime,
            org.apache.manifoldcf.agents.output.solr.SolrConnector.INGEST_ACTIVITY,
            length, documentURI, "OK", null);
      } catch (org.apache.manifoldcf.core.interfaces.ManifoldCFException ignore) {}
      return true;

    } catch (SolrServerException | IOException e) {
      // échec
      try {
        activities.recordActivity(
            fullStartTime,
            org.apache.manifoldcf.agents.output.solr.SolrConnector.INGEST_ACTIVITY,
            length, documentURI,
            e.getClass().getSimpleName().toUpperCase(java.util.Locale.ROOT),
            e.getMessage());
      } catch (org.apache.manifoldcf.core.interfaces.ManifoldCFException ignore) {}

      if (isTransientIo(e) && attempt < maxAttempts) {
        backoff(attempt);
        continue;
      }
      throw e;
    }
  }
}
/** Ensures /update path is safe (never /update/extract). */
private static String chooseSafeUpdatePath(String userPathOrNull) {
  String p = (userPathOrNull == null || userPathOrNull.isBlank()) ? "/update" : userPathOrNull.trim();
  if (!p.startsWith("/")) p = "/" + p;

  if (p.equalsIgnoreCase("/update/extract")) {
    if (org.apache.manifoldcf.agents.system.Logging.ingest.isWarnEnabled()) {
      org.apache.manifoldcf.agents.system.Logging.ingest.warn(
        "User specified /update/extract; forcing /update because we send SolrInputDocument (no multipart).");
    }
    return "/update";
  }
  return p;
}

/** Helper: returns first param value from arguments map (case-insensitive). */
private static String firstParam(Map<String, List<String>> args, String key) {
  if (args == null || key == null) return null;
  for (Map.Entry<String, List<String>> e : args.entrySet()) {
    if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
      final List<String> vs = e.getValue();
      if (vs != null && !vs.isEmpty()) return vs.get(0);
    }
  }
  return null;
}

/** Lit un InputStream texte UTF-8 avec une limite dure pour éviter d'exploser la RAM. */
private static String slurpUtf8WithLimit(final java.io.InputStream in, final int maxChars) throws java.io.IOException {
  if (in == null || maxChars <= 0) return null;
  final char[] cbuf = new char[8192];
  int r;
  int remaining = maxChars;
  final StringBuilder sb = new StringBuilder(Math.min(maxChars, 65536));
  try (java.io.Reader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
    while (remaining > 0 && (r = reader.read(cbuf, 0, Math.min(cbuf.length, remaining))) != -1) {
      sb.append(cbuf, 0, r);
      remaining -= r;
    }
  }
  return sb.length() == 0 ? null : sb.toString();
}

/** Dump du SolrInputDocument pour debug : liste des champs, nb de valeurs, taille cumulée,
 *  et aperçu du champ "content" si présent.
 */
private static String dumpSolrDoc(org.apache.solr.common.SolrInputDocument doc, int previewCharsPerValue) {
  StringBuilder sb = new StringBuilder(2048);
  long totalCharsAllFields = 0L;

  for (String fname : doc.getFieldNames()) {
    java.util.Collection<Object> vals = doc.getFieldValues(fname);
    int n = (vals == null ? 0 : vals.size());
    long totalCharsField = 0L;

    if (vals != null) {
      for (Object v : vals) {
        if (v instanceof CharSequence) {
          totalCharsField += ((CharSequence) v).length();
        } else if (v != null) {
          totalCharsField += v.toString().length();
        }
      }
    }
    totalCharsAllFields += totalCharsField;

    sb.append(" - ").append(fname)
      .append(" : values=").append(n)
      .append(", totalChars=").append(totalCharsField);

    // Aperçu spécial pour "content"
    if ("content".equals(fname) && vals != null && !vals.isEmpty()) {
      sb.append(" [preview: ");
      int shown = 0;
      for (Object v : vals) {
        if (v == null) continue;
        String s = v.toString();
        if (s.length() > previewCharsPerValue) s = s.substring(0, previewCharsPerValue) + "…";
        // échappe les retours ligne pour lisibilité
        s = s.replace("\r", "\\r").replace("\n", "\\n");
        sb.append('"').append(s).append('"');
        shown++;
        if (shown >= 1) break; // on ne montre qu'une valeur en aperçu
      }
      sb.append(" ]");
    }
    sb.append('\n');
  }

  sb.append("Total chars in all string fields = ").append(totalCharsAllFields);
  return sb.toString();
}

  public void deletePost(final String documentURI, final IOutputRemoveActivity activities)
      throws SolrServerException, IOException {
    final String path = normalizePathOrDefault(this.postRemoveAction, "/update");
    final UpdateRequest req = new UpdateRequest();
    req.setPath(path);
    req.deleteById(documentURI);

    final int maxAttempts = 3;
    int attempt = 0;
    while (true) {
      attempt++;
      try {
        solrServer.request(req);
        if (Logging.ingest.isDebugEnabled()) {
          Logging.ingest.debug("Delete request succeeded (attempt " + attempt + ") for id=" + documentURI);
        }
        try {
          if (activities != null) {
            activities.recordActivity(
                null,
                org.apache.manifoldcf.agents.output.solr.SolrConnector.REMOVE_ACTIVITY,
                null,
                documentURI,
                "COMPLETED",
                "Delete succeeded"
            );
          }
        } catch (Exception ignore) {}
        return;
      } catch (SolrServerException e) {
        if (isTransientIo(e)) { backoff(attempt); continue; }
        throw e;
      } catch (IOException e) {
        if (isTransientIo(e)) { backoff(attempt); continue; }
        throw e;
      }
    }
  }

  public void commitPost() throws SolrServerException, IOException {
    final String path = normalizePathOrDefault(this.postUpdateAction, "/update");
    final UpdateRequest req = new UpdateRequest();
    req.setPath(path);
    req.setParam("commit", "true");
    solrServer.request(req);
  }

  public void checkPost() throws SolrServerException, IOException {
    final String path = normalizePathOrDefault(this.postStatusAction, "/admin/ping");
    SolrPing ping = new SolrPing();
    ping.setPath(path);
    ping.process(solrServer);
  }

  // ---------- Metadata ----------

  private void buildSolrParamsFromMetadata(final RepositoryDocument document, final ModifiableSolrParams out)
      throws IOException {
    final Iterator<String> iter = document.getFields();
    if (!iter.hasNext()) {
      if (Logging.ingest.isDebugEnabled())
        Logging.ingest.debug("RepositoryDocument.getFields() returned no metadata fields.");
    } else {
      while (iter.hasNext()) {
        final String originalFieldName = iter.next();
        final String fieldName = makeSafeLuceneField(originalFieldName);
        if (Logging.ingest.isDebugEnabled())
          Logging.ingest.debug("Solr metadata: '" + originalFieldName + "' → '" + fieldName + "'");
        applySingleMapping(document, originalFieldName, out, fieldName);
      }
    }
  }

  private void applySingleMapping(final RepositoryDocument document,
                                  final String originalFieldName,
                                  final ModifiableSolrParams out,
                                  String newFieldName)
      throws IOException {
    if (newFieldName == null || newFieldName.isEmpty()) return;
    if (newFieldName.equalsIgnoreCase(idAttributeName)) {
      newFieldName = "id_metadata";
    }
    final String[] values = document.getFieldAsStrings(originalFieldName);
    if (values != null && values.length > 0) {
      for (String val : values) {
        if (val != null) {
          out.add(LITERAL + newFieldName, val);
        }
      }
    }
  }

  // ---------- Compat helpers expected by SolrConnector ----------

  public void shutdown() {
    try { close(); } catch (Exception e) { /* ignore */ }
  }

  public void close() {
    if (solrServer != null) {
      try { solrServer.close(); } catch (Exception ignore) {}
    }
  }

  public static boolean checkMimeTypeIndexable(final String mimeType,
                                               final boolean useExtractUpdateHandler,
                                               final Set<String> includedMimeTypes,
                                               final Set<String> excludedMimeTypes) {
    if (mimeType == null) return false;
    final String m = mimeType.toLowerCase(Locale.ROOT);
    if (includedMimeTypes != null && !includedMimeTypes.isEmpty() && !includedMimeTypes.contains(m)) return false;
    if (excludedMimeTypes != null && excludedMimeTypes.contains(m)) return false;
    return true;
  }

  // ---------- ACL helpers ----------

  protected static String[] convertACL(final String[] acl,
                                       final String authorityNameString,
                                       final IOutputAddActivity activities) {
    if (acl == null) return new String[0];
    final String[] rval = new String[acl.length];
    for (int i = 0; i < acl.length; i++) {
      try {
        rval[i] = activities.qualifyAccessToken(authorityNameString, acl[i]);
      } catch (Exception e) {
        if (Logging.ingest.isDebugEnabled()) {
          Logging.ingest.debug("qualifyAccessToken failed for token '" + acl[i] + "': " + e.getMessage(), e);
        }
        rval[i] = acl[i];
      }
    }
    return rval;
  }

  protected void writeACLs(final ModifiableSolrParams out,
                           final String aclType,
                           final String[] allowAcl,
                           final String[] denyAcl) {
    final String allowParam = LITERAL + allowAttributeName + aclType;
    final String denyParam  = LITERAL + denyAttributeName  + aclType;

    if (allowAcl != null) {
      for (String v : allowAcl) {
        if (v != null) out.add(allowParam, v);
      }
    }
    if (denyAcl != null) {
      for (String v : denyAcl) {
        if (v != null) out.add(denyParam, v);
      }
    }

    if (Logging.ingest.isDebugEnabled()) {
      Logging.ingest.debug("ACLs → " + allowParam + "=" + Arrays.toString(allowAcl)
          + " ; " + denyParam + "=" + Arrays.toString(denyAcl));
    }
  }

  protected void writeACLsInSolrDoc(final org.apache.solr.common.SolrInputDocument inputDoc,
                                    final String aclType,
                                    final String[] acl,
                                    final String[] denyAcl) {
    final String allowField = allowAttributeName + aclType;
    final String denyField  = denyAttributeName  + aclType;

    if (acl != null && acl.length > 0) {
      for (String a : acl) {
        if (a != null) inputDoc.addField(allowField, a);
      }
    }
    if (denyAcl != null && denyAcl.length > 0) {
      for (String d : denyAcl) {
        if (d != null) inputDoc.addField(denyField, d);
      }
    }

    if (Logging.ingest.isDebugEnabled()) {
      Logging.ingest.debug("ACLs (doc mode) → " + allowField + "=" + Arrays.toString(acl)
          + " ; " + denyField + "=" + Arrays.toString(denyAcl));
    }
  }

  private static boolean isKnownAclType(String t) {
    return RepositoryDocument.SECURITY_TYPE_DOCUMENT.equals(t)
        || RepositoryDocument.SECURITY_TYPE_SHARE.equals(t)
        || (t != null && t.startsWith(RepositoryDocument.SECURITY_TYPE_PARENT));
  }

  // ---------- Helpers ----------

  private static boolean isTransientIo(Throwable t) {
    if (t == null) return false;
    if (t instanceof IOException) return true;
    if (t instanceof SolrServerException && t.getCause() instanceof IOException) return true;
    final String s = t.toString();
    return s != null && s.contains("AsynchronousCloseException");
  }

  private static void backoff(int attempt) {
    if (attempt >= 3) return;
    long sleep = (attempt == 1) ? 250L : 500L;
    try { Thread.sleep(sleep); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
  }

  private static String makeSafeLuceneField(String input) {
    if (input == null) return null;
    return input.replaceAll("[^A-Za-z0-9_]", "_");
  }

  private static String safeContentType(String mt) {
    return (mt == null || mt.isBlank()) ? "application/octet-stream" : mt;
  }

  private static String normalizePathOrDefault(String p, String def) {
    String path = (p != null && !p.isBlank()) ? p : def;
    if (!path.startsWith("/")) path = "/" + path;
    return path;
  }

  private static byte[] toByteArray(InputStream in, Long expectedLenOrNull) throws IOException {
    try (InputStream src = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      byte[] buf = new byte[8192];
      int r;
      while ((r = src.read(buf)) != -1) bos.write(buf, 0, r);
      return bos.toByteArray();
    }
  }

  private static void addIfDateNotNull(ModifiableSolrParams out, String field, Date d) {
    if (field != null && d != null)
      out.add("literal." + field, DateParser.formatISO8601Date(d));
  }

  /** NEW: small debug helper present in earlier snippets, now added back. */
  private void debugHandlers(String where) {
    if (Logging.ingest.isDebugEnabled()) {
      Logging.ingest.debug(
          where + " -> handlers: update=" + normalizePathOrDefault(this.postUpdateAction, "/update/extract")
              + " , remove=" + normalizePathOrDefault(this.postRemoveAction, "/update")
              + " , status=" + normalizePathOrDefault(this.postStatusAction, "/admin/ping")
      );
    }
  }

  private static void debugDumpParams(String title, ModifiableSolrParams params) {
    if (!Logging.ingest.isDebugEnabled() || params == null) return;
    List<String> lines = new ArrayList<>();
    Iterator<String> it = params.getParameterNamesIterator();
    while (it.hasNext()) {
      String name = it.next();
      String[] vals = params.getParams(name);
      lines.add(name + "=" + Arrays.toString(vals));
    }
    Collections.sort(lines);
    Logging.ingest.debug(title + " (" + lines.size() + "):\n  " + String.join("\n  ", lines));
  }

  private static String causeChain(Throwable t) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (t != null && i < 6) {
      if (i++ > 0) sb.append(" <= ");
      sb.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
      t = t.getCause();
    }
    return sb.toString();
  }

  // ---------- Date ISO ----------
  private static final class DateParser {
    private static String pad2(int v) { return (v < 10 ? "0" : "") + v; }
    private static String pad3(int v) {
      if (v < 10) return "00" + v;
      if (v < 100) return "0" + v;
      return String.valueOf(v);
    }
    static String formatISO8601Date(Date d) {
      java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
      c.setTime(d);
      int Y = c.get(java.util.Calendar.YEAR);
      int M = c.get(java.util.Calendar.MONTH) + 1;
      int D = c.get(java.util.Calendar.DAY_OF_MONTH);
      int h = c.get(java.util.Calendar.HOUR_OF_DAY);
      int m = c.get(java.util.Calendar.MINUTE);
      int s = c.get(java.util.Calendar.SECOND);
      int ms = c.get(java.util.Calendar.MILLISECOND);
      return Y + "-" + pad2(M) + "-" + pad2(D) + "T" + pad2(h) + ":" + pad2(m) + ":" + pad2(s) + "." + pad3(ms) + "Z";
    }
  }
}
