package com.francelabs.datafari.searchcomp.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class LoadProperties {
	public static String configPropertiesFileName;
	public static String pathPropertiesFileName;
	private static LoadProperties instance;
	private Properties properties;

	public static String getPathPropertiesFileName() {
		return pathPropertiesFileName;
	}

	public static void setPathPropertiesFileName(String pathPropertiesFileName) {
		LoadProperties.pathPropertiesFileName = pathPropertiesFileName;
	}

	public static String getProperty(String key) throws IOException {
		return (String) getInstance().properties.get(key);
	}

	public static String getConfigPropertiesFileName() {
		return configPropertiesFileName;
	}

	public static void setConfigPropertiesFileName(String configPropertiesFileName) {
		LoadProperties.configPropertiesFileName = configPropertiesFileName;
	}

	private static LoadProperties getInstance() throws IOException {
		if (null == instance) {
			instance = new LoadProperties();
		}
		return instance;
	}

	private LoadProperties() throws IOException {
		File configFile = new File(pathPropertiesFileName + configPropertiesFileName);

		InputStream stream = new FileInputStream(configFile);
		this.properties = new Properties();
		this.properties.load(stream);
		stream.close();
	}
}
