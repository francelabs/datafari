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

import java.io.IOException;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

public class AnonymiseIpUpdateProcessor extends UpdateRequestProcessor {
  private static final String CLIENT_IP = "client_ip";

  public AnonymiseIpUpdateProcessor(final SolrParams params, final UpdateRequestProcessor next) {
    super(next);
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException {
    final SolrInputDocument doc = cmd.getSolrInputDocument();

    // If the sourceField parameter is set and that the doc contains it, extract its value and replace the url field value by this one
    if (doc != null && !"".equals(doc.getFieldValue(CLIENT_IP))) {
      final String newIp = String.valueOf(doc.getFieldValue(CLIENT_IP)
              .toString()
              .hashCode());
      if (!"".equals(newIp) ) {
        doc.setField(CLIENT_IP, newIp);
      }
    }

    // VERY IMPORTANT ! without this line of code any other Update Processor declared AFTER this one in the conf WILL NOT EXECUTE
    super.processAdd(cmd);
  }
}
