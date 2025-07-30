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
import com.francelabs.datafari.licence.LicenceManagement;
import com.francelabs.datafari.service.db.AccessTokenDataServicePostgres;
import com.francelabs.datafari.service.db.StatisticsDataService;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.Alert;
import com.francelabs.datafari.user.Department;
import com.francelabs.datafari.user.Favorite;
import com.francelabs.datafari.user.Lang;
import com.francelabs.datafari.user.SavedSearch;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.licence.exception.LicenceException;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/deleteUser")
public class DeleteUser extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(DeleteUser.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DeleteUser() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    boolean allOK = true;
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final String username = request.getParameter(UserDataService.USERNAMECOLUMN).toString();
    if (username != null) {
      final User user = new User(username, "");
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, "User deleted with success");

      try {
        user.deleteUser();
        Alert.deleteAllAlerts(username);
        Department.deleteDepartment(username);
        Favorite.removeFavoritesAndLikesDB(username);
        Lang.deleteLang(username);
        SavedSearch.removeSearches(username);
        AccessTokenDataServicePostgres.getInstance().removeTokens(username);
        LicenceManagement.getInstance().removeUser(username);
        StatisticsDataService.getInstance().deleteUserStatistics(username);
      } catch (final DatafariServerException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Problem with database");
        logger.error("Unable to delete user", e);
        allOK = false;
      } catch (final LicenceException e) {
        logger.error("Licence problem", e);
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

    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    final String affectedUser = request.getParameter(UserDataService.USERNAMECOLUMN) == null ? "null" : request.getParameter(UserDataService.USERNAMECOLUMN).toString();
    if (allOK) {
      AuditLogUtil.log("Cassandra", authenticatedUserName, request.getRemoteAddr(), "Deleted user " + affectedUser + "as well as all associated data (alerts, favorites, ...).");
    } else {
      AuditLogUtil.log("Cassandra", authenticatedUserName, request.getRemoteAddr(), "Error deleting user " + affectedUser);
    }
  }
}
