package com.francelabs.datafari.utils.relevancy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RelevancySetup {
  
  private static final String SOLR_HOST_KEY = "solrHost";
  private static final String SOLR_COLLECTION_KEY = "solrCollection";
  private static final String SOLR_CLOUD_KEY = "solrCloud";
  private static final String SOLR_ID_KEY = "idField";
  private static final String PARAMETERS_KEY = "parameters";
  private static final String FIXED_PARAMETERS_KEY = "fixed_params";
  private static final String ITERATIONS_KEY = "iterations";
  private static final String ROWS_KEY = "rows";
  private static final String QUERIES_KEY = "queries";
  
  // The current query is persisted if config files are reloaded
  // (file name changed or file content changed)
  // FIXME: Manage concurrent access and modification of these files correctly.
  // Direct modification on disk and in the web interface or from the web interface by
  // multiple users at the same time may result in exceptions thrown and bugs.
  private static RelevancyQuery currentQuery;
  
  private final File relevancySetupFile;
  private final File goldenQueriesFile;

  private final Map<Integer, RelevancyParameter> parameters = new HashMap<>();
  private final Map<Integer, RelevancyFixedParameter> fixedParameters = new HashMap<>();
  private final Map<String, RelevancyQuery> queries = new HashMap<>();
  
  // TODO: Those are not modifiable from the UI yet, considering that the script is always run
  // from the server hosting datafari and holding the solr node.
  private String solrHost = "http://localhost:8983/solr/";
  private String solrCollection = "FileShare";
  private boolean solrCloud = false;
  private String idFieldName = "id";
  private int iterations = 500;
  private int rows = 20;

  public RelevancySetup(final File relevancySetupFile, final File goldenQueriesFile, 
      final boolean overrideConfig, final boolean overrideQueries) {
    this.relevancySetupFile = relevancySetupFile;
    this.goldenQueriesFile = goldenQueriesFile;
    processConf(relevancySetupFile, goldenQueriesFile, overrideConfig, overrideQueries);
    if (currentQuery != null) {
      queries.put(currentQuery.getName(),currentQuery);
    }
  }

  private void processConf(final File relevancySetupFile, final File goldenQueriesFile, 
      final boolean overrideConfig, final boolean overrideQueries) {

    try (BufferedReader setupBr = new BufferedReader(new FileReader(relevancySetupFile));
    BufferedReader queriesBr = new BufferedReader(new FileReader(goldenQueriesFile))) {
      
      JSONParser parser = new JSONParser();
      if (!overrideConfig) {
        JSONObject configObject;
        try {
            configObject = (JSONObject) parser.parse(new FileReader(relevancySetupFile));
            updateParams(configObject);
        } catch (ParseException e) {
            e.printStackTrace();
        }
      }
      
      if (!overrideQueries) {
        JSONObject queriesObject = null;
        try {
          queriesObject = (JSONObject) parser.parse(new FileReader(goldenQueriesFile));
        } catch (Exception e) {}
        JSONArray queries = (JSONArray) queriesObject.get(QUERIES_KEY);
        for (int i = 0; i < queries.size(); i++) {
          RelevancyQuery newQuery = new RelevancyQuery((JSONObject) queries.get(i));
          this.queries.put(newQuery.getName(), newQuery);
        }
      }   
    } catch (final FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public JSONObject getQueriesJSONObject() {
  	JSONObject output = new JSONObject();
    // Ensures that the array exists even if it is empty
  	JSONArray array = new JSONArray();
  	
  	for (final RelevancyQuery query : this.queries.values()) {
  		array.add(query.toJSON());
  	}
  	output.put(QUERIES_KEY, array);
  	return output;
  }
  
  public JSONObject getParametersJSONObject() {
    JSONObject output = new JSONObject();
    output.put(SOLR_HOST_KEY, this.solrHost);
    output.put(SOLR_COLLECTION_KEY, this.solrCollection);
    output.put(SOLR_CLOUD_KEY, this.solrCloud);
    output.put(SOLR_ID_KEY, this.idFieldName);
    output.put(ITERATIONS_KEY, this.iterations);
    output.put(ROWS_KEY, this.rows);
    // Ensures that the array exists even if it is empty
    JSONArray array = new JSONArray();
    for (final RelevancyParameter parameter : this.parameters.values()) {
      array.add(parameter.toJSON());
    }
    output.put(PARAMETERS_KEY, array);
    // Ensures that the array exists even if it is empty
    array = new JSONArray();
    for (final RelevancyFixedParameter parameter : this.fixedParameters.values()) {
      array.add(parameter.toJSON());
    }
    output.put(FIXED_PARAMETERS_KEY, array);
    return output;
  }
  
  // FIXME: Only one query can be edited at a given time, i.e. only one person at a time
  // can create new golden queries. This is a HUGE limitation and it must be somehow fixed 
  // in the future. 
  public void newQuery(final String queryName, final String queryValue) {
    currentQuery = new RelevancyQuery(queryName, queryValue);
    queries.put(currentQuery.getName(),currentQuery);
  }

  public void addRelevantDoc(final String docId) {
    if (currentQuery != null) {
      currentQuery.addRelevantFile(docId);
    }
  }

  public void removeRelevantDoc(final String docId) {
    currentQuery.removeRelevantFile(docId);
  }
  
  public synchronized void updateParams(JSONObject configObject) {
    this.solrHost = (String) configObject.get(SOLR_HOST_KEY);
    this.solrCollection = (String) configObject.get(SOLR_COLLECTION_KEY);
    this.solrCloud = (Boolean) configObject.get(SOLR_CLOUD_KEY);
    this.idFieldName = (String) configObject.get(SOLR_ID_KEY);
    this.iterations = ((Number) configObject.get(ITERATIONS_KEY)).intValue();
    this.rows = ((Number) configObject.get(ROWS_KEY)).intValue();
    // Retrieving all the variable parameter defined in the config file
    JSONArray parameters = (JSONArray) configObject.get(PARAMETERS_KEY);
    Set<Integer> idSet = new HashSet<>();
    for (int i = 0; i < parameters.size(); i++) {
      try {
        RelevancyParameter newParam = new RelevancyParameter((JSONObject) parameters.get(i));
        if(this.parameters.containsKey(newParam.getId())) {
          RelevancyParameter currentParam = this.parameters.get(newParam.getId());
          currentParam.setMin(newParam.getMin());
          currentParam.setMax(newParam.getMax());
          currentParam.setName(newParam.getName());
          currentParam.setType(newParam.getType());
        } else {
          this.parameters.put(newParam.getId(), newParam);
        }
        idSet.add(newParam.getId());
      } catch (Exception e) {
        
      }
    }
    Set<Integer> keySet = new HashSet<Integer>(this.parameters.keySet());
    for (Integer id : keySet) {
      if (!idSet.contains(id)) {
        this.parameters.remove(id);
      }
    }
    
    JSONArray fixedParameters = (JSONArray) configObject.get(FIXED_PARAMETERS_KEY);
    idSet = new HashSet<>();
    for (int i = 0; i < fixedParameters.size(); i++) {
      try {
        RelevancyFixedParameter newParam = new RelevancyFixedParameter((JSONObject) fixedParameters.get(i));
        if(this.fixedParameters.containsKey(newParam.getId())) {
          RelevancyFixedParameter currentParam = this.fixedParameters.get(newParam.getId());
          currentParam.setValue(newParam.getValue());
          currentParam.setName(newParam.getName());
          currentParam.setType(newParam.getType());
        } else {
          this.fixedParameters.put(newParam.getId(), newParam);
        }
        idSet.add(newParam.getId());
      } catch (Exception e) {
        
      }
    }
    keySet = new HashSet<Integer>(this.fixedParameters.keySet());
    for (Integer id : keySet) {
      if (!idSet.contains(id)) {
        this.fixedParameters.remove(id);
      }
    }
  }

  public synchronized void saveSetup() throws IOException {

    String queriesString = "";
    String parametersString = "";
    
    queriesString = this.getQueriesJSONObject().toJSONString();
    parametersString = this.getParametersJSONObject().toJSONString();
    
    try (FileWriter queriesWriter = new FileWriter(goldenQueriesFile);
    FileWriter parametersWriter = new FileWriter(relevancySetupFile)) {
      queriesWriter.write(queriesString);
      parametersWriter.write(parametersString);
      queriesWriter.close();
      parametersWriter.close();
    }
  }

  public List<String> getRelevantDocsList(final String query) {
    final List<String> relevantDocsList = new ArrayList<>();
    if (currentQuery != null && currentQuery.getValue().equals(query)) {
      relevantDocsList.addAll(currentQuery.getRelevantFiles());
    }
    return relevantDocsList;
  }

}
