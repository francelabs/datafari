package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.simplifiedui.utils.FilerFilterRule.FilterType;
import com.francelabs.datafari.simplifiedui.utils.FilerFilterRule.RuleType;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class FilerJobConfig {

  private final File filerJobJSON;
  private static FilerJobConfig instance = null;
  private final static Logger logger = LogManager.getLogger(FilerJobConfig.class);

  private final static String jobElement = "job";
  private final static String descriptionElement = "description";
  private final static String repositoryConnectionElement = "repository_connection";
  private final static String documentSpecificationElement = "document_specification";
  private final static String pipelinestageElement = "pipelinestage";
  private final static String stageConnectionNameElement = "stage_connectionname";
  private final static String stageSpecificationElement = "stage_specification";
  private final static String expressionElement = "expression";
  private final static String securityElement = "security";
  private final static String attributeValue = "_attribute_value";
  private final static String attributeParameter = "_attribute_parameter";
  private final static String attributeIndexable = "_attribute_indexable";
  private final static String attributeFilespec = "_attribute_filespec";
  private final static String attributeType = "_attribute_type";
  private final static String attributePath = "_attribute_path";
  private final static String value = "_value_";
  private final static String type = "_type_";
  private final static String startpointElement = "startpoint";
  private final static String includeElement = "include";
  private final static String childrenElement = "_children_";
  private final static String jobsCommand = "jobs";
  private final static String stageIdElement = "stage_id";
  private final static String stageIsOutputElement = "stage_isoutput";

  private FilerJobConfig() {
    final String filePath = Environment.getEnvironmentVariable("DATAFARI_HOME") + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf"
        + File.separator + "simplifiedui" + File.separator + "jobs" + File.separator + "filer.json";
    filerJobJSON = new File(filePath);
  }

  public static FilerJobConfig getInstance() {
    if (instance == null) {
      instance = new FilerJobConfig();
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  public String createJob(final FilerJob filerJob) throws Exception {

    final JSONObject filerJobObj = JSONUtils.readJSON(filerJobJSON);
    final JSONArray filerJobArr = (JSONArray) filerJobObj.get(jobElement);
    final JSONObject filerJobEl = (JSONObject) filerJobArr.get(0);
    final JSONArray jobChildrenEl = (JSONArray) filerJobEl.get(childrenElement);
    JSONArray documentSpec = new JSONArray();
    JSONObject repoSource = null;

    // Stage id of the last transformation connector in the pipeline
    int lastTransfoPipelineStageId = 0;
    // Stage id of the last connector in the pipeline
    int lastPipelineStageId = 0;
    // JSONArray index of the last connector in the pipeline
    int lastPipelineStageIndex = 0;

    for (int i = 0; i < jobChildrenEl.size(); i++) {
      boolean isPipelineStage = false;
      boolean isOutputStage = false;
      int stageId = 0;
      final JSONObject jobChild = (JSONObject) jobChildrenEl.get(i);
      if (jobChild.get(type).equals(repositoryConnectionElement)) {
        // Set repositoryName
        jobChild.replace(value, filerJob.getRepositoryConnection());
      }

      if (jobChild.get(type).equals(descriptionElement)) {
        // Set description
        jobChild.replace(value, "Crawl_" + filerJob.getRepositoryConnection());
      }

      if (jobChild.get(type).equals(documentSpecificationElement)) {
        // Get document spec element
        documentSpec = (JSONArray) jobChild.get(childrenElement);
      }

      boolean metadataAdjuster = false;
      if (jobChild.get(type).equals(pipelinestageElement)) {
        isPipelineStage = true;
        final JSONArray children = (JSONArray) jobChild.get(childrenElement);
        for (int j = 0; j < children.size(); j++) {
          final JSONObject child = (JSONObject) children.get(j);
          if (child.get(type).equals(stageConnectionNameElement) && child.get(value).equals("MetadataAdjuster")) {
            metadataAdjuster = true;
          } else if (child.get(type).equals(stageSpecificationElement) && metadataAdjuster) {
            final JSONArray metadataChildren = (JSONArray) child.get(childrenElement);
            for (int k = 0; k < metadataChildren.size(); k++) {
              final JSONObject metadataChild = (JSONObject) metadataChildren.get(k);
              if (metadataChild.get(type).equals(expressionElement) && metadataChild.get(attributeParameter).equals("repo_source")) {
                repoSource = metadataChild;
                break;
              }
            }
            metadataAdjuster = false;
          } else if (child.get(type).equals(stageIdElement)) {
            stageId = Integer.parseInt((String) child.get(value));
          } else if (child.get(type).equals(stageIsOutputElement)) {
            isOutputStage = Boolean.parseBoolean((String) child.get(value));
          }

        }
      }

      // Save the lastTransfoPipelineStageId
      if (isPipelineStage && !isOutputStage && stageId > lastTransfoPipelineStageId) {
        lastTransfoPipelineStageId = stageId;
      }

      // Save the index and stage id of the last pipeline stage
      if (isPipelineStage && stageId > lastPipelineStageId) {
        lastPipelineStageId = stageId;
        lastPipelineStageIndex = i;
      }
    }

    // Sets the docFilter and duplicatesOutput stages if the duplicates detection is enabled
    if (filerJob.isDuplicatesDetectionEnabled()) {
      final int docFilterStageId = lastPipelineStageId + 1;
      final int docFilterStagePrereqId = lastTransfoPipelineStageId;
      final int duplicatesOutputStageId = docFilterStageId + 1;
      final int duplicatesOutputStagePrereqId = docFilterStageId;

      final JSONObject docFilterOutputStage = JobStageCreator.getInstance().createDocFilterStage(docFilterStageId, docFilterStagePrereqId);
      final JSONObject duplicatesOutputStage = JobStageCreator.getInstance().createDuplicatesOutputStage(duplicatesOutputStageId, duplicatesOutputStagePrereqId);

      // Insertion in JSONARRAY at a specific index shifts the elements starting at the specified position to the right (add one to their indice)
      // Thus we need to first insert the last element which is the duplicate output, then we insert the docfFilter transfo as it is before the duplicate output in the pipeline
      if (duplicatesOutputStage != null && docFilterOutputStage != null) {
        jobChildrenEl.add(lastPipelineStageIndex + 1, duplicatesOutputStage);
        jobChildrenEl.add(lastPipelineStageIndex + 1, docFilterOutputStage);
      }
    }

    for (int i = 0; i < documentSpec.size(); i++) {
      final JSONObject docSpecChild = (JSONObject) documentSpec.get(i);

      if (docSpecChild.get(type).equals(securityElement) && filerJob.isSecurity()) {
        // Set security
        docSpecChild.replace(attributeValue, "on");
      }
    }

    // Set paths
    final String[] paths = filerJob.getPaths().split("\n");
    for (int i = 0; i < paths.length; i++) {
      final JSONObject path = new JSONObject();
      final JSONArray include = new JSONArray();
      final JSONObject file = new JSONObject();
      final JSONObject directory = new JSONObject();

      path.put(type, startpointElement);

      // Create include rules
      file.put(attributeIndexable, "yes");
      file.put(attributeFilespec, "*");
      file.put(value, "");
      file.put(attributeType, FilterType.FILE.toString());
      include.add(file);

      directory.put(type, RuleType.INCLUDE.toString());
      directory.put(attributeFilespec, "*");
      directory.put(value, "");
      directory.put(attributeType, FilterType.DIRECTORY.toString());
      include.add(directory);

      path.put(includeElement, include);
      path.put(attributePath, paths[i]);
      path.put(value, "");

      documentSpec.add(path);
    }

    // Set sourcename
    if (repoSource != null) {
      repoSource.replace(attributeValue, filerJob.getSourcename());
    }

    // Set schedule task
    final JSONObject scheduleJobConf = JobScheduleCreator.getInstance().createDefaultJobSchedule(filerJob.getTimezone());
    jobChildrenEl.add(scheduleJobConf);

    // If OCR enable then create the corresponding OCR job
    if (filerJob.isOCREnabled()) {
      final String ocrJobName = "CrawlOCR_" + filerJob.getRepositoryConnection();
      JobCreator.getInstance().createOCRJob(filerJobObj, ocrJobName, filerJob.getTikaOCRName(), filerJob.getTikaOCRHost(), filerJob.getTikaOCRPort());
    }

    final JSONObject response = ManifoldAPI.postConfig(jobsCommand, filerJobObj);
    return response.get("job_id").toString();

  }

}
