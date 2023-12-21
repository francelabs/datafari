package com.francelabs.datafari.solraccessors;

import com.francelabs.datafari.config.CollectionPathConfig;
import com.francelabs.datafari.config.JobConfig;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;
import java.util.*;

public class DocumentsUpdator extends AbstractDocuments {
  private static DocumentsUpdator thisInstance = null;

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

  public UpdateResponse updateDocuments(List<SolrDocument> solrDocuments) throws IOException, SolrServerException {
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

      updateResponse = solrRequest.process(solrClient, solrCollection);
      //FIXME replace with log
      System.out.println("Number of documents sent for update: " + docsToUpdate.size());
    }

    return updateResponse;
  }

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
