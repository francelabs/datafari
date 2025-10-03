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
import org.json.simple.JSONObject;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.AuthenticatedUserName;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/deleteRole")
public class DeleteRole extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(DeleteRole.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DeleteRole() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    boolean allOK = true;
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    if (request.getParameter(UserDataService.USERNAMECOLUMN) != null && request.getParameter(UserDataService.ROLECOLUMN) != null) {
      final User user = new User(request.getParameter(UserDataService.USERNAMECOLUMN).toString(), "");
      try {
        user.deleteRole(request.getParameter(UserDataService.ROLECOLUMN).toString());
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, "User deleted with success");
      } catch (final DatafariServerException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Datafari isn't connected to Database");
        logger.error("Unable to delete role", e);
        allOK = false;
      }
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Problem with query");
      logger.error("Problem with query, some parameters are empty or missing: " + request.getQueryString());
      allOK = false;
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

    String authenticatedUserName = AuthenticatedUserName.getName(request);
    String affectedUser = request.getParameter(UserDataService.USERNAMECOLUMN) == null ? "null"
        : request.getParameter(UserDataService.USERNAMECOLUMN).toString();
    String role = request.getParameterValues(UserDataService.ROLECOLUMN) == null ? "null"
        : request.getParameterValues(UserDataService.ROLECOLUMN).toString();
    if (allOK) {
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
          "Removed role " + role + " from user " + affectedUser);
    } else {
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
          "Error trying to remove role " + role + " from user " + affectedUser);
    }
  }
}
