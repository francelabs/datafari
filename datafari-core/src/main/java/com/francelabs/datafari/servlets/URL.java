/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.SystemUtils;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.francelabs.datafari.statistics.StatsPusher;
import com.francelabs.datafari.utils.ScriptConfiguration;





/**
 * Servlet implementation class URL
 */
@WebServlet("/URL")
public class URL extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final String redirectUrl = "/url.jsp";


	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public URL() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		request.setCharacterEncoding("UTF-8");
		
		
		ModifiableSolrParams params = new ModifiableSolrParams(
				request.getParameterMap());
		StatsPusher.pushDocument(params);
		
		//String surl = URLDecoder.decode(request.getParameter("url"), "ISO-8859-1");
		String surl = request.getParameter("url");
		

		if ((ScriptConfiguration.getProperty("ALLOWLOCALFILEREADING").equals("true")) && !(surl.startsWith("file://///"))) {

			int BUFSIZE = 4096;
			String fileName = null ;
			
			
			
			/** File Display/Download -->
		    <!-- Written by Rick Garcia -->
			 */
			if (SystemUtils.IS_OS_LINUX) {
			// try to open the file locally
			String fileNameA[] = surl.split(":");
			fileName= URLDecoder.decode(fileNameA[1], "UTF-8");
			
			}
			else if (SystemUtils.IS_OS_WINDOWS){
				fileName = URLDecoder.decode(surl, "UTF-8").replaceFirst("file:/", "");
			}
			
			

			File file = new File(fileName);
			int length   = 0;
			ServletOutputStream outStream = response.getOutputStream();
			ServletContext context  = getServletConfig().getServletContext();
			String mimetype = context.getMimeType(fileName);

			// sets response content type
			if (mimetype == null) {
				mimetype = "application/octet-stream";
				
			}
			response.setContentType(mimetype);
			response.setContentLength((int)file.length());

			// sets HTTP header
			response.setHeader("Content-Disposition", "inline; fileName=\"" + fileName + "\"");

			byte[] byteBuffer = new byte[BUFSIZE];
			DataInputStream in = new DataInputStream(new FileInputStream(file));

			// reads the file's bytes and writes them to the response stream
			while ((in != null) && ((length = in.read(byteBuffer)) != -1))
			{
				outStream.write(byteBuffer,0,length);
			}

			in.close();
			outStream.close();
		} else {

			RequestDispatcher rd = request.getRequestDispatcher(redirectUrl);
			rd.forward(request, response);
		}
	}
}
