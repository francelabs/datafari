package com.francelabs.datafari.service.db;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Application Lifecycle Listener implementation class
 * CassandraDBContextListerner
 *
 */
@WebListener
public class DBContextListerner implements ServletContextListener {

	private static Object lock = new Object();

	private final static Logger LOGGER = Logger
			.getLogger(DBContextListerner.class.getName());

	private static final String KEYSPACE = "datafari";
	private static final String host = "127.0.0.1";

	private static Cluster cluster;
	private static Session session;

	public static Session getSession() {
		synchronized (lock) {
			if (session == null) {
				initDB();
			}
		}
		return session;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		synchronized (lock) {
			if (session == null) {
				initDB();
			}
		}

	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		cluster.close();
		LOGGER.info("Cassandra closed successfully");
	}

	private static void initDB() {
		try {
			// Connect to the cluster and keyspace "demo"
			cluster = Cluster.builder().addContactPoint(host).build();
			session = cluster.connect(KEYSPACE);
			LOGGER.info("Cassandra client initialized successfully");
		} catch (Exception e) {
			LOGGER.error("Error initializing Cassandra client", e);
		}
	}

}
