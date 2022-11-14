package com.francelabs.datafari.simplifiedui.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class JobCreator {

  private static JobCreator instance = null;

  private final static String jobElement = "job";
  private final static String descriptionElement = "description";
  private final static String pipelinestageElement = "pipelinestage";
  private final static String stageConnectionNameElement = "stage_connectionname";
  private final static String value = "_value_";
  private final static String type = "_type_";
  private final static String childrenElement = "_children_";
  private final static String stageIdElement = "stage_id";
  private final static String stagePrerequisiteElement = "stage_prerequisite";
  private final static String stageIsOutputElement = "stage_isoutput";

  private final static String originalTikaServerConnector = "TikaServerRmetaConnector";

  private final static Logger logger = LogManager.getLogger(JobCreator.class);

  private JobCreator() {
    String datafariHomePath = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHomePath == null) {
      // if no variable is set, use the default installation path
      datafariHomePath = "/opt/datafari";
    }

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
   * @return
   * @throws Exception
   */
  public JSONObject createOCRJob(final JSONObject originalJob, final String ocrJobName, final String tikaOCRName, final String tikaOCRHost, final String tikaOCRPort) throws Exception {

    final JSONObject ocrJob = JSONUtils.cloneJSON(originalJob);

    // Prepare the Tika connector
    final JSONObject tikaServerJSONObj = ConnectorCreator.getInstance().createTikaRmetaConnector(tikaOCRName, tikaOCRHost, tikaOCRPort);

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
    final JSONObject docFilterOCROutputStage = JobStageCreator.getInstance().createDocFilterOCRStage(originalTikaStageId, originalTikaPrereqId);

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
    return ManifoldAPI.postConfig(ManifoldAPI.COMMANDS.JOBS, ocrJob);
  }

  /**
   *
   * @param originalJob        the original job that will be used to create the Spacy job
   * @param spacyJobName       the job name to set for the Spacy job
   * @param spacyConnectorName the name to set for the spacy connector
   * @param spacyServerAddress the address of the Spacy FastAPI server
   * @param modelToUse         the Spacy model to use (may be empty)
   * @param endpointToUse      the Spacy endpoint to use (leave empty if no specific endpoint must be used)
   * @param outputFieldPrefix  the prefix you want to use for the metadata that will be added to the document for the entities
   * @return the job creation JSON response
   * @throws Exception
   */
  public JSONObject createSpacyJob(final JSONObject originalJob, final String spacyJobName, final String spacyConnectorName, final String spacyServerAddress, final String modelToUse,
      final String endpointToUse, final String outputFieldPrefix) throws Exception {

    final JSONObject spacyJob = JSONUtils.cloneJSON(originalJob);

    // Prepare the Spacy connector
    final JSONObject spacyFastAPIJSONObj = ConnectorCreator.getInstance().createSpacyFastAPIConnector(spacyConnectorName, spacyServerAddress);

    // Push the Spacy connector to MCF
    try {
      ManifoldAPI.putConfig(ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS, spacyConnectorName, spacyFastAPIJSONObj);
    } catch (final Exception e) {
      if (e.getMessage().contains("Transformation connection '" + spacyConnectorName + "' already exists")) {
        logger.warn("The SpacyFastAPI connector to create already exists with the provided name '" + spacyConnectorName + "'. The existing one will be used for the Spacy job");
      } else {
        throw e;
      }
    }

    // Now put the spacy connector in the job pipeline
    final JSONArray job = (JSONArray) spacyJob.get(jobElement);
    final JSONObject jobEl = (JSONObject) job.get(0);
    final JSONArray jobChildrenEl = (JSONArray) jobEl.get(childrenElement);

    // Stage id of the last transformation connector in the pipeline
    int lastTransfoPipelineStageId = 0;
    // Stage id of the last transformation connector in the pipeline
    int lastTransfoPipelineStageIndex = 0;

    for (int i = 0; i < jobChildrenEl.size(); i++) {
      boolean isPipelineStage = false;
      boolean isOutputStage = false;
      int stageId = 0;
      final JSONObject jobChild = (JSONObject) jobChildrenEl.get(i);

      if (jobChild.get(type).equals(descriptionElement)) {
        // Set description
        jobChild.replace(value, spacyJobName);
      } else if (jobChild.get(type).equals(pipelinestageElement)) {
        isPipelineStage = true;
        int stagePrereqId = -1;
        final JSONArray children = (JSONArray) jobChild.get(childrenElement);
        for (int j = 0; j < children.size(); j++) {
          final JSONObject child = (JSONObject) children.get(j);
          if (child.get(type).equals(stageIdElement)) {
            stageId = Integer.parseInt((String) child.get(value));
          } else if (child.get(type).equals(stagePrerequisiteElement)) {
            stagePrereqId = Integer.parseInt((String) child.get(value));
          } else if (child.get(type).equals(stageIsOutputElement)) {
            isOutputStage = Boolean.parseBoolean((String) child.get(value));
          }
        }
      }

      // Save the lastTransfoPipelineStageId
      if (isPipelineStage && !isOutputStage && stageId > lastTransfoPipelineStageId) {
        lastTransfoPipelineStageId = stageId;
        lastTransfoPipelineStageIndex = i;
      }

    }

    // Prepare the spacyFastAPI stage
    final int spacyFastAPIStageId = lastTransfoPipelineStageId + 1;
    final int spacyFastAPIStageIndex = lastTransfoPipelineStageIndex + 1;
    final int spacyFastAPIStagePrerequisiteStageId = lastTransfoPipelineStageId;
    final JSONObject spacyFastAPIStage = JobStageCreator.getInstance().createSpacyFastAPIStage(spacyFastAPIStageId, spacyFastAPIStagePrerequisiteStageId, spacyConnectorName, modelToUse, endpointToUse,
        outputFieldPrefix);

    // Insert the spacyFastAPI stage
    jobChildrenEl.add(spacyFastAPIStageIndex, spacyFastAPIStage);

    // Update stage id of stages following the spacyFastAPI stage
    for (int i = spacyFastAPIStageIndex + 1; i < jobChildrenEl.size(); i++) {
      final JSONObject jobChild = (JSONObject) jobChildrenEl.get(i);
      if (jobChild.get(type).equals(pipelinestageElement)) {
        final JSONArray children = (JSONArray) jobChild.get(childrenElement);
        for (int j = 0; j < children.size(); j++) {
          final JSONObject child = (JSONObject) children.get(j);
          if (child.get(type).equals(stageIdElement)) {
            int stageId = Integer.parseInt((String) child.get(value));
            if (stageId >= spacyFastAPIStageId) {
              stageId++;
              child.replace(value, String.valueOf(stageId));
            }
          } else if (child.get(type).equals(stagePrerequisiteElement)) {
            int stagePrereqId = Integer.parseInt((String) child.get(value));
            if (stagePrereqId >= lastTransfoPipelineStageId) {
              stagePrereqId++;
              child.replace(value, String.valueOf(stagePrereqId));
            }
          }
        }
      }
    }

    // Push the job to MCF
    return ManifoldAPI.postConfig(ManifoldAPI.COMMANDS.JOBS, spacyJob);
  }

}
