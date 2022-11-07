package com.francelabs.datafari.servlets.admin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.TikaServerCreator;
import com.francelabs.datafari.utils.ZipUtils;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/createTikaServer")
public class CreateTikaServer extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(CreateTikaServer.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public CreateTikaServer() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    final String tikaType = request.getParameter("tikaType");
    final String ocrStrategy = request.getParameter("ocrStrategy");
    final String port = request.getParameter("tikaPort");
    final String host = request.getParameter("tikaHost");
    final String tmpDir = request.getParameter("tikaTempDir");
    final String installDir = request.getParameter("installDir");
    final String externalInstallation = request.getParameter("externalTika");

    int returnCode = CodesReturned.ALLOK.getValue();
    String status = "Tika server created with success";

    if (tikaType != null && host != null && port != null && installDir != null && tmpDir != null) {
      try {
        if (tikaType.contentEquals("ocr")) {
          if (ocrStrategy != null) {
            TikaServerCreator.getInstance().createTikaOCRServer(installDir, ocrStrategy, host, port, tmpDir);
          } else {
            returnCode = CodesReturned.PROBLEMQUERY.getValue();
            status = "The required parameter 'ocrStrategy' has not been provided";
            logger.error("Problem with request, The required parameter 'ocrStrategy' has not been provided");
          }
        } else if (tikaType.contentEquals("simple") || tikaType.contentEquals("entity")) {
          TikaServerCreator.getInstance().createTikaSimpleServer(installDir, host, port, tmpDir);
        }

        // If the tika server is not meant to be used locally, create a zip of the files
        if (externalInstallation != null) {
          final String zipFilePath = installDir + "/tika-server.zip";
          final File installFolderFile = new File(installDir);
          final File[] filesToZip = installFolderFile.listFiles();
          ZipUtils.zipFiles(filesToZip, zipFilePath);
          // Delete zipped files
          for (final File fileToDelete : filesToZip) {
            if (fileToDelete.isDirectory()) {
              FileUtils.deleteDirectory(fileToDelete);
            } else {
              FileUtils.forceDelete(fileToDelete);
            }
          }
        }
      } catch (final Exception e) {
        returnCode = CodesReturned.GENERALERROR.getValue();
        status = "Something went wrong during Tika server creation: " + e.getMessage();
        logger.error("Unable to create Tika server", e);
      }
    } else {
      returnCode = CodesReturned.PROBLEMQUERY.getValue();
      status = "Some required parameters are missing";
      logger.error("Problem with request, some parameters are empty or missing: " + request.getQueryString());
    }

    final PrintWriter out = response.getWriter();
    jsonResponse.put(OutputConstants.CODE, returnCode);
    jsonResponse.put(OutputConstants.STATUS, status);
    out.print(jsonResponse);
  }
}
