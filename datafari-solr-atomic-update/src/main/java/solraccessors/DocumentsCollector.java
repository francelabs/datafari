package solraccessors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DocumentsCollector extends AbstractDocuments{

  public DocumentsCollector(final String baseSolrUrl, final String solrCollection, final int maxDocsPerQuery){
    super(baseSolrUrl, solrCollection, maxDocsPerQuery);
  }

  public ArrayList<String> collectDocuments(Date fromDate) throws SolrServerException, IOException {
    //Prepare query to Solr
    SolrQuery queryParams = getSolrQuery(fromDate);

    //Retrieve documents in x-packet batches (x = maxDocsPerQuery).
    String cursorMark = CursorMarkParams.CURSOR_MARK_START;
    ArrayList<String> listIDs = new ArrayList<>();
    boolean done = false;
    String nextCursorMark;
    while (! done) {
      queryParams.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
      //Send the query
      QueryResponse response = solrClient.query(solrCollection,queryParams);

      //Retrieve the next results page cursor
      nextCursorMark = response.getNextCursorMark();

      //Retrieve the documents list
      SolrDocumentList documents = response.getResults();
      for(SolrDocument document : documents) {
        listIDs.add((String) document.getFieldValue("id"));
      }
      if (cursorMark.equals(nextCursorMark)) {
        done = true;
      }
      cursorMark = nextCursorMark;
    }

    return listIDs;

  }

  private SolrQuery getSolrQuery(Date fromDate){
    final SolrQuery query = new SolrQuery("*:*");
    query.addField("id");
    if (fromDate != null) {
      query.addFilterQuery("last_modified:[" + fromDate.toInstant().toString() + " TO NOW]");
    }
    query.setSort("id", SolrQuery.ORDER.asc);
    query.setRows(maxDocsPerQuery);
    return query;
  }
}
