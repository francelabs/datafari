package com.francelabs.datafari.servlets.admin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
import com.francelabs.datafari.utils.Environment;

@WebServlet("/SearchAdministrator/changePassword")
public class ChangePassword extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(ChangePassword.class.getName());

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD_SECRET_FILE = "datafari_admin_password";

  public ChangePassword() {
    super();
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    boolean allOk = true;
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    if (request.getParameter(UserDataService.USERNAMECOLUMN) != null
        && request.getParameter(UserDataService.PASSWORDCOLUMN) != null) {

      final String username = request.getParameter(UserDataService.USERNAMECOLUMN);
      final String newPassword = request.getParameter(UserDataService.PASSWORDCOLUMN);

      final User user = new User(username, "");

      try {
        user.changePassword(newPassword);

        if (ADMIN_USERNAME.equals(username)) {
          updateAdminSecretFile(newPassword);
        }

        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Password changed with success");
      } catch (final DatafariServerException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Datafari isn't connected to Database");
        logger.error("Impossible to change the password", e);
        allOk = false;
      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.toString());
        jsonResponse.put(OutputConstants.STATUS, "Error while updating password secret file");
        logger.error("Password changed in database but failed to update admin secret file", e);
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
        : request.getParameter(UserDataService.USERNAMECOLUMN);

    if (allOk) {
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
          "Changed password for user " + affectedUser);
    } else {
      AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
          "Error trying to change password for user " + affectedUser);
    }
  }
  
  private String getDatafariAdminFilePath() {
    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");
    return environnement
        + File.separator + "secrets"
        + File.separator + ADMIN_PASSWORD_SECRET_FILE;
  }

  private void updateAdminSecretFile(final String newPassword) throws IOException {
    final Path secretPath = Path.of(getDatafariAdminFilePath());

    Files.writeString(
        secretPath,
        newPassword,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
    );
  }
}