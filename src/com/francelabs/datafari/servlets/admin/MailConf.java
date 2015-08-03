package com.francelabs.datafari.servlets.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class MailConf
 */
@WebServlet("/admin/MailConf")
public class MailConf extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String env;
	private String content;
	private final static Logger LOGGER = Logger.getLogger(alertsAdmin.class
			.getName());       
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MailConf() {
		env = System.getenv("DATAFARI_HOME");									//Gets the directory of installation if in standard environment
		if(env==null){															//If in development environment	
			RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();	//Gets the D.solr.solr.home variable given in arguments to the VM
			List<String> arguments = runtimeMxBean.getInputArguments();
			for(String s : arguments){
				if(s.startsWith("-Dsolr.solr.home"))
					env = s.substring(s.indexOf("=")+1, s.indexOf("solr_home")-5);
			}
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			content="";
			try {
				content = readFile(env+"/bin/common/mail.txt", StandardCharsets.UTF_8);
			} catch (NoSuchFileException e1) {
				PrintWriter out = response.getWriter();				
				LOGGER.error("Error while reading the mail.txt in the doGet of the MailConf Servlet. Error 69031 ", e1);
				if(request.isUserInRole("SearchAdministrator")){
					out.append("Error while reading the mail.txt, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69031");
				}
				out.close();
				return;
			}
			try{
				if(request.isUserInRole("SearchAdministrator")){
					JSONObject json = new JSONObject();
					String[] lines = content.split(System.getProperty("line.separator"));
					for(int i=0; i<lines.length ; i++){
						if(lines[i].startsWith("smtp"))										//Get the host
							json.put("smtp", lines[i].substring(lines[i].indexOf("=")+1,lines[i].length()).trim());
						else if(lines[i].startsWith("from"))								//Get the address			
							json.put("from", lines[i].substring(lines[i].indexOf("=")+1,lines[i].length()).trim());
						else if(lines[i].startsWith("user"))								//Get the user name
							json.put("user", lines[i].substring(lines[i].indexOf("=")+1,lines[i].length()).trim());
						else if(lines[i].startsWith("pass"))								//Get the password
							json.put("pass", lines[i].substring(lines[i].indexOf("=")+1,lines[i].length()).trim());
					}
					response.getWriter().write(json.toString());
					response.setStatus(200);
					response.setContentType("text/json;charset=UTF-8");
				}else{
					PrintWriter out = response.getWriter();
					out.append("Insufficiant permission to print mail configuration"); 	
					out.close();
					return;
				}
			}catch (JSONException e){
				PrintWriter out = response.getWriter();				
				LOGGER.error("Error while creating the JSON answer inthe doGet of the MailConf Servlet. Error 69032 ", e);
				out.append("Error while reading the mail.txt, please make sure the file is correctly filled and retry, if the problem persists contact your system administrator. Error code : 69032");
				out.close();
				return;
			}
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69512");
			out.close();
			LOGGER.error("Unindentified error in MailConf doGet. Error 69512", e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			String[] lines = content.split(System.getProperty("line.separator"));
			String linesBis = "";
			for(int i=0; i<lines.length ; i++){
				if(lines[i].startsWith("smtp"))										//Get the host
					linesBis += lines[i].substring(0 ,lines[i].indexOf("=")+1)+request.getParameter("SMTP").trim()+"\n";
				else if(lines[i].startsWith("from"))								//Get the address			
					linesBis += lines[i].substring(0 ,lines[i].indexOf("=")+1)+request.getParameter("address").trim()+"\n";
				else if(lines[i].startsWith("user"))								//Get the user name
					linesBis += lines[i].substring(0 ,lines[i].indexOf("=")+1)+request.getParameter("user").trim()+"\n";
				else if(lines[i].startsWith("pass"))								//Get the password
					linesBis += lines[i].substring(0 ,lines[i].indexOf("=")+1)+request.getParameter("pass").trim()+"\n";
				else
					linesBis += lines[i]+"\n";
			}
			FileOutputStream fooStream = new FileOutputStream(new File(env+"/bin/common/mail.txt"), false); 
			byte[] myBytes = linesBis.getBytes();
			fooStream.write(myBytes);										//rewrite the file
			fooStream.close();
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69513");
			out.close();
			LOGGER.error("Unindentified error in MailConf doPost. Error 69513", e);
		}
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

}
