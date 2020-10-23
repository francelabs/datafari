package com.francelabs.datafari.utils.relevancy;

import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class RelevancyQuery {
  
  private static final String NAME_KEY = "name";
  private static final String QUERY_KEY = "q";
  private static final String RELEVANT_FILES_KEY = "relevant";

  private final JSONObject query;
  private final Set<String> relevantFiles;

  public RelevancyQuery(final String queryName, final String queryValue) {
    this.query = new JSONObject();
    this.query.put(NAME_KEY, queryName);
    this.query.put(QUERY_KEY, queryValue);
    this.relevantFiles = new HashSet<String>();
  }
  
  public RelevancyQuery(final JSONObject query) {
    // FIXME: This is a lazy verification. Deeper investigations may be necessary.
    if ((String) query.get(NAME_KEY) != null &&
        (String) query.get(QUERY_KEY) != null &&
        (JSONArray) query.get(RELEVANT_FILES_KEY) != null) {
      this.query = query;
      JSONArray relevants = (JSONArray) this.query.get(RELEVANT_FILES_KEY);
      this.relevantFiles = new HashSet<String>();
      for (int i = 0; i < relevants.size(); i++) {
        this.relevantFiles.add((String) relevants.get(i));
      }
      return;
    }
    throw new RuntimeException("The JSON Object does not represent a query.");
  }

  public void addRelevantFile(final String file) {
    relevantFiles.add(file);
  }

  public void removeRelevantFile(final String file) {
    relevantFiles.remove(file);
  }

  public Set<String> getRelevantFiles() {
    return relevantFiles;
  }

  public String getName() {
    return (String) this.query.get(NAME_KEY);
  }

  public String getValue() {
    return (String) this.query.get(QUERY_KEY);
  }
  
  public JSONObject toJSON() {
	  this.query.put(RELEVANT_FILES_KEY, relevantFiles);
	  return this.query;
  }
  
  public String toJSONString() {
    return this.toJSON().toJSONString();
  }

  @Override
  public String toString() {
    String query = this.getName() + ":" + this.getValue() + "\n";
    for (final String relevantFile : relevantFiles) {
      query += relevantFile + "\n";
    }
    return query;
  }

}
