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

public class DocumentsCollector {
  final private SolrClient solrClient;
  final private String solrCollection;
  final private int maxDocsPerQuery;

  public DocumentsCollector(final String baseSolrUrl, final String solrCollection, final int maxDocsPerQuery){
    //TODO best for SolrCloud mode
    /*final List<String> zkServers = new ArrayList<>();
    //zkServers.add("zookeeper1:2181");
    //zkServers.add("zookeeper2:2181");
    zkServers.add("https://dev.datafari.com:2181"); // ?
    solrClient = new CloudSolrClient.Builder(zkServers, Optional.of("/solr")).build();*/
    solrClient = new Http2SolrClient.Builder(baseSolrUrl)
        .withConnectionTimeout(10000, TimeUnit.MILLISECONDS)
        .withIdleTimeout(60000, TimeUnit.MILLISECONDS)
        .build();

    this.solrCollection = solrCollection;
    this.maxDocsPerQuery = maxDocsPerQuery;
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
