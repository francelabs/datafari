package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class DbJobConfig {

  private final File dbJobJSON;
  private static DbJobConfig instance = null;
  private final static Logger logger = LogManager.getLogger(DbJobConfig.class);

  private final static String jobElement = "job";
  private final static String descriptionElement = "description";
  private final static String repositoryConnectionElement = "repository_connection";
  private final static String documentSpecificationElement = "document_specification";
  private final static String pipelinestageElement = "pipelinestage";
  private final static String stageConnectionNameElement = "stage_connectionname";
  private final static String stageSpecificationElement = "stage_specification";
  private final static String expressionElement = "expression";
  private final static String securityElement = "security";
  private final static String idQueryElement = "idquery";
  private final static String versionQueryElement = "versionquery";
  private final static String aclQueryElement = "aclquery";
  private final static String dataQueryElement = "dataquery";
  private final static String attributeValue = "_attribute_value";
  private final static String attributeParameter = "_attribute_parameter";
  private final static String value = "_value_";
  private final static String type = "_type_";
  private final static String childrenElement = "_children_";
  private final static String jobsCommand = "jobs";
  private final static String stageIdElement = "stage_id";
  private final static String stageIsOutputElement = "stage_isoutput";

  private DbJobConfig() {
    String datafariHome = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHome == null) {
      // if no variable is set, use the default installation path
      datafariHome = "/opt/datafari";
    }

    final String filePath = datafariHome + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "simplifiedui"
        + File.separator + "jobs" + File.separator + "db.json";
    dbJobJSON = new File(filePath);
  }

  public static DbJobConfig getInstance() {
    if (instance == null) {
      instance = new DbJobConfig();
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  public String createJob(final DbJob dbJob) throws Exception {
    boolean isPipelineStage = false;
    boolean isOutputStage = false;
    int stageId = 0;
    final JSONObject dbJobObj = JSONUtils.readJSON(dbJobJSON);
    final JSONArray dbJobArr = (JSONArray) dbJobObj.get(jobElement);
    final JSONObject dbJobEl = (JSONObject) dbJobArr.get(0);
    final JSONArray jobChildrenEl = (JSONArray) dbJobEl.get(childrenElement);
    JSONArray documentSpec = new JSONArray();
    JSONObject repoSource = null;

    // Stage id of the last transformation connector in the pipeline
    int lastTransfoPipelineStageId = 0;
    // Stage id of the last connector in the pipeline
    int lastPipelineStageId = 0;
    // JSONArray index of the last connector in the pipeline
    int lastPipelineStageIndex = 0;

    for (int i = 0; i < jobChildrenEl.size(); i++) {
      final JSONObject jobChild = (JSONObject) jobChildrenEl.get(i);
      if (jobChild.get(type).equals(repositoryConnectionElement)) {
        // Set repositoryName
        jobChild.replace(value, dbJob.getRepositoryConnection());
      }

      if (jobChild.get(type).equals(descriptionElement)) {
        // Set description
        jobChild.replace(value, "Crawl_" + dbJob.getRepositoryConnection());
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
    if (dbJob.isDuplicatesDetectionEnabled()) {
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

      if (docSpecChild.get(type).equals(securityElement)) {
        // Set security
        if (dbJob.isSecurity()) {
          docSpecChild.replace(attributeValue, "on");
        } else {
          docSpecChild.replace(attributeValue, "off");
        }
      }

      if (docSpecChild.get(type).equals(idQueryElement)) {
        // Set seeding query
        docSpecChild.replace(value, dbJob.getSeedingQ());
      }

      if (docSpecChild.get(type).equals(versionQueryElement)) {
        // Set version query
        docSpecChild.replace(value, dbJob.getVersionQ());
      }

      if (docSpecChild.get(type).equals(aclQueryElement)) {
        // Set access token query
        docSpecChild.replace(value, dbJob.getAccessTokenQ());
      }

      if (docSpecChild.get(type).equals(dataQueryElement)) {
        // Set data query
        docSpecChild.replace(value, dbJob.getDataQ());
      }
    }

    // Set sourcename
    if (repoSource != null) {
      repoSource.replace(attributeValue, dbJob.getSourcename());
    }

    // Set schedule task
    final JSONObject scheduleJobConf = JobScheduleCreator.getInstance().createDefaultJobSchedule(dbJob.getTimezone());
    jobChildrenEl.add(scheduleJobConf);

    // If OCR enable then create the corresponding OCR job
    if (dbJob.isOCREnabled()) {
      final String ocrJobName = "CrawlOCR_" + dbJob.getRepositoryConnection();
      JobCreator.getInstance().createOCRJob(dbJobObj, ocrJobName, dbJob.getTikaOCRName(), dbJob.getTikaOCRHost(), dbJob.getTikaOCRPort());
    }

    final JSONObject response = ManifoldAPI.postConfig(jobsCommand, dbJobObj);
    return response.get("job_id").toString();

  }

}
