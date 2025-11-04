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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;

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

  // ---------- Constructeurs principaux (actuels) ----------

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

  // ---------- Constructeurs de compatibilité attendus par SolrConnector ----------

  // STANDALONE (ancienne signature sans protocolForSSL/keystoreForSSL)
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

  // CLOUD (ancienne signature avec protocolForSSL/keystoreForSSL en fin)
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
    // Dans CloudHttp2SolrClient, le SSL côté client est géré par le client cloud lui-même si nécessaire.
    // On ignore protocolForSSL/keystoreForSSL ici et on délègue au ctor principal.
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

  // ---------- Initialisations clients ----------


private void initCloud(List<String> zookeeperHosts, String znodePath, String collection) {
  try {
    final Optional<String> chroot =
        (znodePath != null && !znodePath.isEmpty()) ? Optional.of(znodePath) : Optional.empty();

    if (Logging.ingest.isDebugEnabled()) {
      Logging.ingest.debug("[Cloud init] zkHosts=" + zookeeperHosts
          + " chroot=" + chroot.orElse("(none)")
          + " collection=" + collection);
    }

    // Construction du client Cloud "propre" (sans solrUrls)
    org.apache.solr.client.solrj.impl.CloudHttp2SolrClient.Builder b =
        new org.apache.solr.client.solrj.impl.CloudHttp2SolrClient.Builder(zookeeperHosts, chroot);

    org.apache.solr.client.solrj.impl.CloudHttp2SolrClient client = b.build();

    // ✅ Ici on définit la collection par défaut sur le client (pas sur le builder)
    if (collection != null && !collection.isEmpty()) {
      client.setDefaultCollection(collection);
    }

    this.solrServer = client;

    if (Logging.ingest.isDebugEnabled()) {
      Logging.ingest.debug("[Cloud init] CloudHttp2SolrClient initialisé sans solrUrl (OK)");
    }
  } catch (Exception e) {
    throw new RuntimeException("Failed to initialize Cloud client", e);
  }
}
private static List<String> sanitizeZkHosts(List<String> hosts) {
  if (hosts == null) return Collections.emptyList();
  final List<String> out = new ArrayList<>(hosts.size());
  for (String h : hosts) {
    if (h == null) continue;
    h = h.trim();
    if (h.isEmpty()) continue;
    // CloudHttp2SolrClient attend "host:port" (sans schéma)
    h = stripHttpScheme(h);
    out.add(h);
  }
  return out;
}

