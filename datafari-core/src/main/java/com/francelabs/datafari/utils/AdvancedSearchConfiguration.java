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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Configuration reader
 *
 * @author France Labs
 *
 */
public class AdvancedSearchConfiguration {

	// Properties
	public static final String DENIED_FIELD_LIST = "DENIEDFIELDLIST";

	private static String configPropertiesFileName = "advanced-search.properties";

	private static String configPropertiesFileNameRealPath;

	private static AdvancedSearchConfiguration instance;
	private Properties properties;
	private boolean listenPropertiesChanges = false;

	private final static Logger LOGGER = Logger.getLogger(AdvancedSearchConfiguration.class.getName());

	/**
	 * Return the value of the property given as parameter
	 *
	 * @param key
	 *            the property name
	 * @return the property value
	 * @throws IOException
	 */
	public String getProperty(final String key) throws IOException {
		return (String) getInstance().properties.get(key);
	}

	/**
	 *
	 * Get the instance
	 *
	 */
	public static AdvancedSearchConfiguration getInstance() {
		if (null == instance) {
			instance = new AdvancedSearchConfiguration();
		}
		return instance;
	}

	/**
	 *
	 * Read the properties file to get the parameters to create instance
	 *
	 */
	private AdvancedSearchConfiguration() {

		configPropertiesFileNameRealPath = Environment.getProperty("catalina.home") + File.separator + "conf"
				+ File.separator + configPropertiesFileName;
		loadProperties();
	}

	/**
	 * Load/reload the advanced search properties file
	 */
	private void loadProperties() {
		final File configFile = new File(configPropertiesFileNameRealPath);
		properties = new Properties();
		try (final InputStream stream = new FileInputStream(configFile)) {
			properties.load(stream);
		} catch (final IOException e) {
			LOGGER.error("Cannot read file : " + configFile.getAbsolutePath(), e);
		}
	}

	/**
	 * Start a FileWatcher thread to detect every modification of the advanced
	 * search properties file and automatically reload it
	 */
	private void watchPropertiesFile() {
		final Thread watcherThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// Java Watchers work on directories so we need to extract the
				// parent directory path of the properties file
				String parentDir = new File(configPropertiesFileNameRealPath).getAbsolutePath();
				parentDir = parentDir.substring(0, parentDir.lastIndexOf(File.separator));
				final Path path = new File(parentDir).toPath();
				try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
					// Register the service on MODIFY events
					path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
					while (true && listenPropertiesChanges) {
						final WatchKey wk = watchService.take();
						for (final WatchEvent<?> event : wk.pollEvents()) {
							// we only register "ENTRY_MODIFY" so the context is
							// always
							// a Path.
							final Path changed = (Path) event.context();
							if (changed.endsWith(configPropertiesFileName) && (event.count() == 1)) {
								LOGGER.info("Advanced search config file has changed, reloading the properties");
								Thread.sleep(200);
								loadProperties();
							}
						}
						// reset the key
						wk.reset();
					}
				} catch (IOException | InterruptedException e) {
					LOGGER.error(e.getMessage());
				}
			}
		});
		watcherThread.start();
	}

	/**
	 * Listen for every changes/modifications of the advanced search properties
	 * file
	 */
	public void listenChanges() {
		listenPropertiesChanges = true;
		watchPropertiesFile();
	}

	/**
	 * Order to stop checking any modification of the properties file
	 */
	public void stopListeningChanges() {
		listenPropertiesChanges = false;
	}

}