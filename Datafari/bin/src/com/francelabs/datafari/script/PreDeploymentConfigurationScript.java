package com.francelabs.datafari.script;


import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class PreDeploymentConfigurationScript {

	private static String configPropertiesFileName = "/opt/datafari-server/bin/config/log4j.properties";

	private final static Logger LOGGER = Logger
			.getLogger(PreDeploymentConfigurationScript.class.getName());

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		try {
				PropertyConfigurator.configure(configPropertiesFileName);
				if (ScriptConfiguration.getProperty("DeploymentState").equals("INIT")){
				
					
					preDeployDatafari();
					

					ScriptConfiguration.setProperty("DeploymentState", "INTERMEDIATE");
					LOGGER.info("DeploymentState set to INTERMEDIATE");
				} 

				LOGGER.info("Predeployment finished");
			
		} catch (Exception e) {
			LOGGER.fatal(e.getMessage());
			e.printStackTrace();
		}
	}

	static public void preDeployDatafari() throws Exception {
	}


}
