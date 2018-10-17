package com.francelabs.datafari.service.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class CassandraManager {

  private static CassandraManager instance;

  private final static Logger LOGGER = LogManager.getLogger(CassandraManager.class.getName());

  private static final String DEFAULT_KEYSPACE = "datafari";
  private static final String default_host = "127.0.0.1";
  private static final int default_port = 9042;
  private static final String cassandra_host_var = "CASSANDRA_HOST";
  private static final String cassandra_port_var = "CASSANDRA_PORT";
  private static final String cassandra_keyspace_var = "CASSANDRA_KEYSPACE";

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

    // Determine host
    String contactPoint = default_host;
    final String providedCassandraHost = System.getenv(cassandra_host_var);
    if (providedCassandraHost != null) {
      LOGGER.info("Provided Cassandra host: " + providedCassandraHost);
      contactPoint = providedCassandraHost;
    } else {
      LOGGER.info("Cassandra host not provided, using default value: " + default_host);
    }

    // Determine port
    int cassandraPort = default_port;
    final String providedCassandraPort = System.getenv(cassandra_port_var);
    if (providedCassandraPort != null) {
      LOGGER.info("Provided Cassandra port: " + providedCassandraPort);
      cassandraPort = Integer.valueOf(providedCassandraPort);
    } else {
      LOGGER.info("Cassandra port not provided, using default value: " + default_port);
    }

    cluster = Cluster.builder().addContactPoint(contactPoint).withPort(cassandraPort).build();
    LOGGER.info("Cassandra cluster successfully initialized");
  }

  private void initSession() {
    int retryNum = 0;
    if (session != null) {
      session.close();
      session = null;
    }
    String keyspace = DEFAULT_KEYSPACE;
    final String provided_keyspace = System.getenv(cassandra_keyspace_var);
    if (provided_keyspace != null) {
      LOGGER.info("Provided Cassandra keyspace: " + provided_keyspace);
      keyspace = provided_keyspace;
    }
    while (session == null && retryNum < numRetries) {
      try {
        // Connect to the cluster and keyspace "datafari"
        session = cluster.connect(keyspace);
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
