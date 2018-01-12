package com.francelabs.datafari.service.db;

import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class CassandraManager {

  private static CassandraManager instance;

  private final static Logger LOGGER = Logger.getLogger(CassandraManager.class.getName());

  private static final String KEYSPACE = "datafari";
  private static final String host = "127.0.0.1";

  private final Cluster cluster;
  private Session session;

  private static final int numRetries = 5;
  private static final int waitTimeBetweenEachRetry = 5000;

  private CassandraManager() {
    cluster = Cluster.builder().addContactPoint(host).build();
    initSession();
  }

  public synchronized static CassandraManager getInstance() {
    if (instance == null) {
      instance = new CassandraManager();
    }
    return instance;
  }

  private void initSession() {
    int retryNum = 0;

    while (session == null && retryNum < numRetries) {
      try {
        // Connect to the cluster and keyspace "datafari"
        session = cluster.connect(KEYSPACE);
        LOGGER.info("Cassandra client initialized successfully");
      } catch (final Exception e) {
        retryNum++;
        if (retryNum < numRetries) {
          LOGGER.info("Waiting for Cassandra...");
          try {
            Thread.sleep(waitTimeBetweenEachRetry);
          } catch (final InterruptedException e1) {
            LOGGER.error("Cannot wait for Cassandra", e1);
          }
        } else {
          LOGGER.error("Cannot connect to Cassandra", e);
        }
      }
    }
  }

  public void closeSession() {
    session.close();
    cluster.close();
    LOGGER.info("Cassandra closed successfully");
  }

  public Session getSession() {
    return session;
  }

}
