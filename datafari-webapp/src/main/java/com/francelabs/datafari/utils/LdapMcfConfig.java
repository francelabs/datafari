/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class LdapMcfConfig {
  private static File authorityconnectionJSON;
  private static LdapMcfConfig instance;
  public final static String autorityConnectionElement = "authorityconnection";
  public final static String autorityGroupElement = "authoritygroup";
  public final static String configurationElement = "configuration";
  public final static String domainControllerElement = "domaincontroller";
  public final static String attributeUsername = "_attribute_username";
  public final static String attributeDomainController = "_attribute_domaincontroller";
  public final static String attributePassword = "_attribute_password";
  public final static String attributeSuffix = "_attribute_suffix";
  private final static Logger logger = LogManager.getLogger(LdapMcfConfig.class);

  public static int update(final HashMap<String, String> h) {
    try {
      final JSONObject json = JSONUtils.readJSON(getInstance().authorityconnectionJSON);

      if (h.containsKey(attributeUsername) && h.containsKey(attributeDomainController) && h.containsKey(attributePassword)) {
        final JSONObject authorityconnection = (JSONObject) json.get(autorityConnectionElement);
        final JSONObject configuration = (JSONObject) authorityconnection.get(configurationElement);
        final JSONObject domainController = (JSONObject) configuration.get(domainControllerElement);
        String username = h.get(attributeUsername);
        if (username.indexOf("@") != -1) {
          username = username.substring(0, username.indexOf("@"));
        }
        domainController.put(attributeUsername, username);
        domainController.put(attributeDomainController, h.get(attributeDomainController));
        domainController.put(attributePassword, h.get(attributePassword));

        JSONUtils.saveJSON(json, authorityconnectionJSON);

        try {
          ManifoldAPI.deleteConfig("authorityconnections", "DatafariAD");
        } catch (final Exception e) {
          logger.error("Unable to delete DatafariAD", e);
        }

        ManifoldAPI.putConfig("authorityconnections", "DatafariAD", json);

        return CodesReturned.ALLOK.getValue();
      } else {
        return CodesReturned.PARAMETERNOTWELLSET.getValue();
      }
    } catch (final IOException e) {
      logger.error("FATAL ERROR", e);
      return CodesReturned.GENERALERROR.getValue();
    } catch (final Exception e) {
      logger.error("FATAL ERROR", e);
      return CodesReturned.GENERALERROR.getValue();
    }
  }

  private static LdapMcfConfig getInstance() {
    if (instance == null) {
      return instance = new LdapMcfConfig();
    }
    return instance;
  }

  private LdapMcfConfig() {
    // TODO : change path for dev environment
    final String filePath = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "monoinstance"
        + File.separator + "authorityconnections" + File.separator + "authorityConnection.json";

    authorityconnectionJSON = new File(filePath);
  }
}
