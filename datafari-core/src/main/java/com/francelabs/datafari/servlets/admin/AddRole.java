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

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.user.UserConstants;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/addRole")
public class AddRole extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(AddRole.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public AddRole() {
    super();
    // TODO Auto-generated constructor stub
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
      if (request.getParameter(UserDataService.USERNAMECOLUMN) != null && request.getParameter(UserDataService.ROLECOLUMN) != null) {
        final User user = new User(request.getParameter(UserConstants.USERNAMECOLUMN).toString(), "");
        try {
          user.addRole(request.getParameter(UserDataService.ROLECOLUMN).toString());
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
          jsonResponse.put(OutputConstants.STATUS, "Role add  with success to " + request.getParameter(UserConstants.USERNAMECOLUMN).toString());
        } catch (final DatafariServerException e) {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
          jsonResponse.put(OutputConstants.STATUS, "Datafari isn't connected to DB");
        }
      } else {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Problem with query");
      }
    } catch (final Exception e) {
      logger.error(e);
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
