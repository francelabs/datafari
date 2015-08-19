package com.francelabs.datafari.startup;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.francelabs.datafari.servlets.admin.StringsDatafariProperties;
import com.francelabs.datafari.utils.ScriptConfiguration;
import com.francelabs.datafari.utils.UpdateNbLikes;
import com.francelabs.realm.MongoDBRunning;
import com.francelabs.realm.User;

/**
 * Servlet implementation class UserManagement
 */
@WebServlet("/UserManagement")
public class UserManagement extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger(UserManagement.class.getName());
	
	public void init() throws ServletException{
		
		MongoDBRunning mongoDBRunning = new MongoDBRunning(User.IDENTIFIERSDB);
		if (mongoDBRunning.isConnected()){
			User admin = new User("admin","admin",mongoDBRunning.getDb());
			if (admin.isInBase()){
				if (admin.signIn()){
					LOGGER.info("WARNING : The account admin have the default password");
				}
			}else{
				if (admin.signup("SearchAdministrator"))
					LOGGER.info("The accound admin was created in database. Please change the password");
				else{
					LOGGER.error("Mongodb is not running, please start it. UserManagement Failed to start");
				}
			}
		}else{
			LOGGER.error("Mongodb is not running, please start it. UserManagement Failed to start");
		}
	}
	
	/**
     * @see HttpServlet#HttpServlet()
     */
    public UserManagement() {
        super();
        // TODO Auto-generated constructor stub
    }
}
