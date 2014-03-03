package com.francelabs.manifoldcf.configuration.file;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.francelabs.datafari.script.ScriptConfiguration;
import com.francelabs.manifoldcf.configuration.file.jaxb.Configuration;
import com.francelabs.manifoldcf.configuration.file.jaxb.Property;

/**ZZZ
 * 
 * @author France Labs
 * 
 */
public class ManifoldCFConfigurationFile {

	
	private static ManifoldCFConfigurationFile instance;
	private Properties properties;
	private static String configPropertiesFileName = "properties.xml";

	
	
	public static String getProperty(String key) throws IOException, JAXBException{
		return (String) getInstance().properties.get(key);
	}
	/**
	 * 
	 * Get the JDBC connection instance
	 * @throws JAXBException 
	 * 
	 */
	private static ManifoldCFConfigurationFile getInstance() throws IOException, JAXBException {
		if (null == instance) {
			instance = new ManifoldCFConfigurationFile();
		}
		return instance;
	}

	/**
	 * 
	 * Read the properties file to get the parameters to create the JDBC
	 * connection instance
	 * @throws JAXBException 
	 * 
	 */
	private ManifoldCFConfigurationFile() throws IOException, JAXBException {

		String filePath = ScriptConfiguration.getProperty("ManifoldCFHome")+"/"+configPropertiesFileName ;
		File file = new File(filePath);
		

		JAXBContext jc = JAXBContext.newInstance("com.francelabs.manifoldcf.configuration.file.jaxb");
		Unmarshaller unmarshaller = jc.createUnmarshaller();

		Configuration configuration = (Configuration) unmarshaller.unmarshal(file);
		

		properties = new Properties();
		
		for (Property property : configuration.getProperty()){
			properties.put(property.getName(), property.getValue());
		}
		
	}

}
