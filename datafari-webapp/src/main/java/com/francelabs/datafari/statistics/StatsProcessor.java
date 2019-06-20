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
package com.francelabs.datafari.statistics;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

public class StatsProcessor {

  public static void processStatsResponse(final QueryResponse queryResponse) throws Exception {
    final NamedList responseHeader = queryResponse.getResponseHeader();
    final FacetField QFacet = queryResponse.getFacetField("q");

    final Long numTot = queryResponse.getResults().getNumFound();

    final SolrDocumentList solrDocumentList = new SolrDocumentList();
    solrDocumentList.setNumFound(QFacet.getValueCount());
    solrDocumentList.setStart(0);

    if (numTot != 0) {
      final Map<String, FieldStatsInfo> stats = queryResponse.getFieldStatsInfo();
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
    queryResponse.setResponse(response);
  }

}
