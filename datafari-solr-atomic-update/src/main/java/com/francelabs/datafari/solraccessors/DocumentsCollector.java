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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Solr Accessor to retrieve documents. Related to the <b>Source Collection</b> of an Atomic Update Job.
 */
public class DocumentsCollector extends AbstractDocuments {
  private static DocumentsCollector thisInstance = null;
  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsCollector.class);
  private String cursorMark = null;

  /**
   * Create a unique instance of this Accessor. Configure all necessary parameters to access Solr collection with the
   * given jobConfig. Creates and configure the Solr Client to access the target Solr collection.
   *
   * @param jobConfig the configuration object of the job associated with this Accessor.
   * @return
   * @throws IOException if an I/O exception occurs while configuring this instance (precisely the Solr Client).
   */
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

  /**
   * Same as {@link DocumentsCollector#collectDocuments(String)}.
   *
   */
  public ArrayList<SolrDocument> collectDocuments(Date fromDate) throws SolrServerException, IOException {
    return collectDocuments(fromDate.toInstant().toString());
  }

  /**
   * Collect all documents from the date specified date. The date is compared to the last modified date of the document.
   * Retrieve documents in x-packet batches according to the configuration parameter: nbDocsPerBatch in the configuration file.
   * Call this method as many times as necessary to get all documents (until the resulting list is empty).
   *
   * @param fromDate will be compared to the last_modified field of the documents.
   * @return a list of documents selected. Number is equal or less than nbDocsPerBatch. An empty list means there is no
   * document remaining.
   * @throws SolrServerException
   * @throws IOException
   */
  public ArrayList<SolrDocument> collectDocuments(String fromDate) throws SolrServerException, IOException {
    //Prepare query to Solr
    SolrQuery queryParams = getSolrQuery(fromDate, jobConfig.getFieldsOperation().keySet());

    //Retrieve documents in x-packet batches (x = maxDocsPerQuery).
    if (cursorMark == null) {
      cursorMark = CursorMarkParams.CURSOR_MARK_START;
    }
    queryParams.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);

    // Search handler
    if (jobConfig.getSearchHandler() != null && !jobConfig.getSearchHandler().isEmpty())
      queryParams.set("qt", jobConfig.getSearchHandler());

    //Send the query
    QueryResponse response = null;
    try {
      response = solrClient.query(solrCollection,queryParams);
    } catch (Exception e) {
      LOGGER.error("Query to the server : " + queryParams, e);
      LOGGER.error(jobConfig.getJobName() + " Job: Solr response: " + response);
      throw e;
    }

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
   * Prepare query to select documents to be used to update the destination collection.
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
