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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.ldap.LDAPService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.LdapMcfConfig;
import com.francelabs.datafari.utils.RealmLdapConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/modifyRealmLdap")
public class ModifyRealmLdap extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(ModifyRealmLdap.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ModifyRealmLdap() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    try {
      if (request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_NAME) != null && request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_PW) != null && request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_URL) != null
          && request.getParameter(RealmLdapConfiguration.ATTR_DOMAIN_NAME) != null) {
        boolean isConnected = true;
        try {
          LDAPService.getInstance().testLDAPConnection(request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_URL), request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_NAME),
              request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_PW));
        } catch (final NamingException e1) {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONAD.getValue());
          jsonResponse.put(OutputConstants.STATUS, "Fail to connect to AD with the setting given");
          isConnected = false;
        }
        if (isConnected) {
          final HashMap<String, String> h = new HashMap<>();
          h.put(RealmLdapConfiguration.ATTR_CONNECTION_URL, request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_URL).toString());
          h.put(RealmLdapConfiguration.ATTR_CONNECTION_NAME, request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_NAME).toString());
          h.put(RealmLdapConfiguration.ATTR_CONNECTION_PW, ManifoldCF.obfuscate(request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_PW).toString()));
          h.put(RealmLdapConfiguration.ATTR_DOMAIN_NAME, request.getParameter(RealmLdapConfiguration.ATTR_DOMAIN_NAME).toString());
          h.put(RealmLdapConfiguration.ATTR_SUBTREE, request.getParameter(RealmLdapConfiguration.ATTR_SUBTREE).toString());
          final String[] listOfNode = request.getParameter(RealmLdapConfiguration.ATTR_DOMAIN_NAME).toString().split(",");
          final StringBuilder suffixAttribute = new StringBuilder();
          for (int i = 0; i < listOfNode.length; i++) {
            final String[] element = listOfNode[i].split("=");
            if (!element[0].equals("dc"))
              continue;
            suffixAttribute.append(element[1]);
            if (i < listOfNode.length - 1)
              suffixAttribute.append(".");
          }
          final String urlPort = request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_URL).toString().split("ldap://")[1];
          final String url = urlPort.split(":")[0];
          h.put(LdapMcfConfig.attributeUsername, request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_NAME).toString());
          h.put(LdapMcfConfig.attributeDomainController, url);
          h.put(LdapMcfConfig.attributePassword, ManifoldCF.obfuscate(request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_PW)));
          h.put(LdapMcfConfig.attributeSuffix, suffixAttribute.toString());
          try {
            LdapMcfConfig.update(h);
            RealmLdapConfiguration.setConfig(h, request);
            jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
            jsonResponse.put(OutputConstants.STATUS, "200 ALL OK");
          } catch (SAXException | ParserConfigurationException e) {
            logger.error(e);
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "Problem with XML And JSON Manipulation");
          }
        }

      } else {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Problem with query");
      }
    } catch (

    final ManifoldCFException e)

    {
      logger.error(e);
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    req.setCharacterEncoding("utf8");
    resp.setContentType("application/json");

    try {
      final HashMap<String, String> h = RealmLdapConfiguration.getConfig(req);
      jsonResponse.put("code", CodesReturned.ALLOK.getValue());
      jsonResponse.put(RealmLdapConfiguration.ATTR_CONNECTION_URL, h.get(RealmLdapConfiguration.ATTR_CONNECTION_URL));
      jsonResponse.put(RealmLdapConfiguration.ATTR_CONNECTION_NAME, h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME));
      jsonResponse.put(RealmLdapConfiguration.ATTR_CONNECTION_PW, h.get(RealmLdapConfiguration.ATTR_CONNECTION_PW));
      jsonResponse.put(RealmLdapConfiguration.ATTR_DOMAIN_NAME, h.get(RealmLdapConfiguration.ATTR_DOMAIN_NAME));
      jsonResponse.put(RealmLdapConfiguration.ATTR_SUBTREE, h.get(RealmLdapConfiguration.ATTR_SUBTREE));
    } catch (SAXException | ParserConfigurationException | ManifoldCFException e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Problem with XML Manipulation");
      logger.error(e);
    }

    final PrintWriter out = resp.getWriter();
    out.print(jsonResponse);
  }

}
