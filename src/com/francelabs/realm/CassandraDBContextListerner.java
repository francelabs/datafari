package com.francelabs.realm;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

/**
 * Application Lifecycle Listener implementation class
 * CassandraDBContextListerner
 *
 */
@WebListener
public class CassandraDBContextListerner implements ServletContextListener {

	private final static Logger LOGGER = Logger.getLogger(CassandraDBContextListerner.class.getName());

	private static final String host = "127.0.0.1";
	private static final String KEYSPACE = "datafari";

	private static Cluster cluster = null;
	private static Session session = null;

	public static synchronized Session getSession() {

		try {
			if (cluster == null || cluster.isClosed()) {
				// Connect to the cluster and keyspace "demo"
				cluster = Cluster.builder().addContactPoint(host).build();
				LOGGER.info("Cassandra cluster initialized successfully");

			}
			if (session == null || session.isClosed()){
				session = cluster.connect(KEYSPACE);
				LOGGER.info("Cassandra" + KEYSPACE + " session initialized successfully");

			}
		} catch (Exception e) {
			LOGGER.error("Error initializing Cassandra client", e);
		}

		return session;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if (session != null) {
			session.close();
			LOGGER.info("Cassandra " + KEYSPACE + " session closed successfully");
		}

		if (cluster != null) {
			cluster.close();
			LOGGER.info("Cassandra cluster closed successfully");
		}

		LOGGER.warn("Cassandra cluster was not initialized");
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {

	}

}
