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
public class AlertsConfiguration {

	// Properties frequencies
	public final static String HOURLY_DELAY = "HOURLYDELAY";
	public final static String DAILY_DELAY = "DAILYDELAY";
	public final static String WEEKLY_DELAY = "WEEKLYDELAY";
	public final static String LAST_HOURLY_EXEC = "Hourly";
	public final static String LAST_DAILY_EXEC = "Daily";
	public final static String LAST_WEEKLY_EXEC = "Weekly";
	public final static String ALERTS_ON_OFF = "ALERTS";

	// Properties mails
	public final static String SMTP_ADDRESS = "smtp";
	public final static String SMTP_FROM = "from";
	public final static String SMTP_USER = "user";
	public final static String SMTP_PASSWORD = "pass";

	// Properties Database
	public final static String DATABASE_HOST = "HOST";
	public final static String DATABASE_PORT = "PORT";
	public final static String DATABASE_NAME = "DATABASE";
	public final static String DATABASE_COLLECTION = "COLLECTION";

	private static String configPropertiesFileName = "alerts.properties";

	private static String configPropertiesFileNameRealPath;

	private static AlertsConfiguration instance;
	private final Properties properties;

	private final static Logger LOGGER = Logger.getLogger(AlertsConfiguration.class.getName());

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
	private static AlertsConfiguration getInstance() throws IOException {
		if (null == instance) {
			instance = new AlertsConfiguration();
		}
		return instance;
	}

	/**
	 *
	 * Read the properties file to get the parameters to create instance
	 *
	 */
	private AlertsConfiguration() throws IOException {
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