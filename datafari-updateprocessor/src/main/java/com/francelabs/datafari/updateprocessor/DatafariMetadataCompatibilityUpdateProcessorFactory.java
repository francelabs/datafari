package com.francelabs.datafari.updateprocessor;

import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

/**
 * Factory for {@link DatafariMetadataCompatibilityUpdateProcessor}.
 *
 * <p>This factory creates a metadata compatibility processor for each update
 * request processor chain execution.
 *
 * <p>The processor should be configured at the very beginning of the Datafari
 * update chain so that all subsequent processors receive a document whose
 * metadata is already normalized to Datafari canonical fields.
 *
 * <p>Example configuration:
 *
 * <pre>
 * &lt;updateRequestProcessorChain name="datafari"&gt;
 *   &lt;processor class="com.francelabs.datafari.updateprocessor.DatafariMetadataCompatibilityUpdateProcessorFactory"/&gt;
 *
 *   &lt;processor class="solr.CloneFieldUpdateProcessorFactory"&gt;
 *     &lt;lst name="source"&gt;
 *       &lt;str name="fieldRegex"&gt;content&lt;/str&gt;
 *     &lt;/lst&gt;
 *     &lt;str name="dest"&gt;exactContent&lt;/str&gt;
 *   &lt;/processor&gt;
 *
 *   ...
 * &lt;/updateRequestProcessorChain&gt;
 * </pre>
 */
public class DatafariMetadataCompatibilityUpdateProcessorFactory extends UpdateRequestProcessorFactory {

  @Override
  public UpdateRequestProcessor getInstance(
      final SolrQueryRequest req,
      final SolrQueryResponse rsp,
      final UpdateRequestProcessor next) {

    return new DatafariMetadataCompatibilityUpdateProcessor(next);
  }
}