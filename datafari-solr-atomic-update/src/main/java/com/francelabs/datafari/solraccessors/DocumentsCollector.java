package com.francelabs.datafari.solraccessors;

import com.francelabs.datafari.config.CollectionPathConfig;
import com.francelabs.datafari.config.JobConfig;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class DocumentsCollector extends AbstractDocuments {
  private static DocumentsCollector thisInstance = null;
  private String cursorMark = null;

  public static DocumentsCollector getInstance(JobConfig jobConfig) throws IOException {
    if (thisInstance == null){
      thisInstance = new DocumentsCollector();
    }
    thisInstance.setJobConfig(jobConfig);
    return thisInstance;
  }

  @Override
  protected CollectionPathConfig getCollectionPath() {
    return jobConfig.getSource();
  }

  public ArrayList<SolrDocument> collectDocuments(Date fromDate) throws SolrServerException, IOException {
    return collectDocuments(fromDate.toInstant().toString());
  }
  public ArrayList<SolrDocument> collectDocuments(String fromDate) throws SolrServerException, IOException {
    //Prepare query to Solr
    SolrQuery queryParams = getSolrQuery(fromDate, jobConfig.getFieldsOperation().keySet());

    //Retrieve documents in x-packet batches (x = maxDocsPerQuery).
    if (cursorMark == null) {
      cursorMark = CursorMarkParams.CURSOR_MARK_START;
    }
    queryParams.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
    //Send the query
    QueryResponse response = solrClient.query(solrCollection,queryParams);

    //Retrieve the next results page cursor
    String nextCursorMark = response.getNextCursorMark();

    //Retrieve the documents list
    SolrDocumentList documents = response.getResults();
    if (cursorMark.equals(nextCursorMark)) {
      solrClient.close();
    }
    cursorMark = nextCursorMark;
    return documents;

  }

  /**
   *
   * @param fromDate if null no selection from the last_modified field of documents.
   * @param fieldsToUpdate set of Solr fields used to update the destination documents.
   * @return the Solr query created to be sent to Solr
   */
  private SolrQuery getSolrQuery(String fromDate, Collection<String> fieldsToUpdate){
    final SolrQuery query = new SolrQuery("*:*");
    query.addField(CommonParams.ID);
    for (String field: fieldsToUpdate) {
      query.addField(field);
    }

    if (fromDate != null) {
      query.addFilterQuery("last_modified:[" + fromDate + " TO NOW]");
    }
    query.setSort(CommonParams.ID, SolrQuery.ORDER.asc);
    query.setRows(maxDocsPerQuery);
    return query;
  }
}
