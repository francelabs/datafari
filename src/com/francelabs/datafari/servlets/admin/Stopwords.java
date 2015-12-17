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
package com.francelabs.datafari.servlets.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.utils.ExecutionEnvironment;

/** Javadoc
 * 
 * This servlet is used to modify or download the Stopwords files of the FileShare core
 * It is only called by the Stopwords.html.
 * doGet is used to print the content of the file or download it.
 * doPost is used to confirm the modifications of the file.
 * The semaphores (one for each language) are created in the constructor.
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/Stopwords")
public class Stopwords extends HttpServlet { 
	private static final long serialVersionUID = 1L;
	private static final List<SemaphoreLn> listMutex = new ArrayList<SemaphoreLn>();
	private String server = Core.FILESHARE.toString();
	private String env;
	private String content;
	private final static Logger LOGGER = Logger.getLogger(Stopwords.class
			.getName());
	/**
	 * @throws IOException 
	 * @see HttpServlet#HttpServlet()
	 * Gets the list of the languages
	 * Creates a semaphore for each of them
	 */
	public Stopwords() throws IOException {
		env = System.getenv("DATAFARI_HOME");									//Gets the directory of installation if in standard environment
		if(env==null){															//If in development environment	
			env = ExecutionEnvironment.getDevExecutionEnvironment();
		}
		content="";
		try {
			if(new File(env+"/solr/solr_home/"+server+"/conf/list_language.txt").exists())
				content = readFile(env+"/solr/solr_home/"+server+"/conf/list_language.txt", StandardCharsets.UTF_8);  //Read the various languages
			else{
				content = "";
				return;
			}
		} catch (IOException e1) {
			LOGGER.error("Error while opening list_language.txt in StopWords Servlet's Constructor, please make sure the file exists and is located in "+env+"/solr/solr_home/"+server+"/conf/"+". Error 69012", e1);
		}	
		String[] lines = content.split(System.getProperty("line.separator"));					//There is one language per line
		for(int i=0;i<lines.length;i++){														//For each line
			listMutex.add(new SemaphoreLn(lines[i], "Stop"));									//create a semaphore and add it to the list
		}
	}

	/**
	 * @throws IOException 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * used to print the content of the file or to download it
	 * If called to print it will return plain text
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			if(content.equals("")){
				PrintWriter out = response.getWriter();
				out.append("Error while opening the list of languages, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69012"); 	
				out.close();
				return;
			}
			else if(request.getParameter("language")!=null){		 				//Print the content of the file
				for(SemaphoreLn sem : listMutex){							//For all the semaphores
					if (sem.getLanguage().equals(request.getParameter("language")) && sem.getType().equals("Stop")){ //if it has the good language and type(stopwords or Synonyms for now)
						try{
							if(sem.availablePermits()!=0){						//If it is available
								try {
									sem.acquire(); 								//Acquire it
								} catch (InterruptedException e) {
									LOGGER.error("Error while acquiring semaphore in Stopwords Servlet's doGet. Error 69013", e);
									PrintWriter out = response.getWriter();
									out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69013"); 	
									out.close();
									return;
								}
								String filename = "stopwords_"+request.getParameter("language").toString()+".txt";
								response.setContentType("application/octet-stream");
								String filepath = env+"/solr/solr_home/"+server+"/conf/";	
								String stopContent = readFile(filepath+filename, StandardCharsets.UTF_8);
								//get the file and put its content into a string
								response.setContentType("text/html");
								PrintWriter out = response.getWriter();
								out.append(stopContent);						//returns the content of the file
								out.close();
								return;
							}else{												//if not available
								PrintWriter out = response.getWriter();
								out.append("File already in use"); 	
								out.close();
							}
						}catch(IOException e){
							PrintWriter out = response.getWriter();
							out.append("Error while reading the stopwords file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69014"); 	
							out.close();
							LOGGER.error("Error while reading the stopwords_"+request.getParameter("language")+".txt file in Stopwords servlet, please make sure the file exists and is located in "+env+"/solr/solr_home/"+server+"/conf/"+". Error 69014", e);
						}
					}
				}
			}
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69504");
			out.close();
			LOGGER.error("Unindentified error in Stopwords doGet. Error 69504", e);
		}
	}

	/**
	 * @throws IOException 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * used to confirm the modifications and/or release the semaphore
	 * called by the confirm modification or if the user loads an other page, refreshes the page or switches language
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			if (request.getParameter("content")==null){  					//the user load an other page
				for(SemaphoreLn sem : listMutex){ 			 				//Get the correct semaphore and check if it was not already released
					if (sem.getLanguage().equals(request.getParameter("language")) && sem.getType().equals("Stop") && sem.availablePermits()<1){
						sem.release();										//Release the semaphore
					}
				}
			}else{															//The user clicked on confirm modification
				File file ;
				String filePath =  env+"/solr/solr_home/"+server+"/conf/stopwords_"+request.getParameter("language")+".txt";
				file = new File(filePath);										
				try{
					FileOutputStream fooStream = new FileOutputStream(file, false); // true to append, false to overwrite.
					byte[] myBytes = request.getParameter("content").replaceAll("&gt;", ">").replaceAll("<div>|<br>|<br >", "\n").replaceAll("</div>|</lines>|&nbsp;", "").getBytes();
					fooStream.write(myBytes);										//rewrite the file
					fooStream.close();
				}catch(IOException e){
					PrintWriter out = response.getWriter();
					out.append("Error while rewriting the stopwords file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69015"); 	
					out.close();
					LOGGER.error("Error while rewriting the file stopwords_"+request.getParameter("language")+" Stopwords Servlet's doPost. Error 69015", e);
					return;
				}
				for(SemaphoreLn sem : listMutex){ 								//Get the correct semaphore and check if it was not already released
					if (sem.getLanguage().equals(request.getParameter("language")) && sem.getType().equals("Stop")  && sem.availablePermits()<1){
						sem.release();											//Release the semaphore
					}
				}
			}
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69505");
			out.close();
			LOGGER.error("Unindentified error in Stopwords doPost. Error 69505", e);
		}
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return new String(encoded, encoding);

	}
}
