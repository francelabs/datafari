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
package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;
import java.util.Random;

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
  private final static String securityElement = "security";
  private final static String attributeValue = "_attribute_value";
  private final static String attributeIndexable = "_attribute_indexable";
  private final static String attributeFilespec = "_attribute_filespec";
  private final static String attributeType = "_attribute_type";
  private final static String attributePath = "_attribute_path";
  private final static String value = "_value_";
  private final static String type = "_type_";
  private final static String startpointElement = "startpoint";
  private final static String childrenElement = "_children_";
  private final static String idElement = "id";
  private final static String jobsCommand = "jobs";

  private FilerJobConfig() {
    final String filePath = Environment.getEnvironmentVariable("DATAFARI_HOME") + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf"
        + File.separator + "simplifiedui" + File.separator + "jobs" + File.separator + "filer.json";
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

      // Set description
      final int randomInt = new Random().nextInt(10000);
      filerJobEl.replace(descriptionElement, "CrawlFiler-" + randomInt);

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
        final JSONArray childrenArray = new JSONArray();
        final JSONObject file = new JSONObject();
        final JSONObject directory = new JSONObject();

        // Create children for include and exclude rules
        // Create rules
        for (final FilerFilterRule filterRule : filerJob.getOrderedRules()) {
          final JSONObject newRule = new JSONObject();
          newRule.put(type, filterRule.getRuleType());
          newRule.put(attributeFilespec, filterRule.getFilterValue());
          newRule.put(value, "");
          newRule.put(attributeType, filterRule.getFilterType());
          childrenArray.add(newRule);
        }

        // Create include rules
        file.put(type, RuleType.INCLUDE.toString());
        file.put(attributeIndexable, "yes");
        file.put(attributeFilespec, "*");
        file.put(value, "");
        file.put(attributeType, FilterType.FILE.toString());
        childrenArray.add(file);

        directory.put(type, RuleType.INCLUDE.toString());
        directory.put(attributeFilespec, "*");
        directory.put(value, "");
        directory.put(attributeType, FilterType.DIRECTORY.toString());
        childrenArray.add(directory);

        path.put(childrenElement, childrenArray);
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
