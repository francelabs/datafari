package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class FilerJobConfig {

  private final File filerJobJSON;
  private static FilerJobConfig instance = null;
  private final static Logger logger = Logger.getLogger(FilerJobConfig.class);

  private final static String jobElement = "job";
  private final static String repositoryConnectionElement = "repository_connection";
  private final static String documentSpecificationElement = "document_specification";
  private final static String securityElement = "security";
  private final static String attributeValue = "_attribute_value";
  private final static String attributeIndexable = "_attribute_indexable";
  private final static String attributeFilespec = "_attribute_filespec";
  private final static String attributeType = "_attribute_type";
  private final static String attributePath = "_attribute_path";
  private final static String value = "_value_";
  private final static String includeElement = "include";
  private final static String startpointElement = "startpoint";
  private final static String idElement = "id";
  private final static String jobsCommand = "jobs";

  private FilerJobConfig() {
    final String filePath = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "visilia"
        + File.separator + "jobs" + File.separator + "filer.json";
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

      // Set repositoryName
      filerJobEl.replace(repositoryConnectionElement, filerJob.getRepositoryConnection());
      final JSONObject documentSpec = (JSONObject) filerJobEl.get(documentSpecificationElement);

      // Set security
      if (filerJob.isSecurity()) {
        final JSONObject security = (JSONObject) documentSpec.get(securityElement);
        security.replace(attributeValue, "on");
      }
      // Set paths
      final String[] paths = filerJob.getPaths().split("\n");
      final JSONArray pathsArray = new JSONArray();
      for (int i = 0; i < paths.length; i++) {
        final JSONObject path = new JSONObject();
        final JSONArray includeArray = new JSONArray();
        final JSONObject file = new JSONObject();
        final JSONObject directory = new JSONObject();
        file.put(attributeIndexable, "yes");
        file.put(attributeFilespec, "*");
        file.put(value, "");
        file.put(attributeType, "file");
        includeArray.add(file);

        directory.put(attributeFilespec, "*");
        directory.put(value, "");
        directory.put(attributeType, "directory");
        includeArray.add(directory);

        path.put(includeElement, includeArray);
        path.put(attributePath, paths[i]);
        path.put(value, "");

        pathsArray.add(path);
      }
      documentSpec.replace(startpointElement, pathsArray);

      // Generate unique id to avoid mistakes in jobs
      filerJobEl.remove(idElement);

      final JSONObject response = ManifoldAPI.postConfig(jobsCommand, json);
      return response.get("job_id").toString();
    } catch (final Exception e) {
      logger.error("FATAL ERROR", e);
      return null;
    }

  }

}
