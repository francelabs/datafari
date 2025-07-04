package com.francelabs.datafari.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.SolrAtomicUpdateLauncher;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * This class instantiates the {@link AtomicUpdateConfig} object using the configuration file "atomicUpdate-cfg.json".
 * The configuration file is searched first in directory specified in MAIN_DATAFARI_CONFIG_HOME environment variable,
 * then in CONFIG_HOME and last in "/opt/datafari/tomcat/conf".
 */
public class ConfigLoader {
  private static AtomicUpdateConfig config = null;
  private static String configFileAbsolutePath;

  private ConfigLoader(){}
  public static AtomicUpdateConfig getConfig(){
    if (config == null){

      try {
        String filename = "atomicUpdate-cfg.json";
        configFileAbsolutePath = SolrAtomicUpdateLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int lastDirectoryIndex = configFileAbsolutePath.lastIndexOf(File.separator);
        configFileAbsolutePath = configFileAbsolutePath.substring(0,lastDirectoryIndex);
        config = new ObjectMapper().readValue(new File(configFileAbsolutePath + File.separator + filename), AtomicUpdateConfig.class);

        String logConfigFile = config.getLogConfigFile();
        if (StringUtils.isBlank(logConfigFile)){
          config.setLogConfigFile(configFileAbsolutePath + File.separator + "atomicUpdate-log4j2.xml");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }
    return config;
  }

  public static String getConfigFileAbsolutePath(){
    return configFileAbsolutePath;
  }
}
