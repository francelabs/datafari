package org.apache.manifoldcf.agents.output.solr;

import java.util.Objects;

import org.apache.solr.client.solrj.impl.Http2SolrClient;

/**
 * Version minimale compatible SolrJ 10 :
 * - timeouts en int ms (pas de TimeUnit)
 * - pas de requestTimeout
 */
public class ModifiedKrb5HttpClientBuilder {

  private Integer connectTimeoutMs;
  private Integer idleTimeoutMs;

  public ModifiedKrb5HttpClientBuilder withConnectTimeoutMs(final int ms) {
    this.connectTimeoutMs = ms;
    return this;
  }

  public ModifiedKrb5HttpClientBuilder withIdleTimeoutMs(final int ms) {
    this.idleTimeoutMs = ms;
    return this;
  }

  public Http2SolrClient.Builder getBuilder(final String baseUrl) {
    Objects.requireNonNull(baseUrl, "baseUrl must not be null");

    // Laisse la conf JAAS/Krb5 au runtime si nécessaire
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

    final Http2SolrClient.Builder b = new Http2SolrClient.Builder(baseUrl);
    if (connectTimeoutMs != null) b.connectionTimeout(connectTimeoutMs);
    if (idleTimeoutMs != null)    b.idleTimeout(idleTimeoutMs);
    return b;
  }
}
