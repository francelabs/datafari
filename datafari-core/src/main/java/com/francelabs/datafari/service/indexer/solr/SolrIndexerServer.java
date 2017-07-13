package com.francelabs.datafari.service.indexer.solr;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.FieldTypes;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.ReplaceFieldType;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Update;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.FieldTypesResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import com.francelabs.datafari.service.indexer.IndexerInputDocument;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerResponseDocument;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.statistics.StatsUtils;
import com.francelabs.datafari.utils.ScriptConfiguration;

public class SolrIndexerServer implements IndexerServer {

  private static String defaultURL = "localhost:2181";
  private final Logger LOGGER = Logger.getLogger(SolrIndexerServer.class.getName());
  private CloudSolrClient client;

  public SolrIndexerServer(final Core core) throws Exception {
    // Zookeeper Hosts
    final String solrHosts = ScriptConfiguration.getProperty("SOLRHOSTS");

    try {
      // TODO : change for ZK ensemble
      client = new CloudSolrClient.Builder().withZkHost(solrHosts).build();
      client.setDefaultCollection(core.toString());
      final SolrPing ping = new SolrPing();
      client.request(ping);
    } catch (final Exception e) {
      // test default param
      try {
        client = new CloudSolrClient.Builder().withZkHost(defaultURL).build();
        client.setDefaultCollection(core.toString());
        final SolrPing ping = new SolrPing();
        client.request(ping);
      } catch (final Exception e2) {
        LOGGER.error("Cannot instanciate Solr Client for core : " + core.toString(), e);
        throw new Exception("Cannot instanciate Solr Client for core : " + core.toString());
      }
    }

  }

