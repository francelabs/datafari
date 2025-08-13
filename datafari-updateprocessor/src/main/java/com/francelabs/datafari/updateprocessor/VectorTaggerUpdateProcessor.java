/*******************************************************************************
 /*******************************************************************************
 *  * Copyright 2020 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.updateprocessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VectorTaggerUpdateProcessor extends UpdateRequestProcessor {
  private static final Logger LOGGER = LogManager.getLogger(VectorTaggerUpdateProcessor.class.getName());
  boolean enabled = false;
  String vectorField;

  public VectorTaggerUpdateProcessor(final SolrParams params, final UpdateRequestProcessor next) {
    super(next);
    if (params != null) {
      this.enabled = params.getBool("enabled", false);
      this.vectorField = params.get("outputField", "");
    }
  }

  @Override
  public void processAdd(AddUpdateCommand cmd) throws IOException {
    SolrInputDocument doc = cmd.getSolrInputDocument();

    if (!vectorField.isEmpty()) {
      Map<String, Object> addOperation = new HashMap<>();
      addOperation.put("add-distinct", vectorField);

      // Use atomic update syntax
      doc.addField("has_vector", addOperation);
    }

    super.processAdd(cmd);
  }

}
