package solraccessors;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;
import java.util.*;

public class DocumentsUpdator extends AbstractDocuments {

  public DocumentsUpdator(String baseSolrUrl, String solrCollection, int maxDocsPerQuery) {
    super(baseSolrUrl, solrCollection, maxDocsPerQuery);
  }

  public UpdateResponse updateDocuments(List<String> docIDs) {
    List<SolrInputDocument> docsToUpdate = new ArrayList<>();
    for (String docID : docIDs){
      //Prepare query to Solr
      docsToUpdate.add(createSolrDocToUpdate(docID));
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

  private SolrInputDocument createSolrDocToUpdate(String docID){
    // create the atomic document
    SolrInputDocument atomicDoc = new SolrInputDocument();
    atomicDoc.addField("id", docID);
    Map<String, Object> fieldModifier = new HashMap<>(1);
    fieldModifier.put("set", "val_" + new Date().toInstant().toString());
    atomicDoc.addField("mon_champ1", fieldModifier);
    // Optimistic Concurrency: the document must exist to be updated
    atomicDoc.addField(CommonParams.VERSION_FIELD, 1);

    return atomicDoc;
  }
}
