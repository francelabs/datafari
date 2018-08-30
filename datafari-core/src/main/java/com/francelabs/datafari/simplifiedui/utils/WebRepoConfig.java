package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class WebRepoConfig {

  private final File webRepoJSON;
  private static WebRepoConfig instance = null;
  private final static Logger logger = LogManager.getLogger(WebRepoConfig.class);

  private final static String repositoryConnectionElement = "repositoryconnection";
  private final static String configurationElement = "configuration";
  private final static String parameterElement = "_PARAMETER_";
  private final static String attributeNameElement = "_attribute_name";
  private final static String valueElement = "_value_";
  private final static String emailAttribute = "Email address";
  private final static String nameElement = "name";
  private final static String repoConnectionsCommand = "repositoryconnections";

  private WebRepoConfig() {
    final String filePath = Environment.getEnvironmentVariable("DATAFARI_HOME") + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "visilia" + File.separator
        + "repositoryconnections" + File.separator + "web.json";
    webRepoJSON = new File(filePath);
  }

  public static WebRepoConfig getInstance() {
    if (instance == null) {
      instance = new WebRepoConfig();
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  public String createRepoConnection(final WebRepository webRepo) {

    try {
      final JSONObject json = JSONUtils.readJSON(webRepoJSON);
      final JSONArray repositoryconnection = (JSONArray) json.get(repositoryConnectionElement);
      final JSONObject webRepoConnection = (JSONObject) repositoryconnection.get(0);
      final JSONObject configuration = (JSONObject) webRepoConnection.get(configurationElement);
      final JSONArray parameters = (JSONArray) configuration.get(parameterElement);
      for (int i = 0; i < parameters.size(); i++) {
        final JSONObject parameter = (JSONObject) parameters.get(i);
        if (parameter.get(attributeNameElement).toString().equals(emailAttribute)) {
          parameter.replace(valueElement, webRepo.getEmail());
        }
      }

      // Generate unique name to avoid mistakes in jobs
      String repoName = UUID.randomUUID().toString();
      repoName = repoName.replaceAll("-", "");
      if (repoName.length() > 32) {
        repoName = repoName.substring(0, 32);
      }
      webRepoConnection.replace(nameElement, repoName);

      ManifoldAPI.putConfig(repoConnectionsCommand, repoName, json);

      return repoName;
    } catch (final Exception e) {
      logger.error("FATAL ERROR", e);
      return null;
    }

  }

}
