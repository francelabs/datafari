/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.service.db;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.francelabs.datafari.annotator.exception.AnnotatorException;
import com.francelabs.datafari.annotator.model.CrawledDocument;

public class AnnotatorDataService extends CassandraService {

  final static Logger logger = LogManager.getLogger(AnnotatorDataService.class.getName());

  public static final String DOCUMENTIDCOLUMN = "id";
  public static final String DOCUMENTPATHCOLUMN = "doc_path";
  public static final String FIELDSCOLUMN = "fields";
  public static final String PROCESSEDCOLUMN = "processed";
  public static final String ERROREDCOLUMN = "errored";
  public static final String DOCUMENTCOLLECTION = "crawled_document";
  public static final String LASTCHECK = "lastcheck";
  public static final String SOLRCORECOLUMN = "solr_core";
  public static final String SOLRUPDATEHANDLERCOLUMN = "solr_update_handler";
  public static final String ANNOTATORSCOLUMN = "annotators";

  private static AnnotatorDataService instance;

  private final PreparedStatement addDocumentStatement;
  private final PreparedStatement deleteDocumentStatement;
  private final PreparedStatement popDocument;
  private final PreparedStatement getDocuments;

  public static synchronized AnnotatorDataService getInstance() throws AnnotatorException {
    try {
      if (instance == null) {
        instance = new AnnotatorDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to get instance : " + e.getMessage());
      throw new AnnotatorException("Unable to get instance", e);
    }
  }

  /**
   * Add a document to cassandra
   *
   * @param username
   *          of the user the id that should be liked
   * @return Like.ALREADYPERFORMED if the like was already done, CodesUser.ALLOK if all was ok
   */
  public void addDocument(final String id, final String binaryDocPath, final Map<String, List<String>> fields, final String solrCore, final String solrUpdateHandler, final List<String> annotators,
      final boolean processed, final boolean errored) throws AnnotatorException {
    try {
      final BoundStatement boundStatement = addDocumentStatement.bind(id, binaryDocPath, fields, processed, errored, solrCore, solrUpdateHandler, annotators);
      session.execute(boundStatement);

    } catch (final DriverException e) {
      logger.warn("Unable to add document : " + e.getMessage(), e);
      // TODO catch specific exception
      throw new AnnotatorException("Unable to add document", e);
    }

  }

  public void addDocument(final CrawledDocument document) throws AnnotatorException {
    this.addDocument(document.getId(), document.getOriginalDocPath(), document.getFields(), document.getSolrCore(), document.getSolrUpdateHandler(), document.getAnnotators(), false, false);
  }

  public void addDocument(final CrawledDocument document, final boolean processed, final boolean errored) throws AnnotatorException {
    this.addDocument(document.getId(), document.getOriginalDocPath(), document.getFields(), document.getSolrCore(), document.getSolrUpdateHandler(), document.getAnnotators(), processed, errored);
  }

  private AnnotatorDataService() {
    refreshSession();
    addDocumentStatement = session.prepare("insert into " + DOCUMENTCOLLECTION + " (" + DOCUMENTIDCOLUMN + "," + DOCUMENTPATHCOLUMN + "," + FIELDSCOLUMN + "," + LASTCHECK + "," + PROCESSEDCOLUMN + ","
        + ERROREDCOLUMN + "," + SOLRCORECOLUMN + "," + SOLRUPDATEHANDLERCOLUMN + "," + ANNOTATORSCOLUMN + ")" + " values (?, ?, ?, toTimeStamp(NOW()), ?, ?, ?, ?, ?)");
    deleteDocumentStatement = session
        .prepare("delete from " + DOCUMENTCOLLECTION + " where " + PROCESSEDCOLUMN + " = ? and " + ERROREDCOLUMN + " = ? and " + DOCUMENTIDCOLUMN + " = ? and " + LASTCHECK + " = ?");
    popDocument = session.prepare("select " + DOCUMENTIDCOLUMN + "," + DOCUMENTPATHCOLUMN + "," + FIELDSCOLUMN + "," + LASTCHECK + "," + SOLRCORECOLUMN + "," + SOLRUPDATEHANDLERCOLUMN + ","
        + ANNOTATORSCOLUMN + " from " + DOCUMENTCOLLECTION + " where " + PROCESSEDCOLUMN + " = ? and " + ERROREDCOLUMN + " = ? limit ?");
    getDocuments = session.prepare("select * from " + DOCUMENTCOLLECTION + " where " + PROCESSEDCOLUMN + " = ? and " + ERROREDCOLUMN + " = ? and " + LASTCHECK + " > ? limit ?");
  }

  public boolean setAsProcessed(final CrawledDocument document) throws AnnotatorException {

    // As document the field we want to refresh is part of the Cassandra clustering key, need to delete and add the document to "fake" an update
    // we know that if we are about to set the doc as processed it means that it is not currently either processed or errored, so we know that
    // those two values are false
    this.deleteDocument(document, false, false);
    this.addDocument(document, true, false);

    return true;

  }

  public boolean setAsErrored(final CrawledDocument document) throws AnnotatorException {

    // As document the field we want to refresh is part of the Cassandra clustering key, need to delete and add the document to "fake" an update
    // we know that if we are about to set the doc as errored it means that it is not currently either processed or errored, so we know that
    // those two values are false
    this.deleteDocument(document, false, false);
    this.addDocument(document, false, true);

    return true;

  }

  // Update lastCheck to current timestamp : NOW()
  public boolean refreshDocument(final CrawledDocument document) throws AnnotatorException {

    // As document the field we want to refresh is part of the Cassandra clustering key, need to delete and add the document to "fake" an update
    this.deleteDocument(document, false, false);
    this.addDocument(document);

    return true;

  }

  /**
   * Retrieve documents from cassandra
   *
   * @return Crawled Document
   */
  public List<CrawledDocument> popDocument(final boolean processed, final boolean errored, final int numDocuments) throws AnnotatorException {
    final List<CrawledDocument> documents = new ArrayList<>();
    try {
      final BoundStatement boundStatement = popDocument.bind(processed, errored, numDocuments);
      final ResultSet rs = session.execute(boundStatement);

      for (final Row row : rs) {
        final CrawledDocument document = new CrawledDocument(row.getString(DOCUMENTIDCOLUMN), row.getString(SOLRCORECOLUMN), row.getString(SOLRUPDATEHANDLERCOLUMN), row.getString(DOCUMENTPATHCOLUMN));
        document.setDocPath(row.getString(DOCUMENTPATHCOLUMN));
        document.setFields(row.getMap(FIELDSCOLUMN, String.class, (Class<List<String>>) GenericType.listOf(String.class).getRawType()));
        document.setLastCheck(row.getInstant(LASTCHECK));
        document.setAnnotators(row.getList(ANNOTATORSCOLUMN, String.class));
        documents.add(document);
      }

    } catch (final DriverException e) {
      logger.warn("Unable to get crawledDocuments : " + e.getMessage());
      throw new AnnotatorException("Unable to get crawledDocuments", e);
    }

    return documents;

  }

  /**
   * Retrieve documents from cassandra
   *
   * @param object
   *
   * @return Crawled Document
   */
  public List<CrawledDocument> getDocuments(final boolean processed, final boolean errored, final int numDocuments, Instant lastCheck) throws AnnotatorException {
    final List<CrawledDocument> documents = new ArrayList<>();
    try {
      if (lastCheck == null) {
        // Create epoch date
        lastCheck = new Date(0L).toInstant();
      }
      final BoundStatement boundStatement = getDocuments.bind(processed, errored, lastCheck, numDocuments);
      final ResultSet rs = session.execute(boundStatement);
      rs.forEach(row -> {
        final CrawledDocument document = new CrawledDocument(row.getString(DOCUMENTIDCOLUMN), row.getString(SOLRCORECOLUMN), row.getString(SOLRUPDATEHANDLERCOLUMN), row.getString(DOCUMENTPATHCOLUMN));
        document.setDocPath(row.getString(DOCUMENTPATHCOLUMN));
        document.setFields(row.getMap(FIELDSCOLUMN, String.class, (Class<List<String>>) GenericType.listOf(String.class).getRawType()));
        document.setLastCheck(row.getInstant(LASTCHECK));
        document.setAnnotators(row.getList(ANNOTATORSCOLUMN, String.class));
        documents.add(document);
      });

    } catch (final DriverException e) {
      logger.warn("Unable to get crawledDocument : " + e.getMessage());
      throw new AnnotatorException("Unable to get crawledDocument", e);
    }

    return documents;

  }

  public void deleteDocuments(final List<CrawledDocument> documents, final boolean processed, final boolean errored) throws AnnotatorException {
    for (final CrawledDocument document : documents) {
      this.deleteDocument(document, processed, errored);
    }
  }

  public void deleteDocument(final CrawledDocument document, final boolean processed, final boolean errored) throws AnnotatorException {
    try {
      final BoundStatement boundStatement = deleteDocumentStatement.bind(processed, errored, document.getId(), document.getLastCheck());
      session.execute(boundStatement);

    } catch (final DriverException e) {
      logger.warn("Unable to delete document " + document.getId() + " : " + e.getMessage());
      throw new AnnotatorException("Unable to set document has errored", e);
    }
  }

}