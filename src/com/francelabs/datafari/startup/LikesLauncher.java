package com.francelabs.datafari.startup;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import com.francelabs.datafari.servlets.admin.StringsDatafariProperties;
import com.francelabs.datafari.utils.ScriptConfiguration;
import com.francelabs.datafari.utils.SendHttpRequest;

public class LikesLauncher extends HttpServlet{
	
	private static boolean islaunched = false;
	private static ScheduledExecutorService scheduler;
	private static Logger LOGGER = Logger.getLogger(LikesLauncher.class.getName());
	private static Semaphore semaphore;
	private static ScheduledFuture<?> handler;
	private static boolean doReload = false;
	
	public void init() throws ServletException{
	
		String isEnabled = null;
		try {
			isEnabled = ScriptConfiguration.getProperty(StringsDatafariProperties.LIKESANDFAVORTES);
		} catch (IOException e) {
			LOGGER.error(e);
		}
		if (isEnabled !=null && isEnabled.equals("true"))
			startScheduler();
		/*			
 	    String filename = "reloadCache.sh";
			  String env = System.getenv("DATAFARI_HOME");		//Gets the directory of installation if in standard environment	
			  if(env==null){										//If in development environment
				  String path;
				  path = new File(".").getCanonicalPath();
				  env = path+"/workspace/datafari/tomcat/bin/"+filename;	//Hardcoded path
			  }else{
				  env = env+"/tomcat/bin/"+filename;
			  }
			 Runtime.getRuntime().exec(env);
		*/ 
		System.out.println("----------");
		System.out.println("---------- LikesLauncher Servlet Initialized successfully ----------");
		System.out.println("----------");	
		
	        
	}
	
	public static void startScheduler(){
		if (!islaunched){
			islaunched=true;
		   scheduler = Executors.newScheduledThreadPool(1);
		   handler = scheduler.scheduleAtFixedRate(reloadCache, 1, 10, TimeUnit.SECONDS);
		}
	}
	public static void saveChange(){
		LikesLauncher.doReload = true;
	}
	
	static Runnable reloadCache = new Runnable(){ 
		@Override
		public void run() {
			if (LikesLauncher.doReload){
				try {
					SendHttpRequest.sendGET("http://localhost:8080/datafari-solr/FileShare/reloadCache");
					LikesLauncher.doReload = false;
				} catch (IOException e) {
					LOGGER.error(e);
				}
			}
		}
	};
		
	public static void shutDown(){
		System.out.println("-----------------Trying to ShutDown the scheduler---------------------");
		if (islaunched){
			System.out.println("-----------------*******************************---------------------");
			handler.cancel(true);
			scheduler.shutdown();
		}
		islaunched=false;
	}
	
}
