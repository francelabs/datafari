/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

/**
 * Javadoc
 *
 * This servlet is used to modify or download the Protwords files of the
 * FileShare core It is only called by theProtWords.html. doGet is used to print
 * the content of the file or download it. doPost is used to confirm the
 * modifications of the file. The semaphore is created in the constructor.
 *
 *
 */
@WebServlet("/admin/ProtWords")
public class ProtWords extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final String confname = "FileShare";
  private final String server = Core.FILESHARE.toString();
  private final String env;
  private final String content;
  private final static Logger LOGGER = LogManager.getLogger(ProtWords.class.getName());

  /**
   * @throws IOException
   * @see HttpServlet#HttpServlet() Gets the list of the languages Creates a
   *      semaphore for each of them
   */
  public ProtWords() throws IOException {
    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + "/solr/solrcloud/FileShare/conf";
    content = "ALL";

  }

  /**
   * @throws IOException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response) used to print the content of the file or to download it If
   *      called to print it will return plain text
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {



    try {
      if (content.equals("")) {
        final PrintWriter out = response.getWriter();
        out.append(
            "Error while opening the list of languages, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69012");
        out.close();
        return;
      } else if (request.getParameter("language") != null) { // Print the
        // content of the
        // file

        try {
          final String filename = "protwords.txt";
          response.setContentType("application/octet-stream");
          final String filepath = env + "/";
          final String stopContent = readFile(filepath + filename, StandardCharsets.UTF_8);
          // get the file and put its content into a string
          response.setContentType("text/html");
          final PrintWriter out = response.getWriter();
          out.append(stopContent); // returns the content of the file
          out.close();
          return;

        } catch (final IOException e) {
          final PrintWriter out = response.getWriter();
          out.append(
              "Error while reading the protwords file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69014");
          out.close();
          LOGGER.error("Error while reading the protwords.txt file in Protwords servlet, please make sure the file exists and is located in " + env
              + "/solr/solrcloud/" + server + "/conf/" + ". Error 69014", e);
        }

      }
    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69504");
      out.close();
      LOGGER.error("Unindentified error in Protwords doGet. Error 69504", e);
    }
  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response) used to confirm the modifications and/or release the
   *      semaphore called by the confirm modification or if the user loads an
   *      other page, refreshes the page or switches language
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    IndexerServer server = null;
    try {
      server = IndexerServerManager.getIndexerServer(Core.FILESHARE);
    } catch (final IOException e1) {
      final PrintWriter out = response.getWriter();
      out.append(
          "Error while getting the Solr core, please make sure the core dedicated to FileShare has booted up. Error code : 69000");
      out.close();
      LOGGER.error(
          "Error while getting the Solr core in doGet, admin servlet, make sure the core dedicated to Promolink has booted up and is still called promolink or that the code has been changed to match the changes. Error 69000 ",
          e1);
      return;

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      if (request.getParameter("content") == null) { // the user load an other
        // page
      } else { // The user clicked on confirm modification
        File file;
        final String filePath = env + "/protwords.txt";
        file = new File(filePath);
        try {
          final FileOutputStream fooStream = new FileOutputStream(file, false); // true
          // to
          // append,
          // false
          // to
          // overwrite.
          final byte[] myBytes = request.getParameter("content").replaceAll("&gt;", ">").replaceAll("<div>|<br>|<br >", "\n")
              .replaceAll("</div>|</lines>|&nbsp;", "").getBytes();
          fooStream.write(myBytes); // rewrite the file
          fooStream.close();
          server.uploadFile(env,"protwords.txt", Core.FILESHARE.toString(),"");
          Thread.sleep(1000);
          server.reloadCollection(Core.FILESHARE.toString());

          List<String> collectionsList = null;
          final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
          if (!config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).equals("")) {
            collectionsList = Arrays.asList(config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).split(","));
          }
          
          if (collectionsList != null) {
            for (String object: collectionsList) {
              server.uploadFile(env,"protwords.txt", object,"");
              Thread.sleep(1000);
              server.reloadCollection(object);
            }
          }


        } catch (final IOException e) {
          final PrintWriter out = response.getWriter();
          out.append(
              "Error while rewriting the protwords file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69015");
          out.close();
          LOGGER.error("Error while rewriting the file protwords Protwords Servlet's doPost. Error 69015", e);
          return;
        }
      }
    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69505");
      out.close();
      LOGGER.error("Unindentified error in Protwords doPost. Error 69505", e);
    }
  }

  static String readFile(final String path, final Charset encoding) // Read the
  // file
      throws IOException {
    final byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);

  }
}