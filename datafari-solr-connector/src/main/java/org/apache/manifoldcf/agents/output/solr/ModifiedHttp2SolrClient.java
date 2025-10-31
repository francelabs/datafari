package org.apache.manifoldcf.agents.output.solr;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.client.solrj.util.AsyncListener;
import org.apache.solr.client.solrj.util.Cancellable;
/**
 * Wrapper "Modified" autour de Http2SolrClient pour s'aligner sur SolrJ 10.x.
 * - Étend SolrClient (et non pas seulement Closeable) afin d'être typé comme un SolrClient.
 * - Délègue les appels à un Http2SolrClient interne.
 * - Fournit un Builder minimal compatible 10.x (timeouts en int).
 */
public class ModifiedHttp2SolrClient extends SolrClient implements Closeable {

  private final Http2SolrClient delegate;

  private ModifiedHttp2SolrClient(Http2SolrClient delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate Http2SolrClient must not be null");
  }

  public Cancellable asyncRequest(
    SolrRequest<?> request,
    String collection,
    AsyncListener<NamedList<Object>> listener) {

  // Si ton snapshot n’a pas de version async, on l’exécute en synchrone.
  try {
    NamedList<Object> rsp = delegate.request(request, collection);
    listener.onSuccess(rsp);
  } catch (Exception e) {
    listener.onFailure(e);
  }

  // Renvoie un Cancellable “neutre”
  return () -> {};
}

  /* =========================================================
   *           Pass-through utilitaires demandés
   * ========================================================= */

  public void setParser(ResponseParser parser) {
    delegate.setParser(parser);
  }

  public void setRequestWriter(RequestWriter writer) {
    delegate.setRequestWriter(writer);
  }

  public void setQueryParams(Set<String> params) {
    delegate.setQueryParams(params);
  }

  /* =========================================================
   *              Implémentation SolrClient
   * ========================================================= */

  @Override
  public NamedList<Object> request(SolrRequest<?> request, String collection)
      throws SolrServerException, IOException {
    return delegate.request(request, collection);
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  /* =========================================================
   *                      Builder
   * ========================================================= */

  public static class Builder {
    private String baseUrl;                 // optionnel
    private Integer connectTimeoutMs;       // en millisecondes (int requis par SolrJ 10)
    private Integer idleTimeoutMs;          // en millisecondes (int requis par SolrJ 10)
    // NOTE: certaines variantes de 10.x ne proposent pas requestTimeout; on ne l'utilise pas ici.

    public Builder() {
    }

    public Builder(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    /** Définir l'URL de base du client (si non passé au constructeur). */
    public Builder withBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    /** Timeout de connexion en millisecondes (signature SolrJ 10: int). */
    public Builder connectionTimeout(int ms) {
      this.connectTimeoutMs = ms;
      return this;
    }

    /** Idle timeout en millisecondes (signature SolrJ 10: int). */
    public Builder idleTimeout(int ms) {
      this.idleTimeoutMs = ms;
      return this;
    }

    /** Construit un ModifiedHttp2SolrClient. */
    public ModifiedHttp2SolrClient build() {
      final Http2SolrClient.Builder b = (baseUrl != null && !baseUrl.isEmpty())
          ? new Http2SolrClient.Builder(baseUrl)
          : new Http2SolrClient.Builder();

      if (connectTimeoutMs != null) {
        b.connectionTimeout(connectTimeoutMs.intValue());
      }
      if (idleTimeoutMs != null) {
        b.idleTimeout(idleTimeoutMs.intValue());
      }

      // IMPORTANT (SolrJ 10): ne PAS appeler basicAuthCredentials/followRedirects,
      // ni requestTimeout(Duration|int) si la méthode n'existe pas dans ton snapshot.

      return new ModifiedHttp2SolrClient(b.build());
    }
  }
}
