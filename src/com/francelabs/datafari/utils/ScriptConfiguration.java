package com.francelabs.datafari.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration reader
 * 
 * @author France Labs
 * 
 */
public class ScriptConfiguration {

	
	//TODO switch to relative path
	public static String configPropertiesFileName = "datafari.properties";

	private static ScriptConfiguration instance;
	private Properties properties;


	/*
	public static void setProperty(String key, String value) throws IOException {
		getInstance().properties.setProperty(key, value);
		getInstance().properties.store(new FileOutputStream(configPropertiesFileName), null);
	}*/

	public static String getProperty(String key) throws IOException {
		return (String) getInstance().properties.get(key);
	}

	/**
	 * 
	 * Get the instance
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
	 * Read the properties file to get the parameters to create  instance
	 * 
	 */
	private ScriptConfiguration() throws IOException {
		File configDir = new File(System.getProperty("catalina.base"), "conf");
		File configFile = new File(configDir, configPropertiesFileName);
		InputStream stream = new FileInputStream(configFile);
		properties = new Properties();
		properties.load(stream);
		stream.close();
		
	}

}
