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

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

public class VectorUpdateProcessorFactory extends UpdateRequestProcessorFactory {


  private SolrParams params = null;

  @Override
  public void init(final NamedList args) {
    // Retrieve the parameters if any
    if (args != null) {
      params = args.toSolrParams();
     
    }
  }

  @Override
  public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse rsp, final UpdateRequestProcessor next) {
    // Pass the parameters retrieved in the init (if any) to the update processor
    return new VectorUpdateProcessor(params, next);
  }

}
