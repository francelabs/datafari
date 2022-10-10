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

  private final static String value = "_value_";
  private final static String type = "_type_";
  private final static String childrenElement = "_children_";
  private final static String stageIdElement = "stage_id";
  private final static String stagePrerequisiteElement = "stage_prerequisite";

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
