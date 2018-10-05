package com.francelabs.datafari.service.db;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Application Lifecycle Listener implementation class
 * CassandraDBContextListerner
 *
 */
@WebListener
public class DBContextListerner implements ServletContextListener {

  @Override
  public void contextInitialized(final ServletContextEvent sce) {

  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    CassandraManager.getInstance().closeSession();
  }

}
