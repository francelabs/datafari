package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class DbRepoConfig {

  private final File dbRepoJSON;
  private final File authorityGroupTemplateJSON;
  private static DbRepoConfig instance = null;
  private final static Logger logger = LogManager.getLogger(DbRepoConfig.class);

  private final static String authorityGroupElement = "authoritygroup";
  private final static String repositoryConnectionElement = "repositoryconnection";
  private final static String childrenElement = "_children_";
  private final static String typeElement = "_type_";
  private final static String configurationElement = "configuration";
  private final static String parameterElement = "_PARAMETER_";
  private final static String attributeNameElement = "_attribute_name";
  private final static String valueElement = "_value_";
  private final static String jdbcProviderAttribute = "JDBC Provider";
  private final static String hostAttribute = "Host";
  private final static String dbNameAttribute = "Database name";
  private final static String accessMethodAttribute = "JDBC column access method";
  private final static String connectionStrAttribute = "Raw driver string";
  private final static String userAttribute = "User Name";
  private final static String passwordAttribute = "Password";
  private final static String nameElement = "name";
  private final static String repoConnectionsCommand = "repositoryconnections";

  private DbRepoConfig() {
    String datafariHome = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHome == null) {
      // if no variable is set, use the default installation path
      datafariHome = "/opt/datafari";
    }

    final String filePath = datafariHome + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "simplifiedui"
        + File.separator + "repositoryconnections" + File.separator + "db.json";
    dbRepoJSON = new File(filePath);

    // AuthorityGroup template
    final String authorityGroupTemplateJSONPath = datafariHome + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "init" + File.separator + "authoritygroups" + File.separator + "DatafariAuthorityGroup.json";
    authorityGroupTemplateJSON = new File(authorityGroupTemplateJSONPath);
  }

  public static DbRepoConfig getInstance() {
    if (instance == null) {
      instance = new DbRepoConfig();
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  public String createRepoConnection(final DbRepository dbRepo) throws Exception {
    final String repoName = dbRepo.getReponame();
    final String authorityGroupName = repoName + "AuthorityGroup";
    // Create the authorityGroup
    final JSONObject dbAuthorityGroupJson = JSONUtils.readJSON(authorityGroupTemplateJSON);
    final JSONArray dbAuthorityGroupArr = (JSONArray) dbAuthorityGroupJson.get(authorityGroupElement);
    final JSONObject dbAuthorityGroup = (JSONObject) dbAuthorityGroupArr.get(0);
    final JSONArray dbAuthorityGroupChildren = (JSONArray) dbAuthorityGroup.get(childrenElement);
    for (int i = 0; i < dbAuthorityGroupChildren.size(); i++) {
      final JSONObject authorityConfNode = (JSONObject) dbAuthorityGroupChildren.get(i);
      if (authorityConfNode.get(typeElement).toString().contentEquals(nameElement)) {
        authorityConfNode.replace(valueElement, authorityGroupName);
        break;
      }
    }

    ManifoldAPI.putConfig(ManifoldAPI.COMMANDS.AUTHORITYGROUPS, authorityGroupName, dbAuthorityGroupJson);
    final JSONObject dbRepoJson = JSONUtils.readJSON(dbRepoJSON);
    final JSONArray repositoryconnection = (JSONArray) dbRepoJson.get(repositoryConnectionElement);
    final JSONObject dbRepoConnection = (JSONObject) repositoryconnection.get(0);
    final JSONArray children = (JSONArray) dbRepoConnection.get(childrenElement);
    for (int i = 0; i < children.size(); i++) {
      final JSONObject childNode = (JSONObject) children.get(i);
      final String nodeType = childNode.get(typeElement).toString();
      if (nodeType.contentEquals("name")) {
        // Set the repoName
        childNode.replace(valueElement, repoName);
      } else if (nodeType.contentEquals(configurationElement)) {
        final JSONArray parameters = (JSONArray) childNode.get(parameterElement);

        for (int j = 0; j < parameters.size(); j++) {
          final JSONObject parameterNode = (JSONObject) parameters.get(j);
          final String parameterName = parameterNode.get(attributeNameElement).toString();

          // Set the jdbc provider
          if (parameterName.equals(jdbcProviderAttribute)) {
            parameterNode.replace(valueElement, dbRepo.getType());
          }

          // Set the host
          if (parameterName.equals(hostAttribute)) {
            parameterNode.replace(valueElement, dbRepo.getHost());
          }

          // Set the database name
          if (parameterName.equals(dbNameAttribute)) {
            parameterNode.replace(valueElement, dbRepo.getDbName());
          }

          // Set the raw connection string
          if (parameterName.equals(connectionStrAttribute)) {
            parameterNode.replace(valueElement, dbRepo.getConnectionStr());
          }

          // Set the username
          if (parameterName.equals(userAttribute)) {
            parameterNode.replace(valueElement, dbRepo.getUser());
          }

          // Set the password
          if (parameterName.equals(passwordAttribute)) {
            parameterNode.replace(valueElement, dbRepo.getPassword());
          }
        }
      } else if (nodeType.contentEquals("acl_authority")) {
        childNode.replace(valueElement, authorityGroupName);
      }
    }

    ManifoldAPI.putConfig(repoConnectionsCommand, repoName, dbRepoJson);

    return repoName;

  }

}
