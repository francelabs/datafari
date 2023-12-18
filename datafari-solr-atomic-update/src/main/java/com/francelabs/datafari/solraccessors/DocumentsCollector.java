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

public class DocumentsCollector extends AbstractDocuments{

  public DocumentsCollector(JobConfig jobConfig, int maxDocsPerQuery) {
    super(jobConfig, maxDocsPerQuery);
  }

  @Override
  protected CollectionPathConfig getCollectionPath() {
    return jobConfig.getSource();
  }

  public ArrayList<SolrDocument> collectDocuments(Date fromDate) throws SolrServerException, IOException {
    //Prepare query to Solr
    SolrQuery queryParams = getSolrQuery(fromDate, jobConfig.getFieldsOperation().keySet());

    //Retrieve documents in x-packet batches (x = maxDocsPerQuery).
    String cursorMark = CursorMarkParams.CURSOR_MARK_START;
    ArrayList<SolrDocument> docsResultList = new ArrayList<>();
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
        docsResultList.add(document);
      }
      if (cursorMark.equals(nextCursorMark)) {
        done = true;
      }
      cursorMark = nextCursorMark;
    }

    return docsResultList;

  }

  private SolrQuery getSolrQuery(Date fromDate, Collection<String> fieldsToUpdate){
    final SolrQuery query = new SolrQuery("*:*");
    query.addField(CommonParams.ID);
    for (String field: fieldsToUpdate) {
      query.addField(field);
    }

    if (fromDate != null) {
      query.addFilterQuery("last_modified:[" + fromDate.toInstant().toString() + " TO NOW]");
    }
    query.setSort(CommonParams.ID, SolrQuery.ORDER.asc);
    query.setRows(maxDocsPerQuery);
    return query;
  }
}
