/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.utils;

import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.francelabs.datafari.config.AbstractConfigClass;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class EntityAutocompleteConfiguration extends AbstractConfigClass {

  // Config filename
  private static final String configFilename = "entity-autocomplete.properties";

  // Properties
  public static final String AUTOCOMPLETE_SUGGESTERS = "AUTOCOMPLETESUGGESTERS";
  public static final String CATEGORIES = "CATEGORIES";
  public static final String ACTIVATED = "ACTIVATED";

  public static final String SUGGETER_ACTIVATED = "activated";
  public static final String SUGGETER_CATEGORY_KEY = "categoryKey";
  public static final String SUGGETER_SOLR_CORE = "solrCOre";

  private static EntityAutocompleteConfiguration instance;

  /**
   *
   * Get the instance
   *
   */
  public static synchronized EntityAutocompleteConfiguration getInstance() {
    if (null == instance) {
      instance = new EntityAutocompleteConfiguration();
    }
    return instance;
  }

  /**
   *
   * Read the properties file to get the parameters to create instance
   *
   */
  private EntityAutocompleteConfiguration() {

    super(configFilename, LogManager.getLogger(EntityAutocompleteConfiguration.class.getName()));
  }

  public void setAutocompleteForEntityActivated(String categoryKey, boolean activated) throws Exception {
    // Activation status of the global feature, starts off with the status passed
    Boolean globalActivation = activated;
    try {
      JSONParser parser = new JSONParser();
      final JSONArray suggesters = (JSONArray) parser.parse(this.getProperty(AUTOCOMPLETE_SUGGESTERS, "[]"));
      // Can't use forEach here as I want to use the non final activated variable in the loop.
      for(int i = 0; i < suggesters.size(); i++) {
        final JSONObject jsonSuggester = (JSONObject) suggesters.get(i);
        if (jsonSuggester.get(SUGGETER_CATEGORY_KEY) != null
            && jsonSuggester.get(SUGGETER_CATEGORY_KEY).equals(categoryKey)) {
          jsonSuggester.put(SUGGETER_ACTIVATED, activated + "");
        } else {
          // If another entity autocomplete is activated, we want the global setting to stay activated.
          globalActivation = globalActivation | Boolean.parseBoolean((String) jsonSuggester.get(SUGGETER_ACTIVATED));
        }

      }
      this.setProperty(AUTOCOMPLETE_SUGGESTERS, suggesters.toJSONString());
      this.setProperty(ACTIVATED, globalActivation + "");
      this.saveProperties();
    } catch (Exception e) {
      throw e;
    }
  }

}