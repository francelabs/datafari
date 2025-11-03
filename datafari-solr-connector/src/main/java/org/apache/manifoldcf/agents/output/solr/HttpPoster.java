package org.apache.manifoldcf.agents.output.solr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputRemoveActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;

public class HttpPoster {

  /* =======================
   *        ÉTAT
   * ======================= */

  private SolrClient solrServer;

  // Handlers (provenant de la UI). On garde le “slash” normalisé au moment de l’usage.
  private String postUpdateAction;
  private String postRemoveAction;
  private String postStatusAction;

  // Noms d’attributs (hérités de l’ancien code ManifoldCF/Datafari)
  private final String idAttributeName;
  private final String originalSizeAttributeName;
  private final String modifiedDateAttributeName;
  private final String createdDateAttributeName;
  private final String indexedDateAttributeName;
  private final String fileNameAttributeName;
  private final String mimeTypeAttributeName;
  private final String contentAttributeName;

  // Autres paramètres issus des anciens constructeurs (on stocke seulement ce qui est nécessaire ici)
  private final Long    maxDocumentLength;
  private final String  commitWithin;
  private final boolean useExtractUpdateHandler;
  private final Set<String> includedMimeTypes;
  private final Set<String> excludedMimeTypes;
  private final boolean allowCompression;

  /* =======================
   *       CONSTRUCTEURS
   * ======================= */

  /** Cloud (simple) */
  public HttpPoster(final List<String> zookeeperHosts,
                    final String znodePath,
                    final String collection,
                    final int socketTimeout, final int connectionTimeout) {

    // Pas de métadonnées dans ce constructeur “court”
    this.idAttributeName = "id";
    this.originalSizeAttributeName = null;
    this.modifiedDateAttributeName = null;
    this.createdDateAttributeName = null;
    this.indexedDateAttributeName = null;
    this.fileNameAttributeName = null;
    this.mimeTypeAttributeName = null;
    this.contentAttributeName = null;

    this.maxDocumentLength = null;
    this.commitWithin = null;
    this.useExtractUpdateHandler = true;
    this.includedMimeTypes = null;
    this.excludedMimeTypes = null;
    this.allowCompression = false;

    initCloud(zookeeperHosts, znodePath, collection);

    // Fallbacks défauts
    this.postUpdateAction = "/update/extract";
    this.postRemoveAction = "/update";
    this.postStatusAction = "/admin/ping";
    debugHandlers("Cloud(simple)-ctor");
  }

  /** Cloud (long historique, SANS protocol/keystore) */
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

