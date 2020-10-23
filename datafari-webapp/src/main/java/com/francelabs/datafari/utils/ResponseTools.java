package com.francelabs.datafari.utils;

import java.io.IOException;
import java.text.ParseException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;

public class ResponseTools {

  public static final String writeSolrJResponse(final String handler, final IndexerQuery query, final IndexerQueryResponse queryResponse, final IndexerQuery queryPromolink,
      final IndexerQueryResponse queryResponsePromolink, final String username) throws IOException, ParseException, org.json.simple.parser.ParseException {

    final JSONParser parser = new JSONParser();
    final String jsonStrQueryResponse = queryResponse.getStrJSONResponse();
    // Creating a valid json object from the results
    final JSONObject json = (JSONObject) parser.parse(jsonStrQueryResponse.substring(jsonStrQueryResponse.indexOf("{"), jsonStrQueryResponse.lastIndexOf("}") + 1));

    if (queryResponsePromolink != null) {
      // If it was a request on FileShare therefore on promolink

      // Write the result of the query on promolink
      final String jsonStrPromolinkResponse = queryResponsePromolink.getStrJSONResponse();

      if (queryResponsePromolink.getNumFound() != 0) {

        final JSONArray jsonPromolinkDocs = queryResponsePromolink.getResults();
        json.put("promolinkSearchComponent", jsonPromolinkDocs);

      }

    }

    return json.toJSONString();
  }

}
