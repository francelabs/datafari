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
@WebServlet("/SearchAdministrator/isUserInBase")
public class IsUserInBase extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(IsUserInBase.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public IsUserInBase() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    boolean allOK = true;
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    if (request.getParameter(UserDataService.USERNAMECOLUMN) != null) {
      try {
        final User user = new User(request.getParameter(UserDataService.USERNAMECOLUMN).toString(), "");
        String result = null;
        if (user.isInBase()) {
          result = "true";
        } else {
          result = "false";
        }
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, result);
      } catch (final DatafariServerException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Problem with database");
        logger.error("Problem with database", e);
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
    if (allOK) {
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
          "Checked existence of user " + affectedUser);
    } else {
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
          "Error checking existence of user " + affectedUser);
    }
  }

}
