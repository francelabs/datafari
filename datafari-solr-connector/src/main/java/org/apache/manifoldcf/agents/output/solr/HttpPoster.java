package org.apache.manifoldcf.agents.output.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputRemoveActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.SolrInputDocument;

public class HttpPoster {

  private SolrClient solrServer;

  /* --------- CTORS COMPAT --------- */

  // Cloud (simple)
  public HttpPoster(final List<String> zookeeperHosts, final String znodePath, final String collection,
                    final int connectionTimeout, final int socketTimeout) {
    try {
      final Optional<String> chroot = (znodePath != null && !znodePath.isEmpty()) ? Optional.of(znodePath) : Optional.empty();
      final ModifiedCloudHttp2SolrClient client =
          new ModifiedCloudHttp2SolrClient.Builder(zookeeperHosts, chroot)
              .withDefaultCollection(collection)
              .build();
      this.solrServer = client;
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize Cloud client", e);
    }
  }

  // Standalone (simple)
  public HttpPoster(final String protocol, final String server, final int port, final String webapp,
                    final String core, final String user, final int connectionTimeout, final int socketTimeout) {
    try {
      String location = "";
      if (webapp != null && !webapp.isEmpty()) location += "/" + webapp;
      if (core != null && !core.isEmpty())     location += "/" + core;
      final String baseUrl = protocol + "://" + server + ":" + port + location;

      ModifiedHttp2SolrClient.Builder b = new ModifiedHttp2SolrClient.Builder(baseUrl)
          .connectionTimeout(connectionTimeout)
          .idleTimeout(socketTimeout);
      if (user != null) {
        // mot de passe inconnu dans ce ctor “court”, on ne configure pas l’auth
      }
      this.solrServer = b.build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize standalone client", e);
    }
  }

  // **** CTOR long historique (compat) : accepte tout, route vers les 2 CTOR précédents ****
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
    // on crée un client standalone minimal ; les autres paramètres sont ignorés au runtime par ce shim
    this(protocol, server, port, webapp, core, userID, connectionTimeout, socketTimeout);
  }

  // **** CTOR Cloud long historique (compat) ****
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
    this(zookeeperHosts, znodePath, collection, connectionTimeout, socketTimeout);
  }

  /* --------- Méthodes attendues par SolrConnector --------- */

  public void shutdown() { close(); }

  public void close() {
    if (solrServer != null) {
      try { solrServer.close(); } catch (IOException ignore) {}
    }
  }

  public void checkPost() throws SolrServerException, IOException {
    // ping standard (chemin par défaut du handler)
    final SolrPingResponse resp = new SolrPing().process(solrServer);
  }

  public boolean indexPost(final String documentURI, final RepositoryDocument document,
                           final Map<String, List<String>> arguments, final String authorityNameString,
                           final IOutputAddActivity activities)
      throws SolrServerException, IOException {

    // Implémentation simple: on crée un SolrInputDocument avec l’ID + contenu si présent
    final SolrInputDocument doc = new SolrInputDocument();
    doc.addField("id", documentURI);

    // Copie brute des métadonnées (chaînes) si dispo
    for (var it = document.getFields(); it.hasNext(); ) {
      String field = it.next();
      String[] values = document.getFieldAsStrings(field);
      if (values != null) {
        for (String v : values) {
          doc.addField(field, v);
        }
      }
    }

    final UpdateRequest req = new UpdateRequest();
    req.add(doc);
    req.process(solrServer);
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

  // Fonction utilitaire attendue (signature)
  public static boolean checkMimeTypeIndexable(final String mimeType, final boolean useExtractUpdateHandler,
                                               final Set<String> includedMimeTypes, final Set<String> excludedMimeTypes) {
    if (mimeType == null) return false;
    final String m = mimeType.toLowerCase();
    if (includedMimeTypes != null && !includedMimeTypes.contains(m)) return false;
    if (excludedMimeTypes != null &&  excludedMimeTypes.contains(m)) return false;
    return true;
  }
}
