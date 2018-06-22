package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class FilerRepoConfig {

  private final File filerRepoJSON;
  private static FilerRepoConfig instance = null;
  private final static Logger logger = Logger.getLogger(FilerRepoConfig.class);

  private final static String repositoryConnectionElement = "repositoryconnection";
  private final static String configurationElement = "configuration";
  private final static String parameterElement = "_PARAMETER_";
  private final static String attributeNameElement = "_attribute_name";
  private final static String valueElement = "_value_";
  private final static String serverAttribute = "Server";
  private final static String userAttribute = "User Name";
  private final static String passwordAttribute = "Password";
  private final static String nameElement = "name";
  private final static String repoConnectionsCommand = "repositoryconnections";

  private FilerRepoConfig() {
    final String filePath = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "visilia"
        + File.separator + "repositoryconnections" + File.separator + "filer.json";
    filerRepoJSON = new File(filePath);
  }

  public static FilerRepoConfig getInstance() {
    if (instance == null) {
      instance = new FilerRepoConfig();
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  public String createRepoConnection(final FilerRepository filerRepo) {

    try {
      final JSONObject json = JSONUtils.readJSON(filerRepoJSON);
      final JSONArray repositoryconnection = (JSONArray) json.get(repositoryConnectionElement);
      final JSONObject filerRepoConnection = (JSONObject) repositoryconnection.get(0);
      final JSONObject configuration = (JSONObject) filerRepoConnection.get(configurationElement);
      final JSONArray parameters = (JSONArray) configuration.get(parameterElement);
      for (int i = 0; i < parameters.size(); i++) {
        final JSONObject parameter = (JSONObject) parameters.get(i);
        // Set the server
        if (parameter.get(attributeNameElement).toString().equals(serverAttribute)) {
          parameter.replace(valueElement, filerRepo.getServer());
        }

        // Set the user
        if (parameter.get(attributeNameElement).toString().equals(userAttribute)) {
          parameter.replace(valueElement, filerRepo.getUser());
        }

        // Set the password
        if (parameter.get(attributeNameElement).toString().equals(passwordAttribute)) {
          parameter.replace(valueElement, filerRepo.getPassword());
        }
      }

      // Generate unique name to avoid mistakes in jobs
      String repoName = UUID.randomUUID().toString();
      repoName = repoName.replaceAll("-", "");
      if (repoName.length() > 32) {
        repoName = repoName.substring(0, 32);
      }
      filerRepoConnection.replace(nameElement, repoName);

      ManifoldAPI.putConfig(repoConnectionsCommand, repoName, json);

      return repoName;
    } catch (final Exception e) {
      logger.error("FATAL ERROR", e);
      return null;
    }

  }

}
