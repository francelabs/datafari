package com.francelabs.datafari.ldap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.francelabs.datafari.utils.Environment;

public class LdapConfig {

  private static final String JSON_CONF_FILE = "ldap-config.json";

  private static final Logger logger = LogManager.getLogger(LdapConfig.class);

  public static final String ATTR_CONNECTION_URL = "connectionURL";
  public static final String ATTR_CONNECTION_NAME = "connectionName";
  public static final String ATTR_CONNECTION_PW = "connectionPassword";
  public static final String ATTR_USER_FILTER = "userFilter";
  public static final String ATTR_DOMAIN_NAME = "userBase";

  public static List<LdapRealm> getActiveDirectoryRealms() {
    final List<LdapRealm> adrList = new ArrayList<>();
    String configPath = Environment.getEnvironmentVariable("CONFIG_HOME");
    if (configPath == null) {
      configPath = "/opt/datafari/tomcat/conf";
    }
    final String adConfFilePath = configPath + File.separator + JSON_CONF_FILE;
    final StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines(Paths.get(adConfFilePath), StandardCharsets.UTF_8)) {
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
    } catch (final IOException e) {
      logger.warn("Unable to read file " + adConfFilePath);
      return adrList;
    }
    final String adConfContent = contentBuilder.toString().trim();
    if (!adConfContent.isEmpty()) {
      final JSONParser parser = new JSONParser();
      try {
        final JSONArray array = (JSONArray) parser.parse(adConfContent);
        array.forEach(o -> {
          final JSONObject adr = (JSONObject) o;
          adrList.add(new LdapRealm(adr));
        });
      } catch (final Exception e) {
        logger.error("Content format of file " + adConfFilePath + " is unknown or not expected", e);
      }
    }
    return adrList;
  }

  public static void saveActiveDirectoryConfig(final List<LdapRealm> adRealms) {
    // Save conf as JSON
    final JSONArray adConfJsonArray = new JSONArray();
    adRealms.forEach(adrc -> {
      adConfJsonArray.add(adrc.toJson());
    });
    final String jsonStr = adConfJsonArray.toJSONString();
    String configPath = Environment.getEnvironmentVariable("CONFIG_HOME");
    if (configPath == null) {
      configPath = "/opt/datafari/tomcat/conf";
    }
    final File jsonConfFile = new File(configPath + File.separator + JSON_CONF_FILE);
    try {
      final FileWriter fw = new FileWriter(jsonConfFile, false);
      fw.append(jsonStr);
      fw.flush();
      fw.close();
    } catch (final IOException e) {
      logger.error("Unable to save ActiveDirectory config", e);
    }
  }

}
