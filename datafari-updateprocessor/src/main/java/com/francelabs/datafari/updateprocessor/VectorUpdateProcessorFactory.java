package com.francelabs.datafari.updateprocessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

/**
 * A custom {@link UpdateRequestProcessorFactory} that initializes a {@link CloudHttp2SolrClient}
 * and creates instances of {@link VectorUpdateProcessor}.
 * <p>
 * This factory allows processing updates that interact with a remote SolrCloud collection
 * for vector data handling (e.g., embedding storage or synchronization).
 */
public class VectorUpdateProcessorFactory extends UpdateRequestProcessorFactory {

  private static final Logger LOGGER = LogManager.getLogger(VectorUpdateProcessorFactory.class);

  private static final String DEFAULT_COLLECTION = "VectorMain";
  private static final String DEFAULT_HOST = "localhost:2181"; // ZooKeeper host (not an HTTP endpoint)
  private static final String COLLECTION = "collection";
  private static final String HOST = "host";

  private SolrParams params = null;
  private CloudHttp2SolrClient client;

  @Override
  public void init(final NamedList args) {
    if (args != null) {
      params = args.toSolrParams();
    }
  }

  @Override
  public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse rsp, final UpdateRequestProcessor next) {
    if (params.getBool("enabled", false) && tryInitClient()) {
      return new VectorUpdateProcessor(client, params, next);
    } else {
      // No-op processor when disabled
      return new UpdateRequestProcessor(next) {};
    }
  }

  /**
   * Initializes the Solr client if necessary using Solr 10's {@link CloudHttp2SolrClient}.
   * 
   * @return {@code true} if the client is successfully initialized, {@code false} otherwise.
   */
private boolean tryInitClient() {
  if (client == null) {
    try {
      // ZK host list (e.g. "localhost:2181", or "zk1:2181", "zk2:2181"...)
      final String zkHost = getParam(HOST, DEFAULT_HOST);

      client = new CloudHttp2SolrClient.Builder(
          new ArrayList<>(Collections.singletonList(zkHost)),
          Optional.empty()       
      ).build();

      final String coll = getParam(COLLECTION, DEFAULT_COLLECTION);
      client.request(new SolrPing(), coll);

      return true;
    } catch (final Exception e) {
      LOGGER.warn("{} Error initializing Solr client for vector processing. The processor will be disabled.",
          e.getMessage());
      client = null;
      return false;
    }
  }
  return true;
}
  /**
   * Retrieves a parameter value or returns the default if not defined.
   *
   * @param paramName The parameter name
   * @param def The default value to return if the parameter is missing
   * @return The resolved value, or the default if undefined
   */
  private String getParam(final String paramName, final String def) {
    String result = params.get(paramName);
    return (result != null && !result.isEmpty()) ? result : def;
  }
}
