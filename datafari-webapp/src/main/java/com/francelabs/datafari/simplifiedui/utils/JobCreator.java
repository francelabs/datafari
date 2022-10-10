package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class JobCreator {

  private static JobCreator instance = null;

  private final File tikaServerRmetaConnectorJSON;
  private final File docFilterOCRStageJSON;

  private final static String jobElement = "job";
  private final static String descriptionElement = "description";
  private final static String pipelinestageElement = "pipelinestage";
  private final static String stageConnectionNameElement = "stage_connectionname";
  private final static String value = "_value_";
  private final static String type = "_type_";
  private final static String childrenElement = "_children_";
  private final static String stageIdElement = "stage_id";
  private final static String stagePrerequisiteElement = "stage_prerequisite";

  private final static String transfoConnectionElement = "transformationconnection";
  private final static String nameElement = "name";
  private final static String configurationElement = "configuration";
  private final static String parameterElement = "_PARAMETER_";
  private final static String attributeName = "_attribute_name";
  private final static String tikaHostAttr = "tikaHostname";
  private final static String tikaPortAttr = "tikaPort";
  private final static String originalTikaServerConnector = "TikaServerRmetaConnector";

  private final static Logger logger = LogManager.getLogger(JobCreator.class);

  private JobCreator() {
    String datafariHomePath = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHomePath == null) {
      // if no variable is set, use the default installation path
      datafariHomePath = "/opt/datafari";
    }

    final String tikaServerRmetaConnectorJSONPath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "init" + File.separator + "transformationconnections" + File.separator + "TikaServerRmetaConnector.json";
    tikaServerRmetaConnectorJSON = new File(tikaServerRmetaConnectorJSONPath);

    final String docFilterOCRStagePath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "simplifiedui" + File.separator + "utils" + File.separator + "docFilterOCRStage.json";
    docFilterOCRStageJSON = new File(docFilterOCRStagePath);

  }

  public static JobCreator getInstance() {
    if (instance == null) {
      instance = new JobCreator();
    }
    return instance;
  }

  /**
   * Create a new OCR job based on an existing job This mathod make a duplicate of the provided job and modify the duplicate so it will perform OCR
   *
   * @param originalJob the original job that will be used to create the OCR job
   * @param ocrJobName  the job name to set for the OCR job
   * @param tikaOCRName the name of the Tika Server connector that will be created
   * @param tikaOCRHost the hostname of the Tika server that is configured to perform OCR
   * @param tikaOCRPort the port of the Tika server that is configured to perform OCR
   * @throws Exception
   */
  public void createOCRJob(final JSONObject originalJob, final String ocrJobName, final String tikaOCRName, final String tikaOCRHost, final String tikaOCRPort) throws Exception {

    final JSONObject ocrJob = JSONUtils.cloneJSON(originalJob);

    // Prepare the Tika connector
    final JSONObject tikaServerJSONObj = JSONUtils.readJSON(tikaServerRmetaConnectorJSON);
    final JSONArray transfoConnection = (JSONArray) tikaServerJSONObj.get(transfoConnectionElement);
    final JSONObject transfoConnectionEl = (JSONObject) transfoConnection.get(0);

    // Set name
    transfoConnectionEl.put(nameElement, tikaOCRName);

    final JSONObject configurationEl = (JSONObject) transfoConnectionEl.get(configurationElement);
    final JSONArray parameterEl = (JSONArray) configurationEl.get(parameterElement);

    for (int i = 0; i < parameterEl.size(); i++) {
      final JSONObject parameter = (JSONObject) parameterEl.get(i);

      if (parameter.get(attributeName).equals(tikaHostAttr)) {
        // Set the tikaHost value
        parameter.put(value, tikaOCRHost);
      } else if (parameter.get(attributeName).equals(tikaPortAttr)) {
        // Set the tikaPort value
        parameter.put(value, tikaOCRPort);
      }

    }

    // Push the Tika connector to MCF
    try {
      ManifoldAPI.putConfig(ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS, tikaOCRName, tikaServerJSONObj);
    } catch (final Exception e) {
      if (e.getMessage().contains("Transformation connection '" + tikaOCRName + "' already exists")) {
        logger.warn("The Tika OCR connector to create already exists with the provided name '" + tikaOCRName + "'. The existing one will be used for the OCR job");
      } else {
        throw e;
      }
    }

    // Now replace the original Tika connector in a job by the new one
    final JSONArray job = (JSONArray) ocrJob.get(jobElement);
    final JSONObject jobEl = (JSONObject) job.get(0);
    final JSONArray jobChildrenEl = (JSONArray) jobEl.get(childrenElement);
    int originalTikaStageId = -1;
    int originalTikaPrereqId = -1;
    int originalTikaStageIndex = -1;

    for (int i = 0; i < jobChildrenEl.size(); i++) {
      final JSONObject jobChild = (JSONObject) jobChildrenEl.get(i);

      if (jobChild.get(type).equals(descriptionElement)) {
        // Set description
        jobChild.replace(value, ocrJobName);
      } else if (jobChild.get(type).equals(pipelinestageElement)) {
        int stageId = -1;
        int stagePrereqId = -1;
        final JSONArray children = (JSONArray) jobChild.get(childrenElement);
        for (int j = 0; j < children.size(); j++) {
          final JSONObject child = (JSONObject) children.get(j);
          if (child.get(type).equals(stageIdElement)) {
            stageId = Integer.parseInt((String) child.get(value));
          } else if (child.get(type).equals(stagePrerequisiteElement)) {
            stagePrereqId = Integer.parseInt((String) child.get(value));
          } else if (child.get(type).equals(stageConnectionNameElement) && child.get(value).equals(originalTikaServerConnector)) {
            // Replace the connector name by the new one
            child.put(value, tikaOCRName);
            // Save the tika stage id
            originalTikaStageId = stageId;
            originalTikaPrereqId = stagePrereqId;
            originalTikaStageIndex = i;
          }
        }
      }
    }

    // Prepare the DocFilterOCR stage
    final JSONObject docFilterOCROutputStage = JSONUtils.readJSON(docFilterOCRStageJSON);
    final JSONArray docFilterOCRStageChildrenEl = (JSONArray) docFilterOCROutputStage.get(childrenElement);
    for (int i = 0; i < docFilterOCRStageChildrenEl.size(); i++) {
      final JSONObject child = (JSONObject) docFilterOCRStageChildrenEl.get(i);
      if (child.get(type).equals(stageIdElement)) {
        child.replace(value, String.valueOf(originalTikaStageId));
      } else if (child.get(type).equals(stagePrerequisiteElement)) {
        child.replace(value, String.valueOf(originalTikaPrereqId));
      }
    }

    // Insert the docFilterOCR stage
    jobChildrenEl.add(originalTikaStageIndex, docFilterOCROutputStage);

    // Update stage id of stages following the docFilterOCR stage
    for (int i = originalTikaStageIndex + 1; i < jobChildrenEl.size(); i++) {
      final JSONObject jobChild = (JSONObject) jobChildrenEl.get(i);
      boolean isTikaStage = false;
      if (jobChild.get(type).equals(pipelinestageElement)) {
        final JSONArray children = (JSONArray) jobChild.get(childrenElement);
        for (int j = 0; j < children.size(); j++) {
          final JSONObject child = (JSONObject) children.get(j);
          if (child.get(type).equals(stageIdElement)) {
            int stageId = Integer.parseInt((String) child.get(value));
            if (stageId >= originalTikaStageId) {
              if (stageId == originalTikaStageId) {
                isTikaStage = true;
              }
              stageId++;
              child.replace(value, String.valueOf(stageId));
            }
          } else if (child.get(type).equals(stagePrerequisiteElement)) {
            int stagePrereqId = Integer.parseInt((String) child.get(value));
            if (stagePrereqId >= originalTikaStageId || isTikaStage) {
              stagePrereqId++;
              child.replace(value, String.valueOf(stagePrereqId));
            }
          }
        }
      }
    }

    // Push the job to MCF
    ManifoldAPI.postConfig(ManifoldAPI.COMMANDS.JOBS, ocrJob);
  }

}
