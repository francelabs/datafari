/*******************************************************************************
 *  * Copyright 2016 France Labs
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

package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript;

/**
 * This Servlet is used to save and restore the MCF connections from AdminUI
 * It is called by MCFBackupRestore.html
 * DoGet is not used
 * DoPost is used to save and restore the MCF connections, given the action parameter (save or restore)
 * @author Giovanni Usai
 */
@WebServlet("/admin/MCFBackupRestore")
public class MCFBackupRestore extends HttpServlet {
	
	private static final long serialVersionUID = -6561976993995634818L;

	private String env;
	
	private static final String DEFAULT_MCF_BACKUP_DIR = "/bin/backup/mcf";
	
	private final static Logger LOGGER = LogManager.getLogger(MCFBackupRestore.class);
	
	/**
	 * @see HttpServlet#HttpServlet()
	 * Gets the environment path of Datafari installation
	 */
	public MCFBackupRestore() {
		env = Environment.getEnvironmentVariable("DATAFARI_HOME");									
		if(env==null){															
			// if no variable is set, use the default installation path
			env = "/opt/datafari";
		}
	}

	/**
	 * @throws IOException 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * It saves the MCF connections if action parameter is save
	 * It restores the MCF connections if action parameter is restore
	 * It uses the backup directory in input (if specified) or a default path
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			
			String action = request.getParameter("action");
			
			if (action != null && !action.trim().isEmpty()){
				
				String backupDirTmp = request.getParameter("backupDir");
				
				String backupDirectory;
				
				// Check if the backup dir has been given in input, otherwise take the default path
				if (backupDirTmp != null && !backupDirTmp.trim().isEmpty()){
					backupDirectory = backupDirTmp.trim();
				} else {
					backupDirectory = env + DEFAULT_MCF_BACKUP_DIR;
				}
				
				if (action.trim().equalsIgnoreCase("save")){
					
					BackupManifoldCFConnectorsScript.doSave(backupDirectory);
					
				} else if (action.trim().equalsIgnoreCase("restore")){
					
					BackupManifoldCFConnectorsScript.doRestore(backupDirectory);
				}
			}
		} catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69253");
			out.close();
			LOGGER.error("Error in MCFBackupRestore doPost. Error 69253", e);
		}
	}
}
