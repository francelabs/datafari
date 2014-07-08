package com.francelabs.datafari.utils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration reader
 * 
 * @author France Labs
 * 
 */
public class ScriptConfiguration {

	
	//TODO switch to relative path
	public static String configPropertiesFileName = "/opt/datafari/conf/datafari.properties";

	private static ScriptConfiguration instance;
	private Properties properties;

	public static void setProperty(String key, String value) throws IOException {
		getInstance().properties.setProperty(key, value);
		getInstance().properties.store(new FileOutputStream(configPropertiesFileName), null);
	}

	public static String getProperty(String key) throws IOException {
		return (String) getInstance().properties.get(key);
	}

	/**
	 * 
	 * Get the JDBC connection instance
	 * 
	 */
	private static ScriptConfiguration getInstance() throws IOException {
		if (null == instance) {
			instance = new ScriptConfiguration();
		}
		return instance;
	}

	/**
	 * 
	 * Read the properties file to get the parameters to create the JDBC
	 * connection instance
	 * 
	 */
	private ScriptConfiguration() throws IOException {
		properties = new Properties();
		properties.load(new BufferedReader(new FileReader(
				configPropertiesFileName)));
	}

}
