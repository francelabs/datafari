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
package com.francelabs.datafari.service.indexer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.francelabs.datafari.service.indexer.solr.SolrIndexerInputDocument;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerQuery;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerServer;
import com.francelabs.datafari.utils.DatafariMainConfiguration;

public class IndexerServerManager {

  static String mainCollection ="FileShare";

  public enum Core {
    FILESHARE {
      @Override
      public String toString() {
        try {
          if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)!= null)
            mainCollection = DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return mainCollection;
      }
    },
    STATISTICS {
      @Override
      public String toString() {
        return "Statistics";
      }
    },
    PROMOLINK {
      @Override
      public String toString() {
        return "Promolink";
      }
    }
  }

  private static Map<String, IndexerServer> serversList = new HashMap<>();

  public static IndexerServer getIndexerServer(final Core core) throws Exception {
    return getIndexerServer(core.toString());
  }

  public static IndexerServer getIndexerServer(final String core) throws Exception {
    if (serversList.containsKey(core)) {
      return serversList.get(core);
    } else {
      final IndexerServer newServer = new SolrIndexerServer(core);
      serversList.put(core, newServer);
      return newServer;
    }
  }

  public static IndexerQuery createQuery() {
    return new SolrIndexerQuery();
  }

  public static IndexerInputDocument createDocument() {
    return new SolrIndexerInputDocument();
  }

}
