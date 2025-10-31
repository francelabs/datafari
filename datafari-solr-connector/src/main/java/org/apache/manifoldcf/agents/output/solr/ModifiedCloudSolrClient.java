package org.apache.manifoldcf.agents.output.solr;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.util.NamedList;

public class ModifiedCloudSolrClient implements Closeable {

  private final CloudSolrClient delegate;

  public static class Builder {
    private List<String> zkHosts;
    private String defaultCollection;

    public Builder withZkAddress(String zk) {
      this.zkHosts = List.of(zk);
      return this;
    }

    public Builder withZkHosts(List<String> zks) {
      this.zkHosts = zks;
      return this;
    }

    public Builder withDefaultCollection(String coll) {
      this.defaultCollection = coll;
      return this;
    }

    public ModifiedCloudSolrClient build() {
      Objects.requireNonNull(zkHosts, "zkHosts must not be null/empty");
      CloudSolrClient.Builder b = new CloudSolrClient.Builder(zkHosts);
      CloudSolrClient c = b.build();
      if (defaultCollection != null) c.setDefaultCollection(defaultCollection);
      return new ModifiedCloudSolrClient(c);
    }
  }

  private ModifiedCloudSolrClient(CloudSolrClient delegate) {
    this.delegate = delegate;
  }

  public NamedList<Object> request(SolrRequest<?> request, String collection)
      throws SolrServerException, IOException {
    return delegate.request(request, collection);
  }

  public void setDefaultCollection(String coll) { delegate.setDefaultCollection(coll); }
  public String getDefaultCollection() { return delegate.getDefaultCollection(); }

  @Override
  public void close() throws IOException { delegate.close(); }
}
