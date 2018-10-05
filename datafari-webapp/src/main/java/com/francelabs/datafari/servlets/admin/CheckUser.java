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
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.servlets.constants.OutputConstants;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/checkUser")
public class CheckUser extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(CheckUser.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public CheckUser() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    final String username = request.getParameter(UserDataService.USERNAMECOLUMN);
    final String action = request.getParameter("action");

    if (username != null) {
      if (action.equals("delete")) {
        final String currentUser = request.getUserPrincipal().getName();
        if (currentUser != null) {
          final String currentUserNoDomain = currentUser.split("@")[0];
          if (currentUser.equals(username) || currentUserNoDomain.equals(username)) {
            jsonResponse.put("allowed", false);
          } else {
            jsonResponse.put("allowed", true);
          }
        }
      }
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PARAMETERNOTWELLSET.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Problem with provided parameters");
      logger.error("Problem with provided parameters" + request.getQueryString());
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
