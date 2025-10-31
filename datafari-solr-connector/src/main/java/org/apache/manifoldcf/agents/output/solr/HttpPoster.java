package org.apache.manifoldcf.agents.output.solr;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.net.ssl.SSLContext;

import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputRemoveActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;
import org.apache.manifoldcf.core.common.DateParser;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;

/**
 * Classe HttpPoster unifiée : gère Solr Standalone et Cloud.
 * Compatible Solr 9+/10 et Java 21.
 */
public class HttpPoster {

  private SolrClient solrServer;

  // Champs facultatifs pour métadonnées
  private String idAttributeName = "id";
  private String originalSizeAttributeName;
  private String modifiedDateAttributeName;
  private String createdDateAttributeName;
  private String indexedDateAttributeName;
  private String fileNameAttributeName;
  private String mimeTypeAttributeName;
  private String allowAttributeName;
  private String denyAttributeName;

  /* =======================
   *       CONSTRUCTEURS
   * ======================= */

  /** --- MODE CLOUD --- (long historique) */
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

    this.allowAttributeName = allowAttributeName;
    this.denyAttributeName = denyAttributeName;
    if (idAttributeName != null && !idAttributeName.isEmpty()) this.idAttributeName = idAttributeName;
    this.originalSizeAttributeName = originalSizeAttributeName;
    this.modifiedDateAttributeName = modifiedDateAttributeName;
    this.createdDateAttributeName = createdDateAttributeName;
    this.indexedDateAttributeName = indexedDateAttributeName;
    this.fileNameAttributeName = fileNameAttributeName;
    this.mimeTypeAttributeName = mimeTypeAttributeName;

    initCloud(zookeeperHosts, znodePath, collection);
  }

  /** --- MODE CLOUD --- (simple) */
  public HttpPoster(final List<String> zookeeperHosts, final String znodePath, final String collection,
                    final int connectionTimeout, final int socketTimeout) {
    initCloud(zookeeperHosts, znodePath, collection);
  }

  /** --- MODE STANDALONE --- (long historique) */
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
    this.denyAttributeName = denyAttributeName;
    if (idAttributeName != null && !idAttributeName.isEmpty()) this.idAttributeName = idAttributeName;
    this.originalSizeAttributeName = originalSizeAttributeName;
    this.modifiedDateAttributeName = modifiedDateAttributeName;
    this.createdDateAttributeName = createdDateAttributeName;
    this.indexedDateAttributeName = indexedDateAttributeName;
    this.fileNameAttributeName = fileNameAttributeName;
    this.mimeTypeAttributeName = mimeTypeAttributeName;

    SSLContext ctx = null;
    if ("https".equalsIgnoreCase(protocol)) {
      ctx = resolveTrustAllSSLContext(); // toujours “trust all”
    }
    initStandalone(protocol, server, port, webapp, core, connectionTimeout, socketTimeout, ctx);
  }

  /** --- MODE STANDALONE --- (simple) */
  public HttpPoster(final String protocol, final String server, final int port, final String webapp,
                    final String core, final String user, final int connectionTimeout, final int socketTimeout) {
    initStandalone(protocol, server, port, webapp, core, connectionTimeout, socketTimeout, null);
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
      if (core != null && !core.isEmpty()) location += "/" + core;
      final String baseUrl = protocol + "://" + server + ":" + port + location;

      ModifiedHttp2SolrClient.Builder b = new ModifiedHttp2SolrClient.Builder(baseUrl)
          .connectionTimeout(connectionTimeout)
          .idleTimeout(socketTimeout);

      if (sslContextOrNull != null) b.withSSLContext(sslContextOrNull);

      this.solrServer = b.build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize standalone client", e);
    }
  }

  /** SSLContext “trust all” — utile pour environnements internes */
  private static SSLContext resolveTrustAllSSLContext() {
    try {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, new javax.net.ssl.TrustManager[]{
          new javax.net.ssl.X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
          }
      }, new java.security.SecureRandom());
      return ctx;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create trust-all SSLContext", e);
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

  public void checkPost() throws SolrServerException, IOException {
    new SolrPing().process(solrServer);
  }

  /**
   * Indexation (équivalent “/update/extract-like”) — handler et paramètres configurés via la UI.
   */
  public boolean indexPost(final String documentURI, final RepositoryDocument document,
                           final Map<String, List<String>> arguments, final String authorityNameString,
                           final IOutputAddActivity activities)
      throws SolrServerException, IOException {

    if (documentURI == null || documentURI.isBlank()) {
      throw new IllegalArgumentException("documentURI (id) est vide côté connecteur");
    }

    // Construction de la requête “extract-like”
    final ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("");

    final ModifiableSolrParams out = new ModifiableSolrParams();

    // ID obligatoire
    out.add("literal." + idAttributeName, documentURI);

    // Métadonnées (comme ancien code)
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

    // Champs supplémentaires depuis MCF
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

    solrServer.request(req);
    return true;
  }

  public void deletePost(final String documentURI, final IOutputRemoveActivity activities)
      throws SolrServerException, IOException {
    final UpdateRequest req = new UpdateRequest();
    req.deleteById(documentURI);
    req.process(solrServer);
  }

  public void commitPost() throws SolrServerException, IOException {
    final UpdateRequest req = new UpdateRequest();
    req.setParam("commit", "true");
    req.process(solrServer);
  }

  public static boolean checkMimeTypeIndexable(final String mimeType, final boolean useExtractUpdateHandler,
                                               final Set<String> includedMimeTypes, final Set<String> excludedMimeTypes) {
    if (mimeType == null) return false;
    final String m = mimeType.toLowerCase();
    if (includedMimeTypes != null && !includedMimeTypes.contains(m)) return false;
    if (excludedMimeTypes != null && excludedMimeTypes.contains(m)) return false;
    return true;
  }
}
