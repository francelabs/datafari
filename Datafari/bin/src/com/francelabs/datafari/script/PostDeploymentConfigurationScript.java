package com.francelabs.datafari.script;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;
import com.francelabs.tomcat.WebAppStatus;
import java.lang.Runtime;
import java.lang.Process;

public class PostDeploymentConfigurationScript {

	private static String configPropertiesFileName = "/opt/datafari-server/bin/config/log4j.properties";

	private final static Logger LOGGER = Logger
			.getLogger(PostDeploymentConfigurationScript.class.getName());

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		try {
				PropertyConfigurator.configure(configPropertiesFileName);

				WebAppStatus.waitUntilSolrIsStarted();
				WebAppStatus.waitUntilManifoldCFIsStarted();

				LOGGER.info("Manifold and Solr are now started");

				if (ScriptConfiguration.getProperty("DeploymentState")
								.equals("INTERMEDIATE")) {
				postDeployDatafari();
				
				ScriptConfiguration.setProperty("DeploymentState", "DEPLOYED");

				LOGGER.info("Postdeployment finished");
			}

		} catch (Exception e) {
			LOGGER.fatal(e.getMessage());
			e.printStackTrace();
		}

	}

	private static void postDeployDatafari() throws Exception {
		ManifoldAPI.cleanAll();

		ManifoldAPI.putConfig(ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS,
				"DatafariSolr", JSONUtils.readJSON(new File("/opt/datafari-server/bin/config/manifoldcf/json/DatafariSolr.json")));

		LOGGER.info("Manifold configuration cleaned");
	}

}
