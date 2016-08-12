/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * 
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.startup.LikesLauncher;

public class UpdateNbLikes {

	public static UpdateNbLikes instance;
	public final static String configPropertiesFileName = "external_nbLikes";
	private static File configFile;
	private Properties properties = new Properties();

	final static Logger logger = Logger.getLogger(UpdateNbLikes.class.getName());

	private UpdateNbLikes() throws IOException {
		super();
		BasicConfigurator.configure();
		configFile = new File(System.getProperty("catalina.home") + File.separator + ".." + File.separator + "solr"
				+ File.separator + "solr_home" + File.separator + "FileShare_shard1_replica1" + File.separator + "data"
				+ File.separator + configPropertiesFileName);
		if (configFile.exists()) {
			properties.load(new FileInputStream(configFile));
		} else
			configFile.createNewFile();
	}

	public static synchronized UpdateNbLikes getInstance() throws DatafariServerException {
		try {
			if (instance == null) {
				instance = new UpdateNbLikes();
			}
			return instance;
		} catch (IOException e) {
			logger.error(e);
			throw new DatafariServerException(CodesReturned.GENERALERROR, e.getMessage());
		}
	}

	/**
	 * increment the likes of a document
	 * 
	 * @param document
	 *            the id of the document that have to be which likes has to be
	 *            incremented
	 */
	public void increment(String document) {
		String nbLikes = (String) properties.get(document);
		if (nbLikes == null) {
			properties.setProperty(document, "1.0");
		} else {
			properties.setProperty(document, String.valueOf(Float.parseFloat(nbLikes) + 1));
		}
		saveProperty();
		LikesLauncher.saveChange();
	}

	/**
	 * decrement the likes of a document
	 * 
	 * @param document
	 *            the id of the document that have to be which likes has to be
	 *            decremented
	 */
	public void decrement(String document) {

		String nbLikes = (String) properties.get(document);
		if (nbLikes == null || Float.parseFloat(nbLikes) <= 0) {
			properties.setProperty(document, "0");
		} else {
			properties.setProperty(document, String.valueOf(Float.parseFloat(nbLikes) - 1));
		}
		saveProperty();
		LikesLauncher.saveChange();

	}

	public void saveProperty() {
		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(UpdateNbLikes.configFile);
			properties.store(fileOutputStream, "");
			fileOutputStream.close();
			File originalFile = UpdateNbLikes.configFile;
			BufferedReader br = new BufferedReader(new FileReader(originalFile));
			File tempFile = new File("tempfile.txt");
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				line = line.replaceAll("file\\\\:", "file:");
				line = line.replaceAll("http\\\\:", "http:");
				pw.println(line);
				pw.flush();
			}
			pw.close();
			br.close();
			if (!originalFile.delete()) {
				logger.debug("Could not delete file");
				return;
			}
			if (!tempFile.renameTo(originalFile))
				logger.debug("Could not rename file");

		} catch (FileNotFoundException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public File getConfigFile() {
		return configFile;
	}

}