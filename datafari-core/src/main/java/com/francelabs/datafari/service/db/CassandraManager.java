package com.francelabs.datafari.service.db;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;

public class CassandraManager {

  private static CassandraManager instance;

  private final static Logger LOGGER = LogManager.getLogger(CassandraManager.class.getName());

  private static final String DEFAULT_KEYSPACE = "datafari";
  private static final String default_host = "127.0.0.1";
  private static final int default_port = 9042;
  private static final String cassandra_host_var = "CASSANDRA_HOST";
  private static final String cassandra_port_var = "CASSANDRA_PORT";
  private static final String cassandra_keyspace_var = "CASSANDRA_KEYSPACE";

  private CqlSessionBuilder sessionBuilder;
  private CqlSession session;

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
    if (session == null || session.isClosed()) {
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

    if (session != null) {
      session.close();
      session = null;
    }

    // Determine host
    String contactPoint = default_host;
    final String providedCassandraHost = System.getenv(cassandra_host_var);
    if (providedCassandraHost != null) {
      LOGGER.debug("Provided Cassandra host: " + providedCassandraHost);
      contactPoint = providedCassandraHost;
    } else {
      LOGGER.info("Cassandra host not provided, using default value: " + default_host);
    }

    // Determine port
    int cassandraPort = default_port;
    final String providedCassandraPort = System.getenv(cassandra_port_var);
    if (providedCassandraPort != null) {
      LOGGER.debug("Provided Cassandra port: " + providedCassandraPort);
      cassandraPort = Integer.valueOf(providedCassandraPort);
    } else {
      LOGGER.info("Cassandra port not provided, using default value: " + default_port);
    }

    // Determine keyspace
    String keyspace = DEFAULT_KEYSPACE;
    final String provided_keyspace = System.getenv(cassandra_keyspace_var);
    if (provided_keyspace != null) {
      LOGGER.debug("Provided Cassandra keyspace: " + provided_keyspace);
      keyspace = provided_keyspace;
    }

    sessionBuilder = new CqlSessionBuilder().addContactPoint(new InetSocketAddress(contactPoint, cassandraPort)).withKeyspace(keyspace).withLocalDatacenter("datacenter1");
    LOGGER.debug("Cassandra cluster successfully initialized");
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
        session = sessionBuilder.build();
        LOGGER.debug("Cassandra session successfully initialized");
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
    LOGGER.debug("Stopping Cassandra client");
    session.close();
    session = null;
    LOGGER.debug("Cassandra client stopped !");
  }

  public CqlSession getSession() {
    return session;
  }

}
