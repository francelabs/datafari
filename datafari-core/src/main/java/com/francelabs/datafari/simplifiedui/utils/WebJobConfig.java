package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class WebJobConfig {

  private final File webJobJSON;
  private static WebJobConfig instance = null;
  private final static Logger logger = LogManager.getLogger(WebJobConfig.class);

  private final static String jobElement = "job";
  private final static String repositoryConnectionElement = "repository_connection";
  private final static String descriptionElement = "description";
  private final static String documentSpecificationElement = "document_specification";
  private final static String excludesElement = "excludes";
  private final static String seedsElement = "seeds";
  private final static String idElement = "id";
  private final static String jobsCommand = "jobs";

  private WebJobConfig() {
    final String filePath = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "visilia"
        + File.separator + "jobs" + File.separator + "web.json";
    webJobJSON = new File(filePath);
  }

  public static WebJobConfig getInstance() {
    if (instance == null) {
      instance = new WebJobConfig();
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  public String createJob(final WebJob webJob) {

    try {
      final JSONObject json = JSONUtils.readJSON(webJobJSON);
      final JSONArray job = (JSONArray) json.get(jobElement);
      final JSONObject webJobEl = (JSONObject) job.get(0);

      // Set repositoryName
      webJobEl.replace(repositoryConnectionElement, webJob.getRepositoryConnection());

      // Set description
      final int randomInt = new Random().nextInt(10000);
      webJobEl.replace(descriptionElement, "CrawlWeb-" + randomInt);

      final JSONObject documentSpec = (JSONObject) webJobEl.get(documentSpecificationElement);
      // Set excludes
      documentSpec.replace(excludesElement, webJob.getExclusions());
      // Set seeds
      documentSpec.replace(seedsElement, webJob.getSeeds());

      // Generate unique id to avoid mistakes in jobs
      webJobEl.remove(idElement);

      final JSONObject response = ManifoldAPI.postConfig(jobsCommand, json);
      return response.get("job_id").toString();
    } catch (final Exception e) {
      logger.error("FATAL ERROR", e);
      return null;
    }

  }

}
