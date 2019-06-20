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

import java.io.IOException;
import java.io.StringWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.JSONResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RTimerTree;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.service.indexer.IndexerFacetField;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;

public class SolrIndexerQueryResponse implements IndexerQueryResponse {

  private final QueryResponse response;
  private final SolrQuery query;
  private final Logger LOGGER = LogManager.getLogger(SolrIndexerQueryResponse.class);

  protected SolrIndexerQueryResponse(final SolrQuery query, final QueryResponse response) {
    this.response = response;
    this.query = query;
  }

  protected QueryResponse getQueryResponse() {
    return response;
  }

  @Override
  public long getNumFound() {
    return response.getResults().getNumFound();
  }

  @Override
  public int getQTime() {
    return response.getQTime();
  }

  @Override
  public String getStrJSONResponse() {

    final SolrQueryRequest req = new SolrQueryRequest() {

      @Override
      public void close() {
        // TODO Auto-generated method stub

      }

      @Override
      public Iterable<ContentStream> getContentStreams() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Map<Object, Object> getContext() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public SolrCore getCore() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Map<String, Object> getJSON() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public SolrParams getOriginalParams() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getParamString() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public SolrParams getParams() {
        // TODO Auto-generated method stub
        return query;
      }

      @Override
      public RTimerTree getRequestTimer() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public IndexSchema getSchema() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public SolrIndexSearcher getSearcher() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public long getStartTime() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public Principal getUserPrincipal() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setJSON(final Map<String, Object> arg0) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setParams(final SolrParams arg0) {
        // TODO Auto-generated method stub

      }

      @Override
      public void updateSchemaToLatest() {
        // TODO Auto-generated method stub

      }

    };

    final SolrQueryResponse res = new SolrQueryResponse();
    res.setAllValues(response.getResponse());
    final JSONResponseWriter jsonWriter = new JSONResponseWriter();
    final StringWriter s = new StringWriter();
    try {
      jsonWriter.write(s, req, res);
      return s.toString();
    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public JSONArray getResults() {
    final String strJSON = getStrJSONResponse();
    final JSONParser parser = new JSONParser();
    try {
      final JSONObject objJSON = (JSONObject) parser.parse(strJSON);
      final JSONObject jsonResponse = (JSONObject) objJSON.get("response");
      if (jsonResponse.get("docs") == null) {
        return null;
      } else {
        final JSONArray jsonResults = (JSONArray) jsonResponse.get("docs");
        return jsonResults;
      }
    } catch (final ParseException e) {
      LOGGER.error("JSON parsing error", e);
      return null;
    }
  }

  @Override
  public List<IndexerFacetField> getFacetFields() {
    final List<IndexerFacetField> listFacetFields = new ArrayList<>();
    for (final FacetField facteField : response.getFacetFields()) {
      listFacetFields.add(new SolrIndexerFacetField(facteField));
    }
    return listFacetFields;

  }

}
