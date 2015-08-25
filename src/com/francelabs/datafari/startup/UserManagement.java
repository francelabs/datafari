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
package com.francelabs.datafari.startup;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import org.apache.log4j.Logger;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.user.User;


/**
 * Servlet implementation class UserManagement
 */
@WebServlet("/UserManagement")
public class UserManagement extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger(UserManagement.class.getName());
	
	public void init() throws ServletException{
			
			User admin = new User("admin","admin");
			admin.deleteUser();
			int code = admin.isInBase();
			if (code == CodesReturned.PROBLEMCONNECTIONDATABASE){
				LOGGER.error("Mongodb is not running, please start it. UserManagement Failed to start");
			}else if (code==CodesReturned.TRUE){
				if (admin.signIn()==CodesReturned.TRUE){
					LOGGER.info("WARNING : The account admin have the default password");
				}
			}else{
				if (admin.signup("SearchAdministrator")==CodesReturned.ALLOK)
					LOGGER.info("The accound admin was created in database. Please change the password");
				else{
					LOGGER.error("Mongodb is not running, please start it. UserManagement Failed to start");
				}
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