private static String stripHttpScheme(String s) {
  if (s.startsWith("http://")) return s.substring("http://".length());
  if (s.startsWith("https://")) return s.substring("https://".length());
  return s;
}

  private void initStandalone(String protocol, String server, int port, String webapp, String core,
                              int connectionTimeout, int socketTimeout, SSLContext sslContextOrNull) {
    try {
      String location = "";
      if (webapp != null && !webapp.isEmpty()) location += "/" + webapp;
      if (core != null && !core.isEmpty())     location += "/" + core;
      final String baseUrl = protocol + "://" + server + ":" + port + location;

      ModifiedHttp2SolrClient.Builder b = new ModifiedHttp2SolrClient.Builder(baseUrl)
          .connectionTimeout(connectionTimeout)
          .idleTimeout(socketTimeout);

      if (sslContextOrNull != null) {
        b.withSSLContext(sslContextOrNull);
      }

      this.solrServer = b.build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize standalone client", e);
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

  // ---------- Méthodes principales ----------

  public boolean indexPost(final String documentURI, final RepositoryDocument document,
                           final Map<String, List<String>> arguments, final String authorityNameString,
                           final IOutputAddActivity activities)
      throws SolrServerException, IOException {

    final String path = normalizePathOrDefault(this.postUpdateAction, "/update/extract");
    if (Logging.ingest.isDebugEnabled())
      Logging.ingest.debug("Solr index -> handler: " + path + " , id=" + documentURI);

    final ContentStreamUpdateRequest req = new ContentStreamUpdateRequest(path);
    final ModifiableSolrParams out = new ModifiableSolrParams();

    // ID
    out.add(LITERAL + idAttributeName, documentURI);

    // Méta standard
    if (originalSizeAttributeName != null) {
      final Long size = document.getOriginalSize();
      if (size != null) out.add(LITERAL + originalSizeAttributeName, size.toString());
    }
    addIfDateNotNull(out, modifiedDateAttributeName, document.getModifiedDate());
    addIfDateNotNull(out, createdDateAttributeName,  document.getCreatedDate());
    addIfDateNotNull(out, indexedDateAttributeName,  document.getIndexingDate());

    if (fileNameAttributeName != null) {
      final String fn = document.getFileName();
      if (fn != null && !fn.isBlank()) out.add(LITERAL + fileNameAttributeName, fn);
    }
    if (mimeTypeAttributeName != null) {
      final String mt = document.getMimeType();
      if (mt != null && !mt.isBlank()) out.add(LITERAL + mimeTypeAttributeName, mt);
    }

    // Toutes les métadonnées MCF -> literal.<safeName>
    buildSolrParamsFromMetadata(document, out);

    // Champs supplémentaires via arguments
    if (arguments != null) {
      for (Map.Entry<String, List<String>> e : arguments.entrySet()) {
        final String name = e.getKey();
        final List<String> values = e.getValue();
        if (name != null && values != null) {
          for (String v : values) {
            if (v != null) out.add(name, v);
          }
        }
      }
    }
        // --- ACLs : collecte, qualification, validation types supportés, puis émission en literal.*
    final Map<String,String[]> aclsMap     = new HashMap<>();
    final Map<String,String[]> denyAclsMap = new HashMap<>();

    final Iterator<String> aclTypes = document.securityTypesIterator();
    while (aclTypes.hasNext()) {
      final String aclType = aclTypes.next();

      final String[] allow = convertACL(document.getSecurityACL(aclType),      authorityNameString, activities);
      final String[] deny  = convertACL(document.getSecurityDenyACL(aclType),  authorityNameString, activities);

      aclsMap.put(aclType, allow);
      denyAclsMap.put(aclType, deny);

      if (!isKnownAclType(aclType)) {
        // même logique que l’ancien code : on rejette si type non géré
        activities.recordActivity(
            null,
            SolrConnector.INGEST_ACTIVITY,
            null,
            documentURI,
            activities.UNKNOWN_SECURITY,
            "Solr connector rejected document that has security info which Solr does not recognize: '" + aclType + "'"
        );
        if (Logging.ingest.isDebugEnabled()) {
          Logging.ingest.debug("Rejecting doc due to unknown ACL type: " + aclType + " for " + documentURI);
        }
        return false;
      }
    }

    // Ecriture des ACLs en literal.allow_token_* / literal.deny_token_* etc.
    for (Map.Entry<String,String[]> e : aclsMap.entrySet()) {
      final String aclType = e.getKey();
      writeACLs(out, aclType, e.getValue(), denyAclsMap.get(aclType));
    }
    req.setParams(out);

    // Binaire éventuel
    final InputStream bin = document.getBinaryStream();
    if (bin != null) {
      final String contentType = safeContentType(document.getMimeType());
      final String contentName = (document.getFileName() != null ? document.getFileName() : documentURI);
      byte[] payload = toByteArray(bin, document.getBinaryLength());
      req.addContentStream(new ContentStreamBase.ByteArrayStream(payload, contentType, contentName));
    }
    // Dump complet des params juste avant l’envoi
    debugDumpParams("Final Solr params before request", out);

    solrServer.request(req);
    return true;
  }

  public void deletePost(final String documentURI, final IOutputRemoveActivity activities)
      throws SolrServerException, IOException {
    final String path = normalizePathOrDefault(this.postRemoveAction, "/update");
    final UpdateRequest req = new UpdateRequest();
    req.setPath(path);
    req.deleteById(documentURI);
    solrServer.request(req);
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

  // ---------- Métadonnées additionnelles ----------

  private void buildSolrParamsFromMetadata(final RepositoryDocument document, final ModifiableSolrParams out)
      throws IOException {
    int added = 0;
    final Iterator<String> iter = document.getFields();
if (!iter.hasNext()) {
    if (Logging.ingest.isDebugEnabled())
      Logging.ingest.debug("⚠️  RepositoryDocument.getFields() returned no metadata fields.");
  }
if (Logging.ingest.isDebugEnabled())
Logging.ingest.debug("debug entier olivier");
    while (iter.hasNext()) {
      final String originalFieldName = iter.next();
      final String fieldName = makeSafeLuceneField(originalFieldName);
      if (Logging.ingest.isDebugEnabled())
        Logging.ingest.debug("Solr metadata: '" + originalFieldName + "' → '" + fieldName + "'");
      applySingleMapping(document, originalFieldName, out, fieldName);
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

  // ---------- Compat: méthodes attendues par SolrConnector ----------

  /** Alias rétrocompat pour anciens usages. */
  public void shutdown() {
    try {
      close();
    } catch (Exception e) {
      // ignore
    }
  }

  /** Fermeture client. */
  public void close() {
    if (solrServer != null) {
      try { solrServer.close(); } catch (Exception ignore) {}
    }
  }

  /** Filtrage MIME rétrocompat (utilisé par SolrConnector). */
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
    /** Convertit une ACL non qualifiée en ACL qualifiée via l’autorité. */
  protected static String[] convertACL(final String[] acl,
                                       final String authorityNameString,
                                       final IOutputAddActivity activities) {
    if (acl == null) return new String[0];
    final String[] out = new String[acl.length];
    for (int i = 0; i < acl.length; i++) {
      try {
        out[i] = activities.qualifyAccessToken(authorityNameString, acl[i]);
      } catch (Exception e) {
        // On reste robuste : on remonte une RuntimeException avec contexte
        throw new RuntimeException("ACL qualification failed for token '" + acl[i] + "': " + e.getMessage(), e);
      }
    }
    return out;
  }

  /** Ecrit les ACL en paramètres literal.* pour un handler extract-like. */
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
  /**
   * Écrit les ACL dans un SolrInputDocument (mode non-extract).
   * Exemple : allow_token_document, deny_token_share, etc.
   */
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

    if (org.apache.manifoldcf.agents.system.Logging.ingest.isDebugEnabled()) {
      org.apache.manifoldcf.agents.system.Logging.ingest.debug(
          "ACLs (doc mode) → " + allowField + "=" + java.util.Arrays.toString(acl)
          + " ; " + denyField + "=" + java.util.Arrays.toString(denyAcl));
    }
  }

  /** Renvoie true si le type d'ACL est géré côté Solr (document/share/parent*). */
  private static boolean isKnownAclType(String t) {
    return RepositoryDocument.SECURITY_TYPE_DOCUMENT.equals(t)
        || RepositoryDocument.SECURITY_TYPE_SHARE.equals(t)
        || (t != null && t.startsWith(RepositoryDocument.SECURITY_TYPE_PARENT));
  }

  // ---------- Helpers ----------

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
