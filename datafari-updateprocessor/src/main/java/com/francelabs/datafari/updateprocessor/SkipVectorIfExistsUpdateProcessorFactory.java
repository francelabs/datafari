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

/**
 * A custom UpdateRequestProcessorFactory that skips vector updates
 * if the document already exists in a target Solr collection.
 * <p>
 * This class initializes a {@link CloudHttp2SolrClient} to communicate with
 * a remote SolrCloud collection and performs a lightweight check before
 * indexing vector data.
 */
public class SkipVectorIfExistsUpdateProcessorFactory extends UpdateRequestProcessorFactory {

  private static final Logger LOGGER = LogManager.getLogger(SkipVectorIfExistsUpdateProcessorFactory.class);

  private static final String DEFAULT_COLLECTION = "VectorMain";
  private static final String DEFAULT_HOST = "localhost:2181"; // Zookeeper host (no HTTP prefix)
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
      return new SkipVectorIfExistsUpdateProcessor(client, params, next);
    } else {
      // No-op processor when disabled
      return new UpdateRequestProcessor(next) {};
    }
  }

  /**
   * Initializes the Solr client if it has not already been created.
   * 
   * @return {@code true} if the client is initialized successfully; {@code false} otherwise
   */
  private boolean tryInitClient() {
    if (client == null) {
      try {
        // Solr 10: CloudHttp2SolrClient built using a list of ZooKeeper hosts
        client = new CloudHttp2SolrClient.Builder(
            new ArrayList<>(Collections.singletonList(getParam(HOST, DEFAULT_HOST)))
        ).build();

        // Explicitly ping the target collection (no defaultCollection in builder)
        final String coll = getParam(COLLECTION, DEFAULT_COLLECTION);
        final SolrPing ping = new SolrPing();
        client.request(ping, coll);

        return true;
      } catch (final Exception e) {
        LOGGER.warn("{} Error initializing Solr client for vector existence check. The processor will be disabled.", e.getMessage());
        client = null;
        return false;
      }
    }
    return true;
  }

  /**
   * Retrieves a parameter from the Solr configuration, returning a default value if undefined.
   *
   * @param paramName the name of the parameter
   * @param def the default value to use if not present
   * @return the parameter value or the default
   */
  private String getParam(final String paramName, final String def) {
    String result = params.get(paramName);
    return (result != null && !result.isEmpty()) ? result : def;
  }
}
