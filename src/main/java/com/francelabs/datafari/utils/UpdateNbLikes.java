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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.francelabs.datafari.startup.LikesLauncher;

public class UpdateNbLikes {
	
	public static  UpdateNbLikes instance;
	private final static Logger LOGGER = Logger.getLogger(UpdateNbLikes.class
			.getName());
	public final static String configPropertiesFileName = "external_nbLikes";
	private static File configFile;
	private Properties properties = new Properties();
	
	
	private UpdateNbLikes()throws IOException {
        super();
        BasicConfigurator.configure();
        configFile = new File(System.getProperty("catalina.home") + File.separator + ".." + File.separator + "solr" + File.separator +"solr_home" +
        File.separator + "FileShare"+ File.separator + "data"+ File.separator + configPropertiesFileName);
		if(configFile.exists()){
			properties.load(new FileInputStream(configFile));
		}
		else
			configFile.createNewFile();
	}
	
	public static synchronized UpdateNbLikes getInstance() throws IOException{
		if (instance == null){
			instance = new UpdateNbLikes();
		}
		return instance;
	}
	
	/**
	 * increment the likes of a document
	 * @param document the id of the document that have to be which likes has to be incremented
	 */
	public void increment(String document){
		try {
			String nbLikes = (String) UpdateNbLikes.getInstance().properties.get(document);
			if (nbLikes==null){
				UpdateNbLikes.getInstance().properties.setProperty(document,"1");
			}else{
				UpdateNbLikes.getInstance().properties.setProperty(document,String.valueOf(Float.parseFloat(nbLikes)+1));
			}
			saveProperty();
			LikesLauncher.saveChange();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	/**
	 * decrement the likes of a document
	 * @param document the id of the document that have to be which likes has to be decremented
	 */
	public void decrement(String document){
		try {
			String nbLikes = (String) UpdateNbLikes.getInstance().properties.get(document);
			if (nbLikes==null || Integer.parseInt(nbLikes) <= 0){
				UpdateNbLikes.getInstance().properties.setProperty(document,"0");
			}else{
				UpdateNbLikes.getInstance().properties.setProperty(document,String.valueOf(Float.parseFloat(nbLikes)-1));
			}
			saveProperty();
			LikesLauncher.saveChange();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public  void saveProperty(){
		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(UpdateNbLikes.configFile);
			properties.store(fileOutputStream, "");
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		}
	}
	
	public File getConfigFile(){
		return this.configFile;
	}
	
}
