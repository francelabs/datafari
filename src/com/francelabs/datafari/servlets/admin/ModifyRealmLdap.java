package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.service.ldap.LDAPService;
import com.francelabs.datafari.utils.LdapMcfConfig;
import com.francelabs.datafari.utils.RealmLdapConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/modifyRealmLdap")
public class ModifyRealmLdap extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(ModifyRealmLdap.class.getName());  
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ModifyRealmLdap() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject jsonResponse = new JSONObject();
		request.setCharacterEncoding("utf8");
		response.setContentType("application/json");
		try{
			if (request.getParameter(RealmLdapConfiguration.attributeConnectionName)!=null && request.getParameter(RealmLdapConfiguration.attributeConnectionPassword)!=null
					&& request.getParameter(RealmLdapConfiguration.attributeConnectionURL)!=null && request.getParameter(RealmLdapConfiguration.attributeDomainName)!=null ){
				boolean isConnected = true;
				try {
					LDAPService.getInstance().testLDAPConnection(request.getParameter(RealmLdapConfiguration.attributeConnectionURL),
							request.getParameter(RealmLdapConfiguration.attributeConnectionName), request.getParameter(RealmLdapConfiguration.attributeConnectionPassword));
				} catch (NamingException e1) {
					jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONLDAP).put("statut", "Fail to connect to LDAP with the setting given");
					isConnected = false;
				}
				if (isConnected){
					HashMap<String,String> h = new HashMap<String,String>();
					h.put(RealmLdapConfiguration.attributeConnectionURL , request.getParameter(RealmLdapConfiguration.attributeConnectionURL).toString());
					h.put(RealmLdapConfiguration.attributeConnectionName , request.getParameter(RealmLdapConfiguration.attributeConnectionName).toString());
					h.put(RealmLdapConfiguration.attributeConnectionPassword , request.getParameter(RealmLdapConfiguration.attributeConnectionPassword).toString());
					h.put(RealmLdapConfiguration.attributeDomainName, request.getParameter(RealmLdapConfiguration.attributeDomainName).toString());
					String[] listOfNode = request.getParameter(RealmLdapConfiguration.attributeDomainName).toString().split(",");
					StringBuilder suffixAttribute = new StringBuilder();
					for (int i=0; i<listOfNode.length ; i++){
						String [] element = listOfNode[i].split("=");
						if (!element[0].equals("dc"))
							continue;
						suffixAttribute.append(element[1]);
						if (i<listOfNode.length-1)
							suffixAttribute.append(".");
					}
					String urlPort = request.getParameter(RealmLdapConfiguration.attributeConnectionURL).toString().split("ldap://")[1];
					String url = urlPort.split(":")[0];
					h.put(LdapMcfConfig.attributeUsername,request.getParameter(RealmLdapConfiguration.attributeConnectionName).toString());
					h.put(LdapMcfConfig.attributeDomainController,url);
					h.put(LdapMcfConfig.attributePassword, request.getParameter(RealmLdapConfiguration.attributeConnectionPassword));
					h.put(LdapMcfConfig.attributeSuffix, suffixAttribute.toString());
					try {
						if (CodesReturned.ALLOK == RealmLdapConfiguration.setConfig(h)){
							if (CodesReturned.ALLOK == LdapMcfConfig.update(h))
								jsonResponse.put("code", CodesReturned.ALLOK).put("statut", "200 ALL OK");
							else
								jsonResponse.put("code", CodesReturned.GENERALERROR).put("statut", "Problem with XML And JSON Manipulation");
						}else{
							jsonResponse.put("code", CodesReturned.GENERALERROR).put("statut", "Problem with XML And JSON Manipulation");
						}
					} catch (SAXException | ParserConfigurationException e) {
						logger.error(e);
						jsonResponse.put("code", CodesReturned.GENERALERROR).put("statut", "Problem with XML And JSON Manipulation");
					}
				}
				
			}else{
				jsonResponse.put("code", CodesReturned.PROBLEMQUERY).put("statut", "Problem with query");
			}
		}catch (JSONException e) {
			logger.error(e);
		}
		PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
			JSONObject jsonResponse = new JSONObject();
			req.setCharacterEncoding("utf8");
			resp.setContentType("application/json");
			try {
				try{
					HashMap<String,String> h = RealmLdapConfiguration.getConfig();
					jsonResponse.put("code",CodesReturned.ALLOK);
					jsonResponse.put(RealmLdapConfiguration.attributeConnectionURL, h.get(RealmLdapConfiguration.attributeConnectionURL));
					jsonResponse.put(RealmLdapConfiguration.attributeConnectionName, h.get(RealmLdapConfiguration.attributeConnectionName));
					jsonResponse.put(RealmLdapConfiguration.attributeConnectionPassword, h.get(RealmLdapConfiguration.attributeConnectionPassword));
					jsonResponse.put(RealmLdapConfiguration.attributeDomainName, h.get(RealmLdapConfiguration.attributeDomainName));
				} catch (SAXException | ParserConfigurationException e) {
					jsonResponse.put("code", CodesReturned.GENERALERROR).put("statut", "Problem with XML Manipulation");
					logger.error(e);
				} 
			} catch (JSONException e) {
				logger.error(e);
			}
			PrintWriter out = resp.getWriter();
			out.print(jsonResponse);
	}

}
