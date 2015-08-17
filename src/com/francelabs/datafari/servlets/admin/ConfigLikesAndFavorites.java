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

import com.francelabs.datafari.startup.LikesLauncher;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Servlet implementation class ConfigureLikesAndFacorites
 */
@WebServlet("/ConfigureLikesAndFavorites")
public class ConfigLikesAndFavorites extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ConfigLikesAndFavorites.class.getName());
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConfigLikesAndFavorites() {
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
		if (request.getParameter("enable")!=null){
			String enable = request.getParameter("enable");
			request.setCharacterEncoding("utf8");
			response.setContentType("application/json");
			boolean error=false;
			if (enable.equals("true")){
				error = ScriptConfiguration.setProperty(StringsDatafariProperties.LIKESANDFAVORTES, "true");
				LikesLauncher.startScheduler();
			}else{
				error = ScriptConfiguration.setProperty(StringsDatafariProperties.LIKESANDFAVORTES, "false");
				LikesLauncher.shutDown();
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
			String isEnabled = ScriptConfiguration.getProperty(StringsDatafariProperties.LIKESANDFAVORTES);
			try {
				jsonResponse.put("code", 0)
				    .put("isEnabled",isEnabled);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.error(e);
			}
			
		}
		PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}

}
