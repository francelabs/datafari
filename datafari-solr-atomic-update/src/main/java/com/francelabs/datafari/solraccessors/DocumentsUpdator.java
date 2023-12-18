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

  public DocumentsUpdator(JobConfig jobConfig, int maxDocsPerQuery) {
    super(jobConfig, maxDocsPerQuery);
  }

  @Override
  protected CollectionPathConfig getCollectionPath() {
    return jobConfig.getDestination();
  }

  public UpdateResponse updateDocuments(List<SolrDocument> solrDocuments) {
    List<SolrInputDocument> docsToUpdate = new ArrayList<>();
    //Prepare query to Solr with all documents to update.
    int count=0;
    for (SolrDocument doc : solrDocuments){
      //Do not update documents that have all fields null except ID field.
      if(doc.size() > 1) {
        docsToUpdate.add(createSolrDocToUpdate(doc));
        count++;
      }
    }

    try {
      UpdateRequest solrRequest = new UpdateRequest();
      // Do not reject all update batch for some version conflicts
      solrRequest.setParam("failOnVersionConflicts", "false");
      solrRequest.add(docsToUpdate);

      UpdateResponse updateResponse = solrRequest.process(solrClient, solrCollection);
      solrClient.close();
      //FIXME replace with log
      System.out.println("Number of documents sent for update: " + count);
      return updateResponse;
    } catch (IOException e) {
      //TODO logs
      e.printStackTrace();
    } catch (SolrServerException e) {
      //TODO logs
      e.printStackTrace();
    }

    return null;
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
