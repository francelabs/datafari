package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;

public class JobStageCreator {

  private static JobStageCreator instance = null;

  private final File docFilterStageJSON;
  private final File docFilterOCRStageJSON;
  private final File duplicatesOutputJSON;
  private final File spacyFastAPIStageJSON;

  private final static String value = "_value_";
  private final static String attribute_value = "_attribute_value";
  private final static String type = "_type_";
  private final static String childrenElement = "_children_";
  private final static String stageIdElement = "stage_id";
  private final static String stagePrerequisiteElement = "stage_prerequisite";
  private final static String stageConnectionNameElement = "stage_connectionname";
  private final static String stageSpecificationElement = "stage_specification";
  private final static String modelToUseElement = "modelToUse";
  private final static String endpointToUseElement = "endpointToUse";
  private final static String outputFieldPrefixElement = "outputFieldPrefix";
  
  private final static String DEFAULT_SPACY_ENDPOINT = "/split_detect_and_process/";
  private final static String DEFAULT_SPACY_PREFIX = "entity_";

  private JobStageCreator() {
    String datafariHomePath = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHomePath == null) {
      // if no variable is set, use the default installation path
      datafariHomePath = "/opt/datafari";
    }

    final String docFilterStagePath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "simplifiedui" + File.separator + "utils" + File.separator + "docFilterStage.json";
    docFilterStageJSON = new File(docFilterStagePath);

    final String docFilterOCRStagePath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "simplifiedui" + File.separator + "utils" + File.separator + "docFilterOCRStage.json";
    docFilterOCRStageJSON = new File(docFilterOCRStagePath);

    final String duplicatesOutputStagePath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "simplifiedui" + File.separator + "utils" + File.separator + "duplicatesOutputStage.json";
    duplicatesOutputJSON = new File(duplicatesOutputStagePath);

    final String spacyFastAPIStagePath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "simplifiedui" + File.separator + "utils" + File.separator + "spacyFastAPIStage.json";
    spacyFastAPIStageJSON = new File(spacyFastAPIStagePath);
  }

  public static JobStageCreator getInstance() {
    if (instance == null) {
      instance = new JobStageCreator();
    }
    return instance;
  }

  public JSONObject createDocFilterStage(final int stageId, final int prerequisiteStageId) throws IOException, ParseException {
    final JSONObject docFilterStage = JSONUtils.readJSON(docFilterStageJSON);
    final JSONArray docFilterStageChildrenEl = (JSONArray) docFilterStage.get(childrenElement);
    for (int i = 0; i < docFilterStageChildrenEl.size(); i++) {
      final JSONObject child = (JSONObject) docFilterStageChildrenEl.get(i);
      if (child.get(type).equals(stageIdElement)) {
        child.replace(value, String.valueOf(stageId));
      } else if (child.get(type).equals(stagePrerequisiteElement)) {
        child.replace(value, String.valueOf(prerequisiteStageId));
      }
    }
    return docFilterStage;
  }

  public JSONObject createDocFilterOCRStage(final int stageId, final int prerequisiteStageId) throws IOException, ParseException {
    final JSONObject docFilterOCRStage = JSONUtils.readJSON(docFilterOCRStageJSON);
    final JSONArray docFilterOCRStageChildrenEl = (JSONArray) docFilterOCRStage.get(childrenElement);
    for (int i = 0; i < docFilterOCRStageChildrenEl.size(); i++) {
      final JSONObject child = (JSONObject) docFilterOCRStageChildrenEl.get(i);
      if (child.get(type).equals(stageIdElement)) {
        child.replace(value, String.valueOf(stageId));
      } else if (child.get(type).equals(stagePrerequisiteElement)) {
        child.replace(value, String.valueOf(prerequisiteStageId));
      }
    }
    return docFilterOCRStage;
  }

  public JSONObject createSpacyFastAPIStage(final int stageId, final int prerequisiteStageId, final String stageConnectionName, final String modelToUse, final String endpointToUse,
      final String outputFieldPrefix) throws IOException, ParseException {
    final JSONObject spacyFastAPIStage = JSONUtils.readJSON(spacyFastAPIStageJSON);
    final JSONArray spacyFastAPIStageChildrenEl = (JSONArray) spacyFastAPIStage.get(childrenElement);
    for (int i = 0; i < spacyFastAPIStageChildrenEl.size(); i++) {
      final JSONObject child = (JSONObject) spacyFastAPIStageChildrenEl.get(i);
      if (child.get(type).equals(stageIdElement)) {
        child.replace(value, String.valueOf(stageId));
      } else if (child.get(type).equals(stagePrerequisiteElement)) {
        child.replace(value, String.valueOf(prerequisiteStageId));
      } else if (child.get(type).equals(stageConnectionNameElement)) {
        child.replace(value, String.valueOf(stageConnectionName));
      } else if (child.get(type).equals(stageSpecificationElement)) {
        final JSONArray stageSpecificationChildrenEl = (JSONArray) child.get(childrenElement);
        for (int j = 0; j < stageSpecificationChildrenEl.size(); j++) {
          final JSONObject stageChild = (JSONObject) stageSpecificationChildrenEl.get(j);
          if (stageChild.get(type).equals(modelToUseElement)) {
            stageChild.replace(attribute_value, String.valueOf(modelToUse));
          } else if (stageChild.get(type).equals(endpointToUseElement)) {
            String finalEndPointToUse = DEFAULT_SPACY_ENDPOINT;
            if(endpointToUse != null && !endpointToUse.isEmpty()) {
              finalEndPointToUse = endpointToUse;
            }
            stageChild.replace(attribute_value, finalEndPointToUse);
          } else if (stageChild.get(type).equals(outputFieldPrefixElement)) {
            String finalPrefixToUse = DEFAULT_SPACY_PREFIX;
            if(outputFieldPrefix != null && !outputFieldPrefix.isEmpty()) {
              finalPrefixToUse = outputFieldPrefix;
            }
            stageChild.replace(attribute_value, finalPrefixToUse);
          }
        }
      }
    }
    return spacyFastAPIStage;
  }

  public JSONObject createDuplicatesOutputStage(final int stageId, final int prerequisiteStageId) throws IOException, ParseException {
    final JSONObject duplicatesOutputStage = JSONUtils.readJSON(duplicatesOutputJSON);
    final JSONArray duplicatesStageChildrenEl = (JSONArray) duplicatesOutputStage.get(childrenElement);
    for (int i = 0; i < duplicatesStageChildrenEl.size(); i++) {
      final JSONObject child = (JSONObject) duplicatesStageChildrenEl.get(i);
      if (child.get(type).equals(stageIdElement)) {
        child.replace(value, String.valueOf(stageId));
      } else if (child.get(type).equals(stagePrerequisiteElement)) {
        child.replace(value, String.valueOf(prerequisiteStageId));
      }
    }
    return duplicatesOutputStage;
  }

}
