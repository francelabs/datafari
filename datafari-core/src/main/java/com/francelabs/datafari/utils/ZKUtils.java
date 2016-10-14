package com.francelabs.datafari.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

public class ZKUtils {
	

	private final static Logger LOGGER = Logger.getLogger(ZKUtils.class
			.getName());

	public static void configZK(String scriptname, String configName) throws IOException {
		String datafari_home;
		String pathScript;
		String zkhosts = ScriptConfiguration.getProperty("SOLRHOSTS");
		datafari_home = Environment.getEnvironmentVariable("DATAFARI_HOME"); // Gets
																				// the
																				// directory
																				// of
																				// installation
																				// if
																				// in
																				// standard
																				// environment
		if (datafari_home == null) { // If in development environment
			datafari_home = ExecutionEnvironment.getDevExecutionEnvironment();
		}
		pathScript = datafari_home + "/bin/zkUtils/";

		String[] command = { "/bin/bash", pathScript + scriptname, datafari_home, zkhosts, configName };
		ProcessBuilder p = new ProcessBuilder(command);
		Process p2 = p.start();

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));

		BufferedReader stdError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));

		// read the output from the command
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			LOGGER.info(s);
		}

		// read any errors from the attempted command
		while ((s = stdError.readLine()) != null) {
			LOGGER.warn(s);
		}
		
		

	}

}