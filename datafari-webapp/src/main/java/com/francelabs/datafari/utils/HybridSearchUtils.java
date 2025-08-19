package com.francelabs.datafari.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Utility methods to apply Reciprocal Rank Fusion (RRF) on Solr results.
 */
public class HybridSearchUtils {

    /**
     * Applies RRF to two Solr result sets (BM25 and Vector Search) and returns a list of document IDs
     * sorted by their fused RRF score in descending order.
     *
     * @param bm25Result           the JSON response from the /select handler (BM25)
     * @param vectorSearchResult   the JSON response from the /vector handler (vector search)
     * @param k                    the RRF constant to control score smoothing (typical values: 10–60)
     * @return a list of document IDs sorted by descending RRF score
     */
    public static List<String> fuseResultsWithRRF(JSONObject bm25Result, JSONObject vectorSearchResult, int k) {
        Map<String, Integer> bm25Ranks = extractRanks(bm25Result);
        Map<String, Integer> vectorRanks = extractRanks(vectorSearchResult);

        Map<String, Double> fusedScores = computeRRF(bm25Ranks, vectorRanks, k);

        return new ArrayList<>(fusedScores.keySet());
    }

    /**
     * Extracts the rank (position in the list) of each document from a Solr JSON response.
     *
     * @param resultJson the JSON object containing Solr results
     * @return a map from document ID to its rank (starting from 1)
     */
    private static Map<String, Integer> extractRanks(JSONObject resultJson) {
        Map<String, Integer> ranks = new HashMap<>();

        JSONObject response = (JSONObject) resultJson.get("response");
        if (response == null) {
            // TODO: handle missing "response" object gracefully
            return ranks;
        }

        JSONArray docs = (JSONArray) response.get("docs");
        if (docs == null) {
            // TODO: handle missing "docs" array gracefully
            return ranks;
        }

        for (int i = 0; i < docs.size(); i++) {
            JSONObject doc = (JSONObject) docs.get(i);
            Object idObj = doc.get("id");
            if (idObj instanceof String) {
                ranks.put((String) idObj, i + 1); // Rank starts at 1
            }
        }

        return ranks;
    }

    /**
     * Computes the Reciprocal Rank Fusion score for all documents across two ranked lists.
     *
     * @param bm25Ranks      map of document IDs to ranks from the BM25 results
     * @param vectorRanks    map of document IDs to ranks from the vector results
     * @param k              RRF smoothing constant
     * @return a map of document IDs to their RRF scores, sorted by descending score
     */
    private static Map<String, Double> computeRRF(Map<String, Integer> bm25Ranks, Map<String, Integer> vectorRanks, int k) {
        Map<String, Double> rrfScores = new HashMap<>();

        for (Map.Entry<String, Integer> entry : bm25Ranks.entrySet()) {
            String docId = entry.getKey();
            int rank = entry.getValue();
            rrfScores.put(docId, 1.0 / (k + rank));
        }

        for (Map.Entry<String, Integer> entry : vectorRanks.entrySet()) {
            String docId = entry.getKey();
            int rank = entry.getValue();
            rrfScores.merge(docId, 1.0 / (k + rank), Double::sum);
        }

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}