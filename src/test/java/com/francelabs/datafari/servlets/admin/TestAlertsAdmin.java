package com.francelabs.datafari.servlets.admin;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.francelabs.datafari.utils.AlertsConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class TestAlertsAdmin {

	// Properties frequencies
	private String HOURLY_DELAY_SAVE;
	private String DAILY_DELAY_SAVE;
	private String WEEKLY_DELAY_SAVE;

	// Properties mails
	private String SMTP_ADRESS_SAVE;
	private String SMTP_FROM_SAVE;
	private String SMTP_USER_SAVE;
	private String SMTP_PASSWORD_SAVE;

	// Properties Database
	private String DATABASE_HOST_SAVE;
	private String DATABASE_PORT_SAVE;
	private String DATABASE_NAME_SAVE;
	private String DATABASE_COLLECTION_SAVE;

	private void saveConf() throws IOException {
		HOURLY_DELAY_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.HOURLY_DELAY);
		DAILY_DELAY_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.DAILY_DELAY);
		WEEKLY_DELAY_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.WEEKLY_DELAY);

		// Properties mails
		SMTP_ADRESS_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_ADDRESS);
		SMTP_FROM_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_FROM);
		SMTP_USER_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_USER);
		SMTP_PASSWORD_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_PASSWORD);

		// Properties Database
		DATABASE_HOST_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.DATABASE_HOST);
		DATABASE_PORT_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.DATABASE_PORT);
		DATABASE_NAME_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.DATABASE_NAME);
		DATABASE_COLLECTION_SAVE = AlertsConfiguration.getProperty(AlertsConfiguration.DATABASE_COLLECTION);

	}

	@After
	public void restoreConf() {
		AlertsConfiguration.setProperty(AlertsConfiguration.HOURLY_DELAY, HOURLY_DELAY_SAVE);
		AlertsConfiguration.setProperty(AlertsConfiguration.DAILY_DELAY, DAILY_DELAY_SAVE);
		AlertsConfiguration.setProperty(AlertsConfiguration.WEEKLY_DELAY, WEEKLY_DELAY_SAVE);

		// Properties mails
		AlertsConfiguration.setProperty(AlertsConfiguration.SMTP_ADDRESS, SMTP_ADRESS_SAVE);
		AlertsConfiguration.setProperty(AlertsConfiguration.SMTP_FROM, SMTP_FROM_SAVE);
		AlertsConfiguration.setProperty(AlertsConfiguration.SMTP_USER, SMTP_USER_SAVE);
		AlertsConfiguration.setProperty(AlertsConfiguration.SMTP_PASSWORD, SMTP_PASSWORD_SAVE);

		// Properties Database
		AlertsConfiguration.setProperty(AlertsConfiguration.DATABASE_HOST, DATABASE_HOST_SAVE);
		AlertsConfiguration.setProperty(AlertsConfiguration.DATABASE_PORT, DATABASE_PORT_SAVE);
		AlertsConfiguration.setProperty(AlertsConfiguration.DATABASE_NAME, DATABASE_NAME_SAVE);
		AlertsConfiguration.setProperty(AlertsConfiguration.DATABASE_COLLECTION, DATABASE_COLLECTION_SAVE);
	}

	@Before
	public void initialize() throws IOException {
		final File currentDirFile = new File(".");
		final String helper = currentDirFile.getAbsolutePath();
		final String currentDir = helper.substring(0, currentDirFile.getCanonicalPath().length());
		System.setProperty("catalina.home", currentDir + "/src/test/resources/alertsTests");
		saveConf();
	}

	@Test
	public void TestSaveAlerts() throws ServletException, IOException {
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

		final JSONObject jsonResponse = new JSONObject(sw.toString());
		assertTrue(jsonResponse.getInt("code") == 0);

		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.HOURLY_DELAY).equals("31/07/2015/08:42"));
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.DAILY_DELAY).equals("31/07/2015/08:42"));
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.WEEKLY_DELAY).equals("31/07/2015/08:42"));

		// Properties mails
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_ADDRESS).equals("SMTP_ADRESS_Test"));
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_FROM).equals("SMTP_FROM_Test"));
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_USER).equals("SMTP_USER_Test"));
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.SMTP_PASSWORD).equals("SMTP_PASSWORD_Test"));

		// Properties Database
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.DATABASE_HOST).equals("DATABASE_HOST_Test"));
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.DATABASE_PORT).equals("DATABASE_PORT_Test"));
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.DATABASE_NAME).equals("DATABASE_NAME_Test"));
		assertTrue(AlertsConfiguration.getProperty(AlertsConfiguration.DATABASE_COLLECTION).equals("DATABASE_COLLECTION_Test"));
	}
}
