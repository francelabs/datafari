package com.francelabs.datafari.initializers;

import com.francelabs.datafari.service.indexer.IndexerServerManager;

public class IndexerServersInitializer implements IInitializer {

  @Override
  public void initialize() {
    // TODO Auto-generated method stub

  }

  @Override
  public void shutdown() {
    IndexerServerManager.closeAllIndexerServers();
  }

}
