package com.francelabs.datafari.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.utils.Environment;

import java.io.File;
import java.io.IOException;

public class ConfigLoader {
  private static AtomicUpdateConfig config = null;

  private ConfigLoader(){}
  public static AtomicUpdateConfig getConfig(){
    if (config == null){

      String configFileAbsolutePath = Environment.getEnvironmentVariable("MAIN_DATAFARI_CONFIG_HOME");
      if (configFileAbsolutePath == null){
        configFileAbsolutePath = Environment.getEnvironmentVariable("CONFIG_HOME");
        if (configFileAbsolutePath == null){
          configFileAbsolutePath = "/opt/datafari/tomcat/conf";
        }
      }

      try {
        config = new ObjectMapper().readValue(new File(configFileAbsolutePath + File.separator + "atomicUpdate-cfg.json"), AtomicUpdateConfig.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return config;
  }
}
