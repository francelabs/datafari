package com.francelabs.realm;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Application Lifecycle Listener implementation class CassandraDBContextListerner
 *
 */
@WebListener
public class CassandraDBContextListerner implements ServletContextListener {


	private final static Logger LOGGER = Logger
			.getLogger(CassandraDBContextListerner.class.getName());
	
	private static final String host = "127.0.0.1";

	private static final String KEYSPACE = "datafari";

	private static Cluster cluster;
	private static Session session;
	
	public static Session getSession(){
		while  (session == null){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOGGER.error("Unknow error");
			}
		}
		return session;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if (cluster != null){
		cluster.close();
		LOGGER.info("Cassandra closed successfully");
		}

		LOGGER.warn("Cassandra cluster was not initialized");
	}
	

	@Override
	public void contextInitialized(ServletContextEvent sce) {
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
