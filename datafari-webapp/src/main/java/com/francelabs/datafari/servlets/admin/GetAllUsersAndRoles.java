package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.AuthenticatedUserName;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/getAllUsersAndRoles")
public class GetAllUsersAndRoles extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(GetAllUsersAndRoles.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public GetAllUsersAndRoles() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    boolean allOK = true;
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    JSONArray usersList = new JSONArray();
    final String usersType = request.getParameter("usersType");
    if (usersType != null && usersType.contentEquals("Datafari")) {
      usersList = User.getAllDatafariUsers();
    } else if (usersType != null && usersType.contentEquals("AD")) {
      usersList = User.getAllADUsers();
    } else {
      usersList = User.getAllUsers();
    }
    if (usersList != null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, usersList);
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Datafari isn't connected to Cassandra");
      logger.error("Datafari isn't connected to Cassandra");
      allOK = false;
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

    String authenticatedUserName = AuthenticatedUserName.getName(request);
    if (allOK) {
      AuditLogUtil.log("Cassandra", authenticatedUserName, request.getRemoteAddr(),
          "Requested the list of all users and roles of type " + usersType);
    } else {
      AuditLogUtil.log("Cassandra", authenticatedUserName, request.getRemoteAddr(),
          "Error requesting the list of all users and roles of type " + usersType);
    }
  }

}
