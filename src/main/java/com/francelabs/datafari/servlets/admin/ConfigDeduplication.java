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
package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.utils.*;
import com.francelabs.datafari.servlets.admin.StringsDatafariProperties.*;

/**
 * Servlet implementation class ConfigDeduplication
 */
@WebServlet("/ConfigDeduplication")
public class ConfigDeduplication extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ConfigDeduplication.class.getName());
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConfigDeduplication() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		BasicConfigurator.configure();
		JSONObject jsonResponse = new JSONObject();
		if (request.getParameter("id")!=null){
			int id = Integer.parseInt(request.getParameter("id"));
			String enable = request.getParameter("enable");
			request.setCharacterEncoding("utf8");
			response.setContentType("application/json");
			boolean error=false;
			if (id==1){
				if (enable!=null){
					if (enable.equals("true")){
						error = CorePropertiesConfiguration.setProperty(StringsDatafariProperties.DEDUPLICATION, "true");
					}
					else{
						error = CorePropertiesConfiguration.setProperty(StringsDatafariProperties.DEDUPLICATION, "false");
					}
				}
				
			}else if (id==2){
				if (enable!=null){
					if (enable.equals("true")){
						error = CorePropertiesConfiguration.setProperty(StringsDatafariProperties.DEDUPLICATION_FACTORY, "true");
					}
					else{
						error = CorePropertiesConfiguration.setProperty(StringsDatafariProperties.DEDUPLICATION_FACTORY, "false");
						error = CorePropertiesConfiguration.setProperty(StringsDatafariProperties.DEDUPLICATION, "false");
					}
				}
			}
			try {
				if (error){				
					jsonResponse.put("code",-1);
				}else{
					jsonResponse.put("code",0);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.error(e);
			}
		}else if (request.getParameter("initiate")!=null){
			String checked,checked_factory;
			if (CorePropertiesConfiguration.getProperty(StringsDatafariProperties.DEDUPLICATION)!=null && CorePropertiesConfiguration.getProperty(StringsDatafariProperties.DEDUPLICATION).equals("true") ){
				checked="checked";
			 }else{
				 checked="";
			 }
			
			if (CorePropertiesConfiguration.getProperty(StringsDatafariProperties.DEDUPLICATION_FACTORY)!=null && CorePropertiesConfiguration.getProperty(StringsDatafariProperties.DEDUPLICATION_FACTORY).equals("true") ){
				checked_factory="checked";
			}else{
				checked_factory="";
			}
			try {
				jsonResponse.put("code",0);
				jsonResponse.put("checked", checked);
				jsonResponse.put("checked_factory", checked_factory);
			} catch (JSONException e) {
				logger.error(e);
			}
		}
		PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}
}


