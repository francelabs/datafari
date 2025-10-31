package org.apache.manifoldcf.agents.output.solr;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;

public class ModifiedCloudHttp2SolrClient extends CloudHttp2SolrClient {

  public static class Builder {
    private final List<String> zkHosts;
    private final Optional<String> chroot;
    private String defaultCollection;

    public Builder(List<String> zkHosts, Optional<String> chroot) {
      this.zkHosts = Objects.requireNonNull(zkHosts);
      this.chroot = Objects.requireNonNull(chroot);
    }

    public Builder withDefaultCollection(String coll) {
      this.defaultCollection = coll; return this;
    }

    public ModifiedCloudHttp2SolrClient build() {
      CloudHttp2SolrClient.Builder b = new CloudHttp2SolrClient.Builder(zkHosts, chroot);
      ModifiedCloudHttp2SolrClient client = new ModifiedCloudHttp2SolrClient(b);
      if (defaultCollection != null) client.setDefaultCollection(defaultCollection);
      return client;
    }
  }

  private ModifiedCloudHttp2SolrClient(CloudHttp2SolrClient.Builder builder) {
    super(builder);
  }

  @Override
  protected boolean wasCommError(Throwable t) { return false; }

  @Override
  public void close() throws IOException { super.close(); }
}
