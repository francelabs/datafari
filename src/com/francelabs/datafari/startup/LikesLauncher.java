package com.francelabs.datafari.startup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import com.francelabs.datafari.user.Like;
import com.francelabs.datafari.user.NbLikes;
import com.francelabs.datafari.utils.CustomProperties;
import com.francelabs.datafari.utils.ScriptConfiguration;
import com.francelabs.datafari.utils.SendHttpRequest;
import com.francelabs.datafari.utils.UpdateNbLikes;

public class LikesLauncher extends HttpServlet{
	
	private static boolean islaunched = false;
	private static ScheduledExecutorService scheduler;
	private static Logger LOGGER = Logger.getLogger(LikesLauncher.class.getName());
	private static Semaphore semaphore;
	private static ScheduledFuture<?> handler;
	private static boolean doReload = false;
	private static boolean isThreadUpdateNbLikesStarted = false;
	
	public void init() throws ServletException{
	
		String isEnabled = null;
		try {
			isEnabled = ScriptConfiguration.getProperty(StringsDatafariProperties.LIKESANDFAVORTES);
		} catch (IOException e) {
			LOGGER.error(e);
		}
		if (isEnabled !=null && isEnabled.equals("true")){
			updateNbLikes();
			startScheduler();
		}
		System.out.println("----------");
		System.out.println("---------- LikesLauncher Servlet Initialized successfully ----------");
		System.out.println("----------");	
		
	        
	}
	
	private void updateNbLikes(){
		if (!LikesLauncher.isThreadUpdateNbLikesStarted){
			LikesLauncher.isThreadUpdateNbLikesStarted = true;
			new Thread(new Runnable(){
				public void run(){
					ArrayList<NbLikes> listLikes = Like.getNbLikes();
				
					CustomProperties properties = new CustomProperties();
					for (int i=0; i<listLikes.size() ; i++){
						NbLikes doc = listLikes.get(i);
						properties.put(doc.documentId,doc.nbLikes);
					}
					try {
						UpdateNbLikes.getInstance();
						UpdateNbLikes.properties = properties;
						UpdateNbLikes.saveProperty();
						LOGGER.info("updateNbLikes finished it's work");
					} catch (IOException e) {
						LOGGER.error(e);
					}
				}
			}).start();
		}
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
