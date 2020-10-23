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

  private FilerJobConfig() {
    final String filePath = Environment.getEnvironmentVariable("DATAFARI_HOME") + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "simplifiedui" + File.separator + "jobs"
        + File.separator + "filer.json";
    filerJobJSON = new File(filePath);
  }

  public static FilerJobConfig getInstance() {
    if (instance == null) {
      instance = new FilerJobConfig();
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  public String createJob(final FilerJob filerJob) {

    try {
      final JSONObject json = JSONUtils.readJSON(filerJobJSON);
      final JSONArray job = (JSONArray) json.get(jobElement);
      final JSONObject filerJobEl = (JSONObject) job.get(0);
      final JSONArray jobChildrenEl = (JSONArray) filerJobEl.get(childrenElement);
      JSONArray documentSpec = new JSONArray();
      JSONObject repoSource = null;

      for (int i = 0; i < jobChildrenEl.size(); i++) {
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
              break;
            }

          }
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

      final JSONObject response = ManifoldAPI.postConfig(jobsCommand, json);
      return response.get("job_id").toString();
    } catch (final Exception e) {
      logger.error("FATAL ERROR", e);
      return null;
    }

  }

}
