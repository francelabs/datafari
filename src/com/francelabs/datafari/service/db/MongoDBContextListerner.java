package com.francelabs.datafari.service.db;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;
import com.francelabs.datafari.utils.ScriptConfiguration;
import com.mongodb.MongoClient;

/**
 * Application Lifecycle Listener implementation class MongoDBContextListerner
 *
 */
@WebListener
public class MongoDBContextListerner implements ServletContextListener {

	private final static Logger LOGGER = Logger
			.getLogger(MongoDBContextListerner.class.getName());

	private static MongoClient client;
	
	public static MongoClient getInstance(){
		return client;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		client.close();
		LOGGER.info("MongoClient closed successfully");
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {

			// Gets the address of the host
			String host = ScriptConfiguration.getProperty("HOST");

			// Gets the port
			int port = Integer
					.parseInt(ScriptConfiguration.getProperty("PORT"));

			client = new MongoClient(host, port);
			LOGGER.info("Mongo client initialized successfully");
		} catch (Exception e) {
			LOGGER.error("Error initializing Mongo client", e);
		}
	}

}