  @Override
  public IndexerQueryResponse executeQuery(final IndexerQuery query) throws Exception {
    try {
      final SolrQuery solrQuery = ((SolrIndexerQuery) query).prepareQuery();
      final QueryResponse response = client.query(solrQuery);
      final SolrIndexerQueryResponse sir = new SolrIndexerQueryResponse(solrQuery, response);
      return sir;
    } catch (SolrServerException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void pushDoc(final IndexerInputDocument document, final int commitWithinMs) throws Exception {
    final SolrInputDocument solrDoc = ((SolrIndexerInputDocument) document).getSolrInputDocument();
    client.add(solrDoc, commitWithinMs);
  }

  @Override
  public IndexerResponseDocument getDocById(final String id) throws Exception {
    final SolrDocument document = client.getById(id);
    return new SolrIndexerResponseDocument(document);
  }

  @Override
  public void processStatsResponse(final IndexerQueryResponse queryResponse) {
    final QueryResponse solrResponse = ((SolrIndexerQueryResponse) queryResponse).getQueryResponse();
    final NamedList responseHeader = solrResponse.getResponseHeader();
    final FacetField QFacet = solrResponse.getFacetField("q");

    final Long numTot = queryResponse.getNumFound();

    final SolrDocumentList solrDocumentList = new SolrDocumentList();
    solrDocumentList.setNumFound(QFacet.getValueCount());
    solrDocumentList.setStart(0);

    if (numTot != 0) {
      final Map<String, FieldStatsInfo> stats = solrResponse.getFieldStatsInfo();
      final List<FieldStatsInfo> noHitsStats = stats.get("noHits").getFacets().get("q");
      final List<FieldStatsInfo> QTimeStats = stats.get("QTime").getFacets().get("q");
      List<FieldStatsInfo> positionClickTotStats = null;
      try {
        positionClickTotStats = stats.get("positionClickTot").getFacets().get("q");
      } catch (final Exception e) {

      }
      final List<FieldStatsInfo> clickStats = stats.get("click").getFacets().get("q");
      final List<FieldStatsInfo> numClicksStats = stats.get("numClicks").getFacets().get("q");
      final List<FieldStatsInfo> numFoundStats = stats.get("numFound").getFacets().get("q");

      final List<Count> QFacetValues = QFacet.getValues();

      final Map<String, SolrDocument> mapDocuments = new HashMap<String, SolrDocument>();

      for (int i = 0; i < QFacetValues.size(); i++) {
        final SolrDocument doc = new SolrDocument();
        final String query = QFacetValues.get(i).getName();

        final double count = QFacetValues.get(i).getCount();
        final double frequency = StatsUtils.round(count * 100 / numTot, 2, BigDecimal.ROUND_HALF_UP);

        doc.addField("query", query);

        doc.addField("count", count);
        doc.addField("frequency", frequency);
        mapDocuments.put(query, doc);
        solrDocumentList.add(doc);
      }

      for (int i = 0; i < QTimeStats.size(); i++) {
        final String query = QTimeStats.get(i).getName();
        final SolrDocument doc = mapDocuments.get(query);

        final int AVGHits = new Double((Double) numFoundStats.get(i).getMean()).intValue();
        final Double noHits = new Double((Double) noHitsStats.get(i).getSum());
        final int AVGQTime = new Double((Double) QTimeStats.get(i).getMean()).intValue();
        final int MAXQTime = new Double((Double) QTimeStats.get(i).getMax()).intValue();
        final double click = new Double((Double) clickStats.get(i).getSum());
        final double clickRatio = StatsUtils.round(click * 100 / (Double) doc.getFirstValue("count"), 2,
            BigDecimal.ROUND_HALF_UP);
        if (click > 0) {
          final double AVGClickPosition = new Double(
              (Double) positionClickTotStats.get(i).getSum() / (Double) numClicksStats.get(i).getSum()).intValue();

          doc.addField("AVGClickPosition", AVGClickPosition);

        } else {
          doc.addField("AVGClickPosition", "-");
        }

        doc.addField("withClickRatio", clickRatio);
        doc.addField("AVGHits", AVGHits);
        doc.addField("numNoHits", noHits);
        doc.addField("withClick", click);
        doc.addField("AVGQTime", AVGQTime);
        doc.addField("MaxQTime", MAXQTime);
      }

    }

    final NamedList<Object> response = new SimpleOrderedMap<Object>();
    response.add("responseHeader", responseHeader);
    response.add("response", solrDocumentList);
    solrResponse.setResponse(response);

  }

  @Override
  public void pushDoc(final IndexerInputDocument document) throws Exception {
    final SolrInputDocument solrDoc = ((SolrIndexerInputDocument) document).getSolrInputDocument();
    client.add(solrDoc);

  }

  @Override
  public void commit() throws Exception {
    client.commit();

  }

  @Override
  public void deleteById(final String id) throws Exception {
    client.deleteById(id);

  }

  /**
   * Get current value for a filter of an analyzer type for text_* fields
   *
   * filterClass = name of the filter filterAttr = attr in filter
   *
   */
  @Override
  public String getAnalyzerFilterValue(final String filterClass, final String filterAttr) throws Exception {
    String value = "";
    final FieldTypes solrRequest = new FieldTypes();
    final FieldTypesResponse solrResponse = new FieldTypesResponse();
    solrResponse.setResponse(client.request(solrRequest));

    for (final FieldTypeRepresentation fieldType : solrResponse.getFieldTypes()) {
      final String name = (String) fieldType.getAttributes().get("name");
      // get all analyzers for text_* field
      if (name != null && name.startsWith("text_")) {
        final List<AnalyzerDefinition> analyzers = new ArrayList<AnalyzerDefinition>();
        analyzers.add(fieldType.getAnalyzer());
        analyzers.add(fieldType.getIndexAnalyzer());
        analyzers.add(fieldType.getQueryAnalyzer());
        for (final AnalyzerDefinition analyzer : analyzers) {
          if (analyzer != null) {
            final List<Map<String, Object>> filters = analyzer.getFilters();
            if (filters != null) {
              for (final Map<String, Object> filter : filters) {
                if (filter != null) {
                  final String clazzAttr = (String) filter.get("class");
                  if (clazzAttr != null && clazzAttr.equals(filterClass)) {
                    // get last value
                    value = (String) filter.get(filterAttr);
                  }
                }
              }
            }
          }
        }
      }
    }
    return value;
  }

  /**
   * Set the current value for a filter of an analyzer type
   *
   * filterClass = name of the filter filterAttr = attribute of the filter value
   * = value to change
   *
   */
  @Override
  public void updateAnalyzerFilterValue(final String filterClass, final String filterAttr, final String value)
      throws Exception {
    final FieldTypes solrRequest = new FieldTypes();
    final FieldTypesResponse solrResponse = new FieldTypesResponse();
    solrResponse.setResponse(client.request(solrRequest));
    final List<Update> updates = new ArrayList<Update>();
    for (final FieldTypeRepresentation fieldType : solrResponse.getFieldTypes()) {
      final String name = (String) fieldType.getAttributes().get("name");
      if (name != null && name.startsWith("text_")) {
        final List<AnalyzerDefinition> analyzers = new ArrayList<AnalyzerDefinition>();
        analyzers.add(fieldType.getAnalyzer());
        analyzers.add(fieldType.getIndexAnalyzer());
        analyzers.add(fieldType.getQueryAnalyzer());
        for (final AnalyzerDefinition analyzer : analyzers) {
          if (analyzer != null) {
            final List<Map<String, Object>> filters = analyzer.getFilters();
            if (filters != null) {
              for (final Map<String, Object> filter : filters) {
                if (filter != null) {
                  final String clazzAttr = (String) filter.get("class");
                  if (clazzAttr != null && clazzAttr.equals(filterClass)) {
                    filter.put(filterAttr, value);
                    // keep each modified fieldType definition
                    updates.add(new ReplaceFieldType(fieldType));
                  }
                }
              }
            }
          }
        }
      }
    }

    // send a bulk update of each modified fieldType definition
    final MultiUpdate multiUpdateRequest = new MultiUpdate(updates);
    client.request(multiUpdateRequest);

  }
}
