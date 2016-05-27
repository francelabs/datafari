package com.francelabs.datafari.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ZKUtils {

	
	public static void configZK (String scriptname,String configName) throws IOException {
		String datafari_home;
		boolean devMode = false;
		String pathScript ;
		String zkhosts = ScriptConfiguration.getProperty("SOLRHOSTS");
		datafari_home = System.getenv("DATAFARI_HOME");									//Gets the directory of installation if in standard environment
		if(datafari_home==null){															//If in development environment	
			datafari_home = ExecutionEnvironment.getDevExecutionEnvironment();
			devMode = true;
		}
		if (devMode == false)
			pathScript = datafari_home + "/bin/zkUtils/";
		else
			pathScript = datafari_home + "/debian7/bin/zkUtils/";
		
		
		System.out.println("pathscript" +pathScript);
	    String[] command = {"/bin/bash", pathScript+scriptname, datafari_home, zkhosts, configName};
	    ProcessBuilder p = new ProcessBuilder(command);
	    Process p2 = p.start();
	   
	}


}