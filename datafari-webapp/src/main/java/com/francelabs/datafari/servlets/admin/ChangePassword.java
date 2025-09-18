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
@WebServlet("/SearchAdministrator/changePassword")
public class ChangePassword extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(ChangePassword.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ChangePassword() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    boolean allOk = true;
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    if (request.getParameter(UserDataService.USERNAMECOLUMN) != null
        && request.getParameter(UserDataService.PASSWORDCOLUMN) != null) {
      final User user = new User(request.getParameter(UserDataService.USERNAMECOLUMN).toString(), "");
      try {
        user.changePassword(request.getParameter(UserDataService.PASSWORDCOLUMN).toString());
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, "User deleted with success");
      } catch (final DatafariServerException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Datafari isn't connected to Database");
        logger.error("Impossible to change the password", e);
        allOk = false;
      }
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Problem with query");
      logger.error("Problem with query, some parameters are empty or missing: " + request.getQueryString());
      allOk = false;
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

    String authenticatedUserName = AuthenticatedUserName.getName(request);
    String affectedUser = request.getParameter(UserDataService.USERNAMECOLUMN) == null ? "null"
        : request.getParameter(UserDataService.USERNAMECOLUMN).toString();
    if (allOk) {
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
          "Changed password for user " + affectedUser);
    } else {
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
          "Error trying to change password for user " + affectedUser);
    }
  }
}
