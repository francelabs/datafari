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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.francelabs.datafari.statistics.StatsPusher;

/**
 * Configuration reader
 * 
 * @author France Labs
 * 
 */
public class CorePropertiesConfiguration {

	
	public static String configPropertiesFileName = "core.properties";
	
	public static String configPropertiesFileNameRealPath;

	private static CorePropertiesConfiguration instance;
	private Properties properties;

	private final static Logger LOGGER = Logger.getLogger(CorePropertiesConfiguration.class
			.getName());


	/**
	 * Set a property and save it the datafar.properties
	 * @param key : the key that should be change
	 * @param value : the new value of the key
	 * @return : true if there's an error and false if not
	 */
	public static boolean setProperty(String key, String value) {
			try {
				String env;
				if (System.getenv("DATAFARI_HOME") == null)
					env = System.getProperty("solr.solr.home");
				else
					env = System.getenv("DATAFARI_HOME") + "/solr/solr_home";
				env += "/FileShare/"+configPropertiesFileName ;
				getInstance().properties.setProperty(key, value);
				FileOutputStream fileOutputStream = new FileOutputStream(configPropertiesFileNameRealPath);
				instance.properties.store(fileOutputStream, null);
				fileOutputStream = new FileOutputStream(env);
				instance.properties.store(fileOutputStream, null);
				fileOutputStream.close();
				return false;
			} catch (IOException e) {
				LOGGER.error(e);
				return true;		
			}
	}

	public static String getProperty(String key) throws IOException {
		return (String) getInstance().properties.get(key);
	}

	
	/**
	 * 
	 * Get the instance
	 * 
	 */
	private static CorePropertiesConfiguration getInstance() throws IOException {
		if (null == instance) {
			instance = new CorePropertiesConfiguration();
		}
		return instance;
	}

	/**
	 * 
	 * Read the properties file to get the parameters to create  instance
	 * 
	 */
	private CorePropertiesConfiguration() throws IOException {
		BasicConfigurator.configure();
		String env;
		if (System.getenv("DATAFARI_HOME") == null)
			env = System.getProperty("solr.solr.home");
		else
			env = System.getenv("DATAFARI_HOME") + "/solr/solr_home";
		env += "/FileShare/"+configPropertiesFileName ;
		
		configPropertiesFileNameRealPath = env;
		File configFile = new File(configPropertiesFileNameRealPath);
		InputStream stream = new FileInputStream(configFile);
		properties = new Properties();
		try {
			properties.load(stream);
		} catch (IOException e){
			LOGGER.error("Cannot read file : "+ configFile.getAbsolutePath(),e );
			throw e;
		} finally {
			stream.close();
		}
		
	}

}
