package com.francelabs.datafari.servlets.admin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.utils.DatafariMainConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchExpert/zeppelinRedirect")
public class ZeppelinRedirect extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ZeppelinRedirect() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf8");

    final String notebookType = request.getParameter("notebookType");
    final String notebookId = request.getParameter("notebookId");
    final String analyticsActivation = DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.ANALYTICS_ACTIVATION).toString();

    if (analyticsActivation.contentEquals("true")) {
      if (notebookType != null && notebookId != null) {
        if (notebookType.equals("usages")) {
          response.sendRedirect("/zeppelin/#/notebook/" + notebookId);
        } else {
          response.sendRedirect("/Datafari/admin/?page=analyticsCE");
        }
      } else {
        response.sendRedirect("/Datafari/error.jsp");
      }
    } else {
      response.sendRedirect("/Datafari/admin/?page=analyticsDisabled");
    }
  }
}
