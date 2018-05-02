package com.francelabs.datafari.service.db;

import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class CassandraManager {

  private static CassandraManager instance;

  private final static Logger LOGGER = Logger.getLogger(CassandraManager.class.getName());

  private static final String KEYSPACE = "datafari";
  private static final String host = "127.0.0.1";

  private Cluster cluster;
  private Session session;

  private static final int numRetries = 5;
  private static final int waitTimeBetweenEachRetry = 5000;

  private CassandraManager() {
    initCassandraClient();
  }

  public synchronized static CassandraManager getInstance() {
    if (instance == null) {
      instance = new CassandraManager();
    }
    if (!instance.isCassandraClientUp()) {
      instance.initCassandraClient();
    }
    return instance;
  }

  private boolean isCassandraClientUp() {
    if (cluster == null || session == null || session.isClosed() || cluster.isClosed()) {
      return false;
    } else {
      return true;
    }
  }

  private void initCassandraClient() {
    initCluster();
    initSession();
    LOGGER.info("Cassandra client successfully initialized");
  }

  private void initCluster() {
    if (cluster != null) {
      if (session != null) {
        session.close();
        session = null;
      }
      cluster.close();
      cluster = null;
    }
    cluster = Cluster.builder().addContactPoint(host).build();
    LOGGER.info("Cassandra cluster successfully initialized");
  }

  private void initSession() {
    int retryNum = 0;
    if (session != null) {
      session.close();
      session = null;
    }
    while (session == null && retryNum < numRetries) {
      try {
        // Connect to the cluster and keyspace "datafari"
        session = cluster.connect(KEYSPACE);
        LOGGER.info("Cassandra session successfully initialized");
      } catch (final Exception e) {
        retryNum++;
        session = null;
        if (retryNum < numRetries) {
          LOGGER.info("Waiting for Cassandra... Retry " + retryNum);
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
    session = null;
    cluster = null;
    LOGGER.info("Cassandra client successfully closed");
  }

  public Session getSession() {
    return session;
  }

}
