package com.francelabs.datafari.solraccessors;

import com.francelabs.datafari.config.CollectionPathConfig;
import com.francelabs.datafari.config.JobConfig;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Solr Accessor to update documents. Related to the <b>Destination Collection</b> of an Atomic Update Job.
 */
public class DocumentsUpdator extends AbstractDocuments {
  private static DocumentsUpdator thisInstance = null;
  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsUpdator.class);

  /**
   * Create a unique instance of this Accessor. Configure all necessary parameters to access Solr collection with the
   * given jobConfig. Creates and configure the Solr Client to access the target Solr collection.
   *
   * @param jobConfig the configuration object of the job associated with this Accessor.
   * @return
   * @throws IOException if an I/O exception occurs while configuring this instance (precisely the Solr Client).
   */
  public static DocumentsUpdator getInstance(JobConfig jobConfig) throws IOException {
    if (thisInstance == null){
      thisInstance = new DocumentsUpdator();
    }
    thisInstance.setJobConfig(jobConfig);
    return thisInstance;
  }

  @Override
  protected CollectionPathConfig getCollectionPath() {
    return jobConfig.getDestination();
  }

  /**
   * Update the given Solr documents list with atomic update query. That is to say, only the fields given in the
   * {@link JobConfig} object (related to this module configuration file) will be updated according to the given
   * operation associated. (see {@link JobConfig for more information}
   *
   * @param solrDocuments the current list of Solr documents to be updated. This list more often comes from the
   * {@link DocumentsCollector}.
   * @return The Solr response for this update query.
   * @throws SolrServerException
   * @throws IOException
   */
  public UpdateResponse updateDocuments(List<SolrDocument> solrDocuments) throws SolrServerException, IOException {
    List<SolrInputDocument> docsToUpdate = new ArrayList<>();
    //Prepare query to Solr with all documents to update.
    for (SolrDocument doc : solrDocuments){
      //Do not update documents that have all fields null except ID field.
      if(doc.size() > 1) {
        docsToUpdate.add(createSolrDocToUpdate(doc));
      }
    }

    UpdateResponse updateResponse = null;
    if (!docsToUpdate.isEmpty()){
      UpdateRequest solrRequest = new UpdateRequest();
      // Do not reject all update batch for some version conflicts
      solrRequest.setParam("failOnVersionConflicts", "false");
      solrRequest.add(docsToUpdate);

      try {
        updateResponse = solrRequest.process(solrClient, solrCollection);

      } catch (Exception e) {
        LOGGER.error(jobConfig.getJobName() + " Job:", e);
        LOGGER.error(jobConfig.getJobName() + " Job: was sending these documents : number = " + docsToUpdate.size());
        for (SolrInputDocument doc :docsToUpdate){
          LOGGER.error(jobConfig.getJobName() + " Job: \t" + doc.toString());
        }
        LOGGER.error(jobConfig.getJobName() + " Job: Solr response: " + updateResponse);
        throw e;
      }

    }

    return updateResponse;
  }

  /**
   * Create the Solr document object used for the update query.
   * Apply field mapping if necessary (from {@link JobConfig}).
   * Use Optimistic Concurrency: the document must exist to be updated
   *
   * @param doc a Solr document resulting from a select query to Solr.
   * @return the object used for the update query
   */
  private SolrInputDocument createSolrDocToUpdate(SolrDocument doc) {
    // create the atomic document
    SolrInputDocument atomicDoc = new SolrInputDocument();
    atomicDoc.addField(CommonParams.ID, doc.getFieldValue(CommonParams.ID));

    // Retrieve fields to apply Atomic Update on and their operator
    for (Map.Entry<String, String> fieldConfig : jobConfig.getFieldsOperation().entrySet()) {
      String fieldName = fieldConfig.getKey();
      String modifierName = fieldConfig.getValue();
      Object fieldValue = doc.getFieldValue(fieldName);

      Map<String, Object> fieldModifier = new HashMap<>(1);
      fieldModifier.put(modifierName, fieldValue);

      //check for field mapping
      String finalFieldName = jobConfig.getFieldsMapping().get(fieldName);
      if (finalFieldName != null) {
        fieldName = finalFieldName;
      }
      atomicDoc.addField(fieldName, fieldModifier);
    }

    // Optimistic Concurrency: the document must exist to be updated
    atomicDoc.addField(CommonParams.VERSION_FIELD, 1);

    return atomicDoc;
  }
}
