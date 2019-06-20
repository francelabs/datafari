/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.service.indexer.solr;

import org.apache.solr.common.SolrInputDocument;

import com.francelabs.datafari.service.indexer.IndexerInputDocument;

/**
 * Input document that is meant to be pushed to the indexer server
 * 
 * @author francelabs
 *
 */
public class SolrIndexerInputDocument implements IndexerInputDocument {

  private final SolrInputDocument inputDocument;

  public SolrIndexerInputDocument() {
    inputDocument = new SolrInputDocument();
  }

  @Override
  public void addField(final String fieldName, final Object value) {
    inputDocument.addField(fieldName, value);
  }

  protected SolrInputDocument getSolrInputDocument() {
    return inputDocument;
  }

  @Override
  public Object getFieldValue(final String fieldName) {
    return inputDocument.getFieldValue(fieldName);
  }

}
