/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
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

	public static void processStatsResponse(QueryResponse queryResponse)
			throws Exception {
		NamedList responseHeader = queryResponse.getResponseHeader();
		FacetField QFacet = queryResponse.getFacetField("q");

		Long numTot = queryResponse.getResults().getNumFound();
		

		SolrDocumentList solrDocumentList = new SolrDocumentList();
		solrDocumentList.setNumFound(QFacet.getValueCount());
		solrDocumentList.setStart(0);

		if (numTot != 0) {
			Map<String, FieldStatsInfo> stats = queryResponse
					.getFieldStatsInfo();
			List<FieldStatsInfo> noHitsStats = ((FieldStatsInfo) stats
					.get("noHits")).getFacets().get("q");
			List<FieldStatsInfo> QTimeStats = ((FieldStatsInfo) stats
					.get("QTime")).getFacets().get("q");
			List<FieldStatsInfo> positionClickTotStats = null;
			try {
				positionClickTotStats = ((FieldStatsInfo) stats
					.get("positionClickTot")).getFacets().get("q");
			} catch (Exception e){
				
			}
			List<FieldStatsInfo> clickStats = ((FieldStatsInfo) stats
					.get("click")).getFacets().get("q");
			List<FieldStatsInfo> numClicksStats = ((FieldStatsInfo) stats
					.get("numClicks")).getFacets().get("q");
			List<FieldStatsInfo> numFoundStats = ((FieldStatsInfo) stats
					.get("numFound")).getFacets().get("q");


			List<Count> QFacetValues = QFacet.getValues();
			
			Map<String, SolrDocument> mapDocuments = new HashMap<String, SolrDocument>();
			
			for (int i = 0; i < QFacetValues.size(); i++) {
				SolrDocument doc = new SolrDocument();
				String query = QFacetValues.get(i).getName();

				double count = QFacetValues.get(i).getCount();
				double frequency = StatsUtils.round(count * 100 / numTot, 2,
						BigDecimal.ROUND_HALF_UP);
				
				doc.addField("query", query);

				doc.addField("count", count);
				doc.addField("frequency", frequency);
				mapDocuments.put(query, doc);
				solrDocumentList.add(doc);
			}
			

			for (int i = 0; i < QTimeStats.size(); i++) {
				String query = QTimeStats.get(i).getName();
				SolrDocument doc = mapDocuments.get(query);

				int AVGHits = new Double((Double) numFoundStats.get(i)
						.getMean()).intValue();
				Double noHits = new Double((Double) noHitsStats.get(i).getSum());
				int AVGQTime = new Double((Double) QTimeStats.get(i).getMean())
						.intValue();
				int MAXQTime = new Double((Double) QTimeStats.get(i).getMax())
						.intValue();
				double click = new Double((Double) clickStats.get(i).getSum());
				double clickRatio = StatsUtils.round(click * 100 / (Double)doc.getFirstValue("count"), 2,
						BigDecimal.ROUND_HALF_UP);
				if (click>0){
					double AVGClickPosition = new Double(
							(Double) positionClickTotStats.get(i).getSum()
									/ (Double) numClicksStats.get(i).getSum())
							.intValue();

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

		NamedList<Object> response = new SimpleOrderedMap<Object>();
		response.add("responseHeader", responseHeader);
		response.add("response", solrDocumentList);
		queryResponse.setResponse(response);
	}


}
