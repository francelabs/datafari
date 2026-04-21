package com.francelabs.datafari.servlets.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

@WebServlet("/SearchAdministrator/MCFChangePassword")
public class MCFChangePassword extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LogManager.getLogger(MCFChangePassword.class.getName());

  private static final String MCF_ADMIN_PASSWORD_SECRET_FILE = "mcf_admin_password";

  private final String env;

  public MCFChangePassword() {
    super();

    String environnement = System.getenv("DATAFARI_HOME");
    if (environnement == null) {
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }

    env = environnement + "/mcf/mcf_home";
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    response.getWriter().print(jsonResponse);
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    boolean allOk = true;
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    final String newClearMCFPassword = request.getParameter("password");

    if (newClearMCFPassword == null || newClearMCFPassword.isBlank()) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Problem with query");
      allOk = false;
    } else {
      try {
        final String newMCFPassword = ManifoldCF.obfuscate(newClearMCFPassword);

        modifyPropertiesMCF(newMCFPassword);
        runSetGlobalPropertiesScript();
        updateMCFSecretFile(newClearMCFPassword);

        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, "MCF password changed with success");
      } catch (final ManifoldCFException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Error while obfuscating MCF password");
        LOGGER.error("Exception during MCF change password", e);
        allOk = false;
      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Error while changing MCF password");
        LOGGER.error("Unable to change MCF password", e);
        allOk = false;
      }
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    if (allOk) {
      AuditLogUtil.log("mcf", authenticatedUserName, request.getRemoteAddr(),
          "Changed MCF admin password");
    } else {
      AuditLogUtil.log("mcf", authenticatedUserName, request.getRemoteAddr(),
          "Error trying to change MCF admin password");
    }
  }

  protected void modifyPropertiesMCF(final String password) throws Exception {
    final File file = new File(env + "/properties-global.xml");

    final DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    final Document doc = dBuilder.parse(file);

    final Element element = doc.getDocumentElement();
    final NodeList nodeList = element.getElementsByTagName("property");

    if (nodeList.getLength() > 0) {
      for (int i = 0; i < nodeList.getLength(); i++) {
        final Element elementAttribute = (Element) nodeList.item(i);
        final NamedNodeMap nodeMap = elementAttribute.getAttributes();

        if (nodeMap.getLength() >= 2) {
          final Node nameNode = nodeMap.item(0);
          final Node valueNode = nodeMap.item(1);

          if ("org.apache.manifoldcf.login.password.obfuscated".equals(nameNode.getNodeValue())
              || "org.apache.manifoldcf.apilogin.password.obfuscated".equals(nameNode.getNodeValue())) {
            valueNode.setTextContent(password);
          }
        }
      }
    }

    final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    final Transformer transformer = transformerFactory.newTransformer();
    final DOMSource source = new DOMSource(doc);
    final StreamResult result = new StreamResult(new File(env + "/properties-global.xml"));
    transformer.transform(source, result);
  }

  private void runSetGlobalPropertiesScript() throws Exception {
    String datafariHome = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHome == null) {
      datafariHome = ExecutionEnvironment.getDevExecutionEnvironment();
    }

    final String mcfPath = datafariHome + "/mcf/mcf_home/";
    final String scriptName = "setglobalproperties.sh";

    final String[] command = { "/bin/bash", "-c", "cd " + mcfPath + " && bash " + scriptName };
    final ProcessBuilder p = new ProcessBuilder(command);
    final Process process = p.start();

    try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
         BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

      String line;
      while ((line = stdInput.readLine()) != null) {
        LOGGER.info(line);
      }

      String errorLine = null;
      while ((line = stdError.readLine()) != null) {
        LOGGER.warn(line);
        errorLine = line;
      }

      final int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new IllegalStateException("setglobalproperties.sh failed with exit code " + exitCode
            + (errorLine != null ? " and error: " + errorLine : ""));
      }
    }
  }

  private String getMCFAdminFilePath() {
    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (environnement == null || environnement.isBlank()) {
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }

    return environnement
        + File.separator + "secrets"
        + File.separator + MCF_ADMIN_PASSWORD_SECRET_FILE;
  }

  private void updateMCFSecretFile(final String newPassword) throws IOException {
    final Path secretPath = Path.of(getMCFAdminFilePath());

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