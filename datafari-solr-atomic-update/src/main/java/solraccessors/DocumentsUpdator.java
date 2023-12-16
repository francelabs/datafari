package solraccessors;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentsUpdator extends AbstractDocuments {

  public DocumentsUpdator(String baseSolrUrl, String solrCollection, int maxDocsPerQuery) {
    super(baseSolrUrl, solrCollection, maxDocsPerQuery);
  }

  public UpdateResponse updateDocuments(List<String> docIDs) {
    int count = 0;
    List<SolrInputDocument> docsToUpdate = new ArrayList<>();
    for (String docID : docIDs){
      //Prepare query to Solr
      docsToUpdate.add(getSolrQuery(docID));
      count++;
    }

    try {
      UpdateResponse updateResponse = solrClient.add(solrCollection, docsToUpdate,1000);
      solrClient.close();
      return updateResponse;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SolrServerException e) {
      e.printStackTrace();
    }

    return null;
  }

  private SolrInputDocument getSolrQuery(String docID){
    // create the atomic document
    SolrInputDocument atomicDoc = new SolrInputDocument();
    atomicDoc.addField("id", docID);
    Map<String, Object> fieldModifier = new HashMap<>(1);
    fieldModifier.put("set", "val_1");
    atomicDoc.addField("mon_champ1", fieldModifier);

    return atomicDoc;
  }
}
