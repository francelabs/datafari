package solraccessors;

import config.CollectionPathConfig;
import config.JobConfig;
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
    for (SolrDocument doc : solrDocuments){
      //Prepare query to Solr
      docsToUpdate.add(createSolrDocToUpdate(doc));
    }

    try {
      UpdateRequest solrRequest = new UpdateRequest();
      // Do not reject all update batch for some version conflicts
      solrRequest.setParam("failOnVersionConflicts", "false");
      solrRequest.add(docsToUpdate);

      UpdateResponse updateResponse = solrRequest.process(solrClient, solrCollection);
      solrClient.close();
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

  private SolrInputDocument createSolrDocToUpdate(SolrDocument doc){
    // create the atomic document
    SolrInputDocument atomicDoc = new SolrInputDocument();
    //atomicDoc.addField(CommonParams.ID, doc.getFieldValue(CommonParams.ID));

    Map<String, Object> fieldModifier;
    String fieldValue;
    for(String fieldName : doc.getFieldNames()){
      fieldValue = (String) doc.getFieldValue(fieldName);

      //FIXME Work in progress
      fieldModifier = new HashMap<>(1);
      fieldModifier.put("set", "val_" + new Date().toInstant().toString());
      if (CommonParams.ID.equals(fieldName)){
        atomicDoc.addField(fieldName, fieldValue);

      } else {
        atomicDoc.addField(fieldName, fieldModifier);
      }

    }

    // Optimistic Concurrency: the document must exist to be updated
    atomicDoc.addField(CommonParams.VERSION_FIELD, 1);

    return atomicDoc;
  }
}
