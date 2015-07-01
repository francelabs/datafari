package com.francelabs.datafari.servlets.admin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.francelabs.datafari.solrj.SolrServers.Core;


/** Javadoc
 * 
 * This servlet is used to modify or download the Synonyms files of the FileShare core
 * It is only called by the Synonyms.html.
 * doGet is used to print the content of the file or download it.
 * doPost is used to confirm the modifications of the file.
 * The semaphores (one for each language) are created in the constructor.
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/Synonyms")
public class Synonyms extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final List<SemaphoreLn> listMutex = new ArrayList<SemaphoreLn>();
	private String server = Core.FILESHARE.toString();
	private String absoluteDiskPath;
	/**
     * @throws IOException 
     * @see HttpServlet#HttpServlet()
     * Gets the list of the languages
     * Creates a semaphore for each of them
     */
	public Synonyms() throws IOException {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * used to print the content of the file or to download it
	 * If called to print it will return plain text
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String relativeWebPath = "./"; 
		absoluteDiskPath = getServletContext().getRealPath(relativeWebPath);
		absoluteDiskPath = absoluteDiskPath.substring(0,absoluteDiskPath.indexOf("tomcat"));
		String content = readFile(absoluteDiskPath+"solr/solr_home/"+server+"/conf/list_language.txt", StandardCharsets.UTF_8);
		String[] lines = content.split(System.getProperty("line.separator"));						//There is one language per line
		if(listMutex.size()==0){																	//If it's the first time
			for(int i=0;i<lines.length;i++){														//For each line
				listMutex.add(new SemaphoreLn(lines[i], "Syn"));									//create a semaphore and add it to the list
			}
		}
		if(request.getParameter("language")!=null){						//Print the content of the file
			for(SemaphoreLn sem : listMutex){							//For all the semaphores
				if (sem.getLanguage().equals(request.getParameter("language")) && sem.getType().equals("Syn")){ //if it has the good language and type(stopwords or Synonyms for now)
					if(sem.availablePermits()!=0){						//If it is available
						try {
							sem.acquire();								//Acquire it
						} catch (InterruptedException e) {

							e.printStackTrace();
						}
						String filename = "synonyms_"+request.getParameter("language").toString()+".txt";
						response.setContentType("application/octet-stream");
						String filepath = absoluteDiskPath+"solr/solr_home/"+server+"/conf/";			//hardcoded path
						String synContent = readFile(filepath+filename, StandardCharsets.UTF_8);
																		//get the file and put its content into a string
						response.setContentType("text/html");
						PrintWriter out = response.getWriter();
						out.append(synContent);							//returns the content of the file
						out.close();
					}else{												//if not available
						PrintWriter out = response.getWriter();
						out.append("File already in use"); 	
						out.close();
					}
				}
			}
		}else{															//Download the file
			String filename = "synonyms_"+request.getParameter("languagebis")+".txt";
			response.setContentType("application/octet-stream");
			String filepath = absoluteDiskPath+"solr/solr_home/"+server+"/conf/";			//hardcoded path
			String disHeader = "Attachment; Filename=\"" + filename + "\"";
			response.setHeader("Content-Disposition", disHeader);
			File fileToDownload = new File(filepath + filename);
			InputStream in = null;
			ServletOutputStream outs = response.getOutputStream();
			try {
				in = new BufferedInputStream(
						new FileInputStream(fileToDownload));
				int ch;
				char caractere;
				while ((ch = in.read()) != -1) {
					caractere = (char) ch;
					if (caractere == '\\')
						continue;

					outs.print(caractere);
				}
			} finally {
				if (in != null){
					in.close();
				}
			}
			outs.flush();
			outs.close();
			in.close(); 
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * used to confirm the modifications and/or release the semaphore
	 * called by the confirm modification or by the user loading an other page
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameter("content")==null){  					//the user load an other page  
			for(SemaphoreLn sem : listMutex){  							//Get the correct semaphore and check if it was not already released
				if (sem.getLanguage().equals(request.getParameter("language")) && sem.getType().equals("Syn") && sem.availablePermits()<1){
					sem.release();										//Release the semaphore
				}
			}
		}else{															//The user clicked on confirm modification
			File file ;
			String filePath = absoluteDiskPath+"solr/solr_home/"+server+"/conf/synonyms_"+request.getParameter("language")+".txt"; 			//hardcoded path
			file = new File(filePath);										
			FileOutputStream fooStream = new FileOutputStream(file, false); // true to append false to overwrite.
			byte[] myBytes = request.getParameter("content").getBytes();
			fooStream.write(myBytes);										//rewrite the file
			fooStream.close();
			for(SemaphoreLn sem : listMutex){ 								//Get the correct semaphore and check if it was not already released
				if (sem.getLanguage().equals(request.getParameter("language")) && sem.getType().equals("Syn") && sem.availablePermits()<1){
					sem.release();											//Release the semaphore
				}
			}
		}
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
}