package org.apache.manifoldcf.agents.output.solr;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import javax.net.ssl.SSLContext;

import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.client.solrj.util.AsyncListener;
import org.apache.solr.client.solrj.util.Cancellable;

/** Wrapper autour de Http2SolrClient, typé SolrClient, avec un Builder minimal. */
public class ModifiedHttp2SolrClient extends SolrClient implements Closeable {

  private final Http2SolrClient delegate;

  private ModifiedHttp2SolrClient(Http2SolrClient delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate Http2SolrClient must not be null");
  }

  public Cancellable asyncRequest(
      SolrRequest<?> request,
      String collection,
      AsyncListener<NamedList<Object>> listener) {
    try {
      NamedList<Object> rsp = delegate.request(request, collection);
      listener.onSuccess(rsp);
    } catch (Exception e) {
      listener.onFailure(e);
    }
    return () -> {};
  }

  public void setParser(ResponseParser parser) { delegate.setParser(parser); }
  public void setRequestWriter(RequestWriter writer) { delegate.setRequestWriter(writer); }
  public void setQueryParams(Set<String> params) { delegate.setQueryParams(params); }

  @Override
  public NamedList<Object> request(SolrRequest<?> request, String collection)
      throws SolrServerException, IOException {
    return delegate.request(request, collection);
  }

  @Override
  public void close() throws IOException { delegate.close(); }

  /** Builder compatible 10.x (timeouts int) + hook SSL facultatif. */
  public static class Builder {
    private String baseUrl;
    private Integer connectTimeoutMs;
    private Integer idleTimeoutMs;
    private SSLContext sslContext; // optionnel

    public Builder() {}
    public Builder(String baseUrl) { this.baseUrl = baseUrl; }

    public Builder withBaseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
    public Builder connectionTimeout(int ms) { this.connectTimeoutMs = ms; return this; }
    public Builder idleTimeout(int ms) { this.idleTimeoutMs = ms; return this; }

    /** Optionnel : fournir un SSLContext pour https. */
    public Builder withSSLContext(SSLContext ctx) { this.sslContext = ctx; return this; }

    public ModifiedHttp2SolrClient build() {
      final Http2SolrClient.Builder b = (baseUrl != null && !baseUrl.isEmpty())
          ? new Http2SolrClient.Builder(baseUrl)
          : new Http2SolrClient.Builder();

      if (connectTimeoutMs != null) b.connectionTimeout(connectTimeoutMs.intValue());
      if (idleTimeoutMs != null) b.idleTimeout(idleTimeoutMs.intValue());

      // Certaines versions exposent b.sslContext(SSLContext). Si absente, ce call est no-op via réflexion.
      if (sslContext != null) {
        try {
          Http2SolrClient.Builder.class
              .getMethod("sslContext", SSLContext.class)
              .invoke(b, sslContext);
        } catch (Throwable ignore) {
          // No-op si la méthode n'existe pas dans ta version.
        }
      }

      return new ModifiedHttp2SolrClient(b.build());
    }
  }
}
