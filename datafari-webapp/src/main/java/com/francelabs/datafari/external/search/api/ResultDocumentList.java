package com.francelabs.datafari.external.search.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ResultDocumentList {

  private final List<ResultDocument> resultList;
  private int numFound;
  private int start;
  private int rows;
  private final Map<String, Object> otherFields = new HashMap<>();

  public ResultDocumentList() {
    resultList = new ArrayList<>();
    this.numFound = -1;
    this.start = 0;
    this.rows = 0;
  }

  public void addOtherField(final String fieldName, final Object value) {
    otherFields.put(fieldName, value);
  }

  public void addResultDocument(final ResultDocument rd) {
    resultList.add(rd);
  }

  public void setNumFound(final int numFound) {
    this.numFound = numFound;
  }

  public void setStart(final int start) {
    this.start = start;
  }

  public void setRows(final int rows) {
    this.rows = rows;
  }

  public JSONObject getAsJSONResponse() {
    final JSONObject fullResponse = new JSONObject();
    final JSONObject response = new JSONObject();
    final JSONObject highlighting = new JSONObject();
    response.put("numFound", numFound);
    response.put("start", start);
    response.put("rows", rows);
    final JSONArray docs = new JSONArray();
    resultList.forEach(rd -> {
      docs.add(rd.getAsJSON());
      highlighting.put(rd.getId(), rd.getHighlighting());
    });
    response.put("docs", docs);
    for (final String fieldName : otherFields.keySet()) {
      response.put(fieldName, otherFields.get(fieldName));
    }
    fullResponse.put("response", response);
    fullResponse.put("highlighting", highlighting);
    return fullResponse;
  }

}
