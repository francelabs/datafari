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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

/**
 * Javadoc
 *
 * This servlet is used to modify or download the Synonyms files of the
 * FileShare core It is only called by the Synonyms.html. doGet is used to print
 * the content of the file or download it. doPost is used to confirm the
 * modifications of the file. The semaphores (one for each language) are created
 * in the constructor.
 *
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/Synonyms")
public class Synonyms extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final String confname = "FileShare";
  private final String server = Core.FILESHARE.toString();
  private final String env;
  private final String langFolder = "lang";
  private final static Logger LOGGER = LogManager.getLogger(Synonyms.class.getName());

  /**
   * @throws InterruptedException
   * @throws IOException
   * @see HttpServlet#HttpServlet() Gets the list of the languages Creates a
   *      semaphore for each of them
   */
  public Synonyms() {
    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + "/solr/solrcloud/FileShare/conf/lang";
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
      if (request.getParameter("language") != null) { // Print the
        // content
        // of the
        // file

        try {
          final String filename = "synonyms_" + request.getParameter("language").toString() + ".txt";
          final JSONObject jsonResponse = new JSONObject();
          response.setCharacterEncoding("utf8");
          response.setContentType("application/json");
          final Map<String, String> synonymsList = new HashMap<>();
          final String filepath = env + "/";
          try (BufferedReader br = new BufferedReader(new FileReader(new File(filepath + filename)))) {
            String line;
            while ((line = br.readLine()) != null) {
              if (!line.startsWith("#")) {
                if (line.contains("=>")) {
                  final String[] synpair = line.split("=>");
                  synonymsList.put(synpair[0].trim(), synpair[1].trim());
                }
              }
            }
          }
          jsonResponse.put("synonymsList", synonymsList);
          final PrintWriter out = response.getWriter();
          out.print(jsonResponse); // returns the content
          // of the file
          out.close();
          return;
        } catch (final IOException e) {
          LOGGER.error("Error while reading the synonyms_" + request.getParameter("language") + ".txt file in Synonyms servlet, please make sure the file exists and is located in " + env + "/solr/solrcloud/" + server + "/conf/" + ". Error 69018", e);
          final PrintWriter out = response.getWriter();
          out.append("Error while reading the synonyms file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69018");
          out.close();
          return;
        }

      }
    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69506");
      out.close();
      LOGGER.error("Unindentified error in Synonyms doGet. Error 69506", e);
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
      out.append("Error while getting the Solr core, please make sure the core dedicated to PromoLinks has booted up. Error code : 69000");
      out.close();
      LOGGER.error("Error while getting the Solr core in doGet, admin servlet, make sure the core dedicated to Promolink has booted up and is still called promolink or that the code has been changed to match the changes. Error 69000 ", e1);
      return;

    } catch (final Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      if (request.getParameter("synonymsList") == null) { // the user load an
        // other page

      } else { // The user clicked on confirm modification
        final String language = request.getParameter("language").toLowerCase();
        final String filePath = env + "/synonyms_" + language + ".txt";
        final File file = new File(filePath);
        final File tempFile = File.createTempFile("synonyms_" + language, ".txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file)); BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
          String line = br.readLine();
          while (line != null && line.startsWith("#")) {
            bw.write(line + "\n");
            line = br.readLine();
          }
          final JSONParser parser = new JSONParser();
          final JSONObject synonymsList = (JSONObject) parser.parse(request.getParameter("synonymsList"));
          for (final Object words : synonymsList.keySet()) {
            bw.write(words + " => " + synonymsList.get(words.toString()) + "\n");
          }

        } catch (final IOException e) {
          LOGGER.error("Error while rewriting the file synonyms_" + language + " Synonyms Servlet's doPost. Error 69019", e);
          final PrintWriter out = response.getWriter();
          out.append("Error while rewriting the synonyms file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69015");
          out.close();
          tempFile.delete();
          return;
        }

       
        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        server.uploadFile(env,"synonyms_" + language + ".txt", Core.FILESHARE.toString(),langFolder);
        Thread.sleep(1000);
        server.reloadCollection(Core.FILESHARE.toString());
        
        List<String> collectionsList = null;

        final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
        if (!config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).equals("")) {
          collectionsList = Arrays.asList(config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).split(","));
        }
        if (collectionsList != null) {
          for (String object: collectionsList) {
            server.uploadFile(env,"synonyms_" + language + ".txt",object,langFolder);
            Thread.sleep(1000);
            server.reloadCollection(object);
          }
        }
        



        
      }
    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69507");
      out.close();
      LOGGER.error("Unindentified error in Synonyms doPost. Error 69507", e);
    }
  }

  static String readFile(final String path, final Charset encoding) // Read
  // the
  // file
      throws IOException {
    final byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }
}