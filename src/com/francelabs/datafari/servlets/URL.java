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

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.common.params.ModifiableSolrParams;

import com.francelabs.datafari.statistics.StatsPusher;
import com.francelabs.datafari.utils.LocalFileReader;
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
	
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		ModifiableSolrParams params = new ModifiableSolrParams(
				request.getParameterMap());
		StatsPusher.pushDocument(params);
		String surl = request.getParameter("url");

		if (ScriptConfiguration.getProperty("ALLOWLOCALFILEREADING").equals(
				"true")
				&& LocalFileReader.isLocalFile(surl)) {

		    /** File Display/Download -->
		    <!-- Written by Rick Garcia -->
		    */
			// try to open the file locally
			ServletOutputStream outStream = response.getOutputStream();
			long length = LocalFileReader.readFile(surl, outStream);

			ServletContext context = getServletConfig().getServletContext();
			String mimetype = context.getMimeType(surl);

			// sets response content type
			if (mimetype == null) {
				mimetype = "application/octet-stream";
			}
			response.setContentType(mimetype);
			response.setContentLength((int) length);

			// sets HTTP header
			response.setHeader("Content-Disposition", "inline; fileName=\""
					+ surl + "\"");

			outStream.close();
		} else {

			RequestDispatcher rd = request.getRequestDispatcher(redirectUrl);
			rd.forward(request, response);
			System.out.println("p");
		}
	}
}
