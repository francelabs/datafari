package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.ActivateLDAPSolr;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/isLdapConfig")
public class IsLdapConfig extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(IsLdapConfig.class);
	private static final String LDAPACTIVATED = "ISLDAPACTIVATED";
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public IsLdapConfig() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject jsonResponse = new JSONObject();
		request.setCharacterEncoding("utf8");
		response.setContentType("application/json");
		try {
			String isLdapActivated = (String) ScriptConfiguration.getProperty(LDAPACTIVATED);
			if (isLdapActivated == null)
				isLdapActivated = "false";
			jsonResponse.put("code",CodesReturned.ALLOK).put("isActivated",isLdapActivated);
		}catch (JSONException e) {
				logger.error(e);
		}
		PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		JSONObject jsonResponse = new JSONObject();
		req.setCharacterEncoding("utf8");
		resp.setContentType("application/json");
		try {
			if (req.getParameter("isLdapActivated") == null){
				jsonResponse.put("code",CodesReturned.PROBLEMQUERY).put("statut","Query Malformed");
			}else{
				if (ScriptConfiguration.setProperty(LDAPACTIVATED,req.getParameter("isLdapActivated"))){
					jsonResponse.put("code",CodesReturned.GENERALERROR);
				}else{
					try {
						if (req.getParameter("isLdapActivated").toString().equals("true")){
								ActivateLDAPSolr.activate();
						}else{
							ActivateLDAPSolr.disactivate();
						}
						jsonResponse.put("code",CodesReturned.ALLOK)
						.put("isActivated",req.getParameter("isLdapActivated"));
					} catch (SAXException e) {
						jsonResponse.put("code",CodesReturned.GENERALERROR);
						logger.error("Fatal Error",e);
					} catch (ParserConfigurationException e) {
						jsonResponse.put("code",CodesReturned.GENERALERROR);
						logger.error("Fatal Error",e);
					} catch (TransformerException e) {
						jsonResponse.put("code",CodesReturned.GENERALERROR);
						logger.error("Fatal Error",e);
					} catch (Exception e) {
						jsonResponse.put("code",CodesReturned.GENERALERROR);
						logger.error("Fatal Error",e);					
					}
				}
			}
		} catch (JSONException e) {
			logger.error("Error",e);
		}
		PrintWriter out = resp.getWriter();
		out.print(jsonResponse);
	}

}
