package com.francelabs.datafari.servlets.admin;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.francelabs.datafari.utils.AlertsConfiguration;
import com.francelabs.datafari.utils.Environment;

@PrepareForTest(Environment.class)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.crypto.*" })
public class TestAlertsAdmin {

  final static String resourcePathStr = "src/test/resources/alertsTests";
  final static String configHomeTemp = "conf";
  Path tempDirectory = null;

  @Before
  public void initialize() throws IOException {

    // create temp dir
    tempDirectory = Files.createTempDirectory(configHomeTemp);
    FileUtils.copyDirectory(new File(resourcePathStr), tempDirectory.toFile());

    // set catalina_home to temp dir
    PowerMockito.mockStatic(Environment.class);
    Mockito.when(Environment.getEnvironmentVariable("CONFIG_HOME")).thenReturn(tempDirectory.toFile().getAbsolutePath());

  }

  @Test
  public void TestSaveAlerts() throws ServletException, IOException, ManifoldCFException, NoSuchAlgorithmException, ParseException {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    Mockito.when(request.getParameter("restart")).thenReturn("false");
    Mockito.when(request.getParameter(AlertsConfiguration.HOURLY_DELAY)).thenReturn("31/07/2015/  08:42");
    Mockito.when(request.getParameter(AlertsConfiguration.DAILY_DELAY)).thenReturn("31/07/2015/  08:42");
    Mockito.when(request.getParameter(AlertsConfiguration.WEEKLY_DELAY)).thenReturn("31/07/2015/  08:42");

    Mockito.when(request.getParameter(AlertsConfiguration.SMTP_ADDRESS)).thenReturn("SMTP_ADRESS_Test");
    Mockito.when(request.getParameter(AlertsConfiguration.SMTP_FROM)).thenReturn("SMTP_FROM_Test");
    Mockito.when(request.getParameter(AlertsConfiguration.SMTP_USER)).thenReturn("SMTP_USER_Test");
    Mockito.when(request.getParameter(AlertsConfiguration.SMTP_PASSWORD)).thenReturn("SMTP_PASSWORD_Test");

    Mockito.when(request.getParameter(AlertsConfiguration.DATABASE_HOST)).thenReturn("DATABASE_HOST_Test");
    Mockito.when(request.getParameter(AlertsConfiguration.DATABASE_PORT)).thenReturn("DATABASE_PORT_Test");
    Mockito.when(request.getParameter(AlertsConfiguration.DATABASE_NAME)).thenReturn("DATABASE_NAME_Test");
    Mockito.when(request.getParameter(AlertsConfiguration.DATABASE_COLLECTION)).thenReturn("DATABASE_COLLECTION_Test");

    final StringWriter sw = new StringWriter();
    final PrintWriter writer = new PrintWriter(sw);
    Mockito.when(response.getWriter()).thenReturn(writer);

    new alertsAdmin().doPost(request, response);

    writer.flush(); // it may not have been flushed yet...

    final JSONParser parser = new JSONParser();
    final JSONObject jsonResponse = (JSONObject) parser.parse(sw.toString());
    assertTrue((Long) jsonResponse.get("code") == 0);

    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.HOURLY_DELAY).equals("31/07/2015/08:42"));
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.DAILY_DELAY).equals("31/07/2015/08:42"));
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.WEEKLY_DELAY).equals("31/07/2015/08:42"));

    // Properties mails
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_ADDRESS).equals("SMTP_ADRESS_Test"));
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_FROM).equals("SMTP_FROM_Test"));
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_USER).equals("SMTP_USER_Test"));
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_PASSWORD).equals("SMTP_PASSWORD_Test"));

    // Properties Database
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.DATABASE_HOST).equals("DATABASE_HOST_Test"));
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.DATABASE_PORT).equals("DATABASE_PORT_Test"));
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.DATABASE_NAME).equals("DATABASE_NAME_Test"));
    assertTrue(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.DATABASE_COLLECTION).equals("DATABASE_COLLECTION_Test"));
  }
}