    // Handlers issus de la UI
    this.postUpdateAction = updatePath;
    this.postRemoveAction = removePath;
    this.postStatusAction = statusPath;
    debugHandlers("Cloud(long)-ctor");
  }

  /** Cloud (long historique, AVEC protocol/keystore à la fin — ignorés pour Cloud) */
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
         originalSizeAttributeName, modifiedDateAttributeName, createdDateAttributeName, indexedDateAttributeName,
         fileNameAttributeName, mimeTypeAttributeName, contentAttributeName, maxDocumentLength, commitWithin,
         useExtractUpdateHandler, includedMimeTypes, excludedMimeTypes, allowCompression);
    // (Pour Cloud on ignore ssl ici; le client cloud se charge du transport)
  }

  /** Standalone (simple) */
  public HttpPoster(final String protocol, final String server, final int port, final String webapp,
                    final String core, final String user, final int connectionTimeout, final int socketTimeout) {

    // Pas de métadonnées dans ce constructeur “court”
    this.idAttributeName = "id";
    this.originalSizeAttributeName = null;
    this.modifiedDateAttributeName = null;
    this.createdDateAttributeName = null;
    this.indexedDateAttributeName = null;
    this.fileNameAttributeName = null;
    this.mimeTypeAttributeName = null;
    this.contentAttributeName = null;

    this.maxDocumentLength = null;
    this.commitWithin = null;
    this.useExtractUpdateHandler = true;
    this.includedMimeTypes = null;
    this.excludedMimeTypes = null;
    this.allowCompression = false;

    initStandalone(protocol, server, port, webapp, core, connectionTimeout, socketTimeout, null);

    // Fallbacks défauts
    this.postUpdateAction = "/update/extract";
    this.postRemoveAction = "/update";
    this.postStatusAction = "/admin/ping";
    debugHandlers("Std(simple)-ctor");
  }

  /** Standalone (long historique — SANS keystore/protocol) */
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

    initStandalone(protocol, server, port, webapp, core, connectionTimeout, socketTimeout, null);

    // Handlers issus de la UI
    this.postUpdateAction = updatePath;
    this.postRemoveAction = removePath;
    this.postStatusAction = statusPath;
    debugHandlers("Std(long)-ctor/no-ssl");
  }

  /** Standalone (long historique — AVEC protocol+keystore à la fin) */
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

    // SSLContext si https
    SSLContext ctx = null;
    if ("https".equalsIgnoreCase(protocol)) {
      ctx = resolveSSLContext(keystoreForSSL);
    }
    initStandalone(protocol, server, port, webapp, core, connectionTimeout, socketTimeout, ctx);

    // Handlers issus de la UI
    this.postUpdateAction = updatePath;
    this.postRemoveAction = removePath;
    this.postStatusAction = statusPath;
    debugHandlers("Std(long)-ctor/with-ssl");
  }

  /* =======================
   *     INITIALISATIONS
   * ======================= */

  private void initCloud(List<String> zookeeperHosts, String znodePath, String collection) {
    try {
      final Optional<String> chroot = (znodePath != null && !znodePath.isEmpty())
          ? Optional.of(znodePath) : Optional.empty();
      final ModifiedCloudHttp2SolrClient client =
          new ModifiedCloudHttp2SolrClient.Builder(zookeeperHosts, chroot)
              .withDefaultCollection(collection)
              .build();
      this.solrServer = client;
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

  /** SSL “trust all” par défaut (améliorable avec IKeystoreManager si besoin). */
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

  /* =======================
   *     MÉTHODES APPELÉES
   * ======================= */

  public void shutdown() { close(); }

  public void close() {
    if (solrServer != null) {
      try { solrServer.close(); } catch (IOException ignore) {}
    }
  }

  /** Check (ping) */
  public void checkPost() throws SolrServerException, IOException {
    final String path = normalizePathOrDefault(this.postStatusAction, "/admin/ping");
    if (Logging.ingest.isDebugEnabled())
      Logging.ingest.debug("Solr check -> handler: " + path);
    SolrPing ping = new SolrPing();
    ping.setPath(path);
    ping.process(solrServer);
  }

  /** Indexation (handler fourni par UI ; pas d’uprefix/chain forcé ici) */
  public boolean indexPost(final String documentURI, final RepositoryDocument document,
                           final Map<String, List<String>> arguments, final String authorityNameString,
                           final IOutputAddActivity activities)
      throws SolrServerException, IOException {

    if (documentURI == null || documentURI.isBlank()) {
      throw new IllegalArgumentException("documentURI (id) est vide côté connecteur");
    }

    final String path = normalizePathOrDefault(this.postUpdateAction, "/update/extract");
    if (Logging.ingest.isDebugEnabled())
      Logging.ingest.debug("Solr index -> handler: " + path + " , id=" + documentURI);

    final ContentStreamUpdateRequest req = new ContentStreamUpdateRequest(path);

    // Paramètres “literal.*” comme l’ancien code
    final ModifiableSolrParams out = new ModifiableSolrParams();
    out.add("literal." + effectiveIdField(), documentURI);

    if (originalSizeAttributeName != null) {
      final Long size = document.getOriginalSize();
      if (size != null) out.add("literal." + originalSizeAttributeName, size.toString());
    }
    if (modifiedDateAttributeName != null) {
      final Date d = document.getModifiedDate();
      if (d != null) out.add("literal." + modifiedDateAttributeName, DateParser.formatISO8601Date(d));
    }
    if (createdDateAttributeName != null) {
      final Date d = document.getCreatedDate();
      if (d != null) out.add("literal." + createdDateAttributeName, DateParser.formatISO8601Date(d));
    }
    if (indexedDateAttributeName != null) {
      final Date d = document.getIndexingDate();
      if (d != null) out.add("literal." + indexedDateAttributeName, DateParser.formatISO8601Date(d));
    }
    if (fileNameAttributeName != null) {
      final String fn = document.getFileName();
      if (fn != null && !fn.isBlank()) out.add("literal." + fileNameAttributeName, fn);
    }
    if (mimeTypeAttributeName != null) {
      final String mt = document.getMimeType();
      if (mt != null && !mt.isBlank()) out.add("literal." + mimeTypeAttributeName, mt);
    }

    // Champs additionnels MCF → pass-through
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

    req.setParams(out);

    // Corps binaire (si présent)
    final InputStream bin = document.getBinaryStream();
    if (bin != null) {
      final String contentType = safeContentType(document.getMimeType());
      final String contentName = (document.getFileName() != null ? document.getFileName() : documentURI);

      byte[] payload = toByteArray(bin, document.getBinaryLength());
      if (Logging.ingest.isDebugEnabled())
        Logging.ingest.debug("Solr index -> add content stream (" + contentType + "), bytes=" + payload.length);

      req.addContentStream(new ContentStreamBase.ByteArrayStream(payload, contentType, contentName));
    } else {
      // Pas de stream binaire → /update/extract acceptera quand même les literals,
      // mais certains handlers attendent un stream. À ajuster selon tes handlers custom.
      if (Logging.ingest.isDebugEnabled())
        Logging.ingest.debug("Solr index -> aucun stream binaire fourni (id=" + documentURI + ")");
    }

    solrServer.request(req);
    return true;
  }

  /** Suppression */
  public void deletePost(final String documentURI, final IOutputRemoveActivity activities)
      throws SolrServerException, IOException {

    final String path = normalizePathOrDefault(this.postRemoveAction, "/update");
    if (Logging.ingest.isDebugEnabled())
      Logging.ingest.debug("Solr delete -> handler: " + path + " , id=" + documentURI);

    final UpdateRequest req = new UpdateRequest();
    req.setPath(path);
    req.deleteById(documentURI);
    solrServer.request(req);
  }

  /** Commit */
  public void commitPost() throws SolrServerException, IOException {
    final String path = normalizePathOrDefault(this.postRemoveAction, "/update");
    if (Logging.ingest.isDebugEnabled())
      Logging.ingest.debug("Solr commit -> handler: " + path);

    final UpdateRequest req = new UpdateRequest();
    req.setPath(path);
    req.setParam("commit", "true");
    solrServer.request(req);
  }

  /** Filtrage simple mimeTypes (héritage) */
  public static boolean checkMimeTypeIndexable(final String mimeType, final boolean useExtractUpdateHandler,
                                               final Set<String> includedMimeTypes, final Set<String> excludedMimeTypes) {
    if (mimeType == null) return false;
    final String m = mimeType.toLowerCase();
    if (includedMimeTypes != null && !includedMimeTypes.contains(m)) return false;
    if (excludedMimeTypes != null &&  excludedMimeTypes.contains(m)) return false;
    return true;
  }

  /* =======================
   *      OUTILS PRIVÉS
   * ======================= */

  private String effectiveIdField() {
    return (this.idAttributeName != null && !this.idAttributeName.isBlank()) ? this.idAttributeName : "id";
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

  private void debugHandlers(String where) {
    if (Logging.ingest.isDebugEnabled()) {
      Logging.ingest.debug(
          where + " -> handlers: update=" + normalizePathOrDefault(this.postUpdateAction, "/update/extract")
          + " , remove=" + normalizePathOrDefault(this.postRemoveAction, "/update")
          + " , status=" + normalizePathOrDefault(this.postStatusAction, "/admin/ping")
      );
    }
  }

  /* =======================
   *  DATES (ISO 8601, idem ancien code)
   * ======================= */

  private static final class DateParser {
    private static String pad2(int v) { return (v < 10 ? "0" : "") + v; }
    private static String pad3(int v) {
      if (v < 10) return "00" + v;
      if (v < 100) return "0" + v;
      return String.valueOf(v);
    }
    static String formatISO8601Date(Date d) {
      // Simple formateur ISO 8601 UTC (YYYY-MM-DDThh:mm:ss.SSSZ) sans dépendances
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
