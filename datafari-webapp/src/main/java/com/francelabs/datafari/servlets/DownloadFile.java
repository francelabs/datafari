package com.francelabs.datafari.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@WebServlet("/DownloadFile")
public class DownloadFile extends HttpServlet {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final static Logger logger = LogManager.getLogger(DownloadFile.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DownloadFile() {

  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final String delete = request.getParameter("delete");
    final String filePath = request.getParameter("filePath");
    if (filePath != null && !filePath.isEmpty()) {
      final File downloadFile = new File(filePath);
      try (final FileInputStream inStream = new FileInputStream(downloadFile);) {

        // obtains ServletContext
        final ServletContext context = getServletContext();

        // gets MIME type of the file
        String mimeType = context.getMimeType(filePath);
        if (mimeType == null) {
          // set to binary type if MIME mapping not found
          mimeType = "application/octet-stream";
        }

        // modifies response
        response.setContentType(mimeType);
        response.setContentLength((int) downloadFile.length());

        // forces download
        final String headerKey = "Content-Disposition";
        final String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
        response.setHeader(headerKey, headerValue);

        // obtains response's output stream
        final OutputStream outStream = response.getOutputStream();

        final byte[] buffer = new byte[4096];
        int bytesRead = -1;

        while ((bytesRead = inStream.read(buffer)) != -1) {
          outStream.write(buffer, 0, bytesRead);
        }
        outStream.close();
      } catch (final FileNotFoundException e) {
        logger.error("Download file error: file not found", e);
      }

      if (delete != null && delete.equalsIgnoreCase("true")) {
        downloadFile.delete();
      }
    } else {
      logger.warn("Impossible to download a file : No file path provided");
    }
  }

}
