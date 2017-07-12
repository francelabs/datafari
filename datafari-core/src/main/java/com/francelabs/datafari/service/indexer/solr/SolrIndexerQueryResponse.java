package com.francelabs.datafari.service.indexer.solr;

import java.io.IOException;
import java.io.StringWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.json.JSONArray;
import org.json.JSONObject;

import com.francelabs.datafari.service.indexer.IndexerFacetField;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;

public class SolrIndexerQueryResponse implements IndexerQueryResponse {

  private final QueryResponse response;
  private final SolrQuery query;

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
    final JSONObject jsonResponse = new JSONObject(strJSON).getJSONObject("response");
    if (!jsonResponse.has("docs") || jsonResponse.isNull("docs")) {
      return null;
    } else {
      final JSONArray jsonResults = jsonResponse.getJSONArray("docs");
      return jsonResults;
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
