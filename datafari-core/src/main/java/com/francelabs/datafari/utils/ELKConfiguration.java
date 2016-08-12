/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class ELKConfiguration {

	// Properties
	public static final String ELK_ACTIVATION = "ELKactivation";
	public static final String KIBANA_URI = "KibanaURI";
	public static final String EXTERNAL_ELK_ON_OFF = "externalELK";
	public static final String ELK_SERVER = "ELKServer";
	public static final String ELK_SCRIPTS_DIR = "ELKScriptsDir";

	private static String configPropertiesFileName = "elk.properties";

	private static String configPropertiesFileNameRealPath;

	private static ELKConfiguration instance;
	private final Properties properties;

	private final static Logger LOGGER = Logger.getLogger(ELKConfiguration.class.getName());

	/**
	 * Set a property and save it the alerts.properties
	 *
	 * @param key
	 *            : the key that should be change
	 * @param value
	 *            : the new value of the key
	 * @return : true if there's an error and false if not
	 */
	public static synchronized boolean setProperty(final String key, final String value) {
		try {
			getInstance().properties.setProperty(key, value);
			final FileOutputStream fileOutputStream = new FileOutputStream(configPropertiesFileNameRealPath);
			instance.properties.store(fileOutputStream, null);
			fileOutputStream.close();
			return false;
		} catch (final IOException e) {
			LOGGER.error(e);
			return true;
		}
	}

	public static synchronized String getProperty(final String key) throws IOException {
		return (String) getInstance().properties.get(key);
	}

	/**
	 *
	 * Get the instance
	 *
	 */
	private static ELKConfiguration getInstance() throws IOException {
		if (null == instance) {
			instance = new ELKConfiguration();
		}
		return instance;
	}

	/**
	 *
	 * Read the properties file to get the parameters to create instance
	 *
	 */
	private ELKConfiguration() throws IOException {
		configPropertiesFileNameRealPath = System.getProperty("catalina.home") + File.separator + "conf" + File.separator + configPropertiesFileName;
		final File configFile = new File(configPropertiesFileNameRealPath);
		final InputStream stream = new FileInputStream(configFile);
		properties = new Properties();
		try {
			properties.load(stream);
		} catch (final IOException e) {
			LOGGER.error("Cannot read file : " + configFile.getAbsolutePath(), e);
			throw e;
		} finally {
			stream.close();
		}

	}

}