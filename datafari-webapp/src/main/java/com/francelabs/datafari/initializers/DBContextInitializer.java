package com.francelabs.datafari.initializers;

import com.francelabs.datafari.service.db.CassandraManager;

/**
 * Application Lifecycle Listener implementation class CassandraDBContextListener
 *
 */
public class DBContextInitializer implements IInitializer {

  @Override
  public void initialize() {
    // Nothing to do

  }

  @Override
  public void shutdown() {
    CassandraManager.getInstance().closeSession();

  }

}
