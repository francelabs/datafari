package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/getAllUsersAndRolesApache")
public class GetAllUsersAndRolesApache extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(GetAllUsersAndRolesApache.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public GetAllUsersAndRolesApache() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final List<String> usersList = new ArrayList<String>();
    usersList.add("apacheadmin");
    usersList.add("solradmin");
    usersList.add("monitadmin");
    usersList.add("glancesadmin");
    if (usersList != null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, usersList);
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Datafari isn't connected to Cassandra");
      logger.error("Datafari isn't connected to Cassandra");
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
