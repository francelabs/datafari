package com.francelabs.datafari.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class LdapMcfConfig{
	private static File authorityconnectionJSON;
	private static LdapMcfConfig instance;
	public final  static String autorityConnectionElement = "authorityconnection";
	public final  static String autorityGroupElement = "authoritygroup";
	public final static String configurationElement = "configuration";
	public final  static String domainControllerElement = "domaincontroller";
	public final static String attributeUsername = "_attribute_username";
	public final static String attributeDomainController = "_attribute_domaincontroller";
	public final static String attributePassword = "_attribute_password";
	public final static String attributeSuffix = "_attribute_suffix";
	private final static Logger logger = Logger.getLogger(LdapMcfConfig.class); 
	
	public static int update(HashMap<String,String> h){
		try {
			JSONObject json = JSONUtils.readJSON(getInstance().authorityconnectionJSON);
			
			if (h.containsKey(attributeUsername) && h.containsKey(attributeDomainController) && h.containsKey(attributePassword) && h.containsKey(attributeSuffix)){	
				JSONObject authorityconnection =  (JSONObject) json.get(autorityConnectionElement);
				JSONObject configuration = (JSONObject) authorityconnection.get(configurationElement);
				JSONObject domainController = (JSONObject) configuration.get(domainControllerElement);
				domainController.put(attributeUsername,h.get(attributeUsername));
				domainController.put(attributeDomainController,h.get(attributeDomainController));
				domainController.put(attributePassword,h.get(attributePassword));
				domainController.put(attributeSuffix, h.get(attributeSuffix));
				//				configuration.put(domainControllerElement,domainController);
				//				authorityconnection.put(configurationElement,configuration);
				//				json.put(autorityConnectionElement, authorityconnection);
				JSONUtils.saveJSON(json, authorityconnectionJSON);
				//				FileWriter file;
				//				try {
				//					file = new FileWriter(authorityconnectionJSON);
				//					file.write(json.toString());
				//					file.flush();
				//					file.close();
				//				} catch (IOException e) {
				//					logger.error(e);
				//					return CodesReturned.GENERALERROR;
				//				}
				try {
					ManifoldAPI.deleteConfig("authorityconnections", "DatafariAD");
				}
				catch (Exception e){
					
				}
				ManifoldAPI.putConfig("authorityconnections","DatafariAD",json);
				//				urlParameters.add(new BasicNameValuePair("parameter", authorityconnection.toString()));
				//				urlParameters.add(new BasicNameValuePair("caller", ""));
				//				urlParameters.add(new BasicNameValuePair("num", "12345"));
				return CodesReturned.ALLOK;
			}else{
				return CodesReturned.PARAMETERNOTWELLSET;
			}
		} catch (IOException e) {
			logger.error("FATAL ERROR",e);
			return CodesReturned.GENERALERROR;
		} catch (JSONException e) {
			logger.error("FATAL ERROR",e);
			return CodesReturned.GENERALERROR;
		} catch (Exception e) {
			logger.error("FATAL ERROR",e);
			return CodesReturned.GENERALERROR;
		}
	}
	
	private static LdapMcfConfig getInstance(){
		if (instance==null)
			return instance = new LdapMcfConfig();
		return instance;
	}
	
	private LdapMcfConfig(){
		// TODO : change path for dev environment
		String filePath = System.getProperty("catalina.home") + File.separator +".." + File.separator + "bin" + File.separator + "common" + File.separator + "config" 
				+ File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator + "authorityconnections" + File.separator + "authorityConnection.json";
		
		authorityconnectionJSON = new File(filePath);
	}
}
