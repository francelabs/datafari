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

import com.francelabs.datafari.utils.ELKConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class TestChangeELKConf {

	private String KIBANAURI_SAVE;
	private String EXTERNALELK_SAVE;
	private String ELKSERVER_SAVE;
	private String ELKSCRIPTSDIR_SAVE;

	private void saveConf() throws IOException {
		KIBANAURI_SAVE = ELKConfiguration.getProperty(ELKConfiguration.KIBANA_URI);
		EXTERNALELK_SAVE = ELKConfiguration.getProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF);
		ELKSERVER_SAVE = ELKConfiguration.getProperty(ELKConfiguration.ELK_SERVER);
		ELKSCRIPTSDIR_SAVE = ELKConfiguration.getProperty(ELKConfiguration.ELK_SCRIPTS_DIR);
	}

	@After
	public void restoreConf() {
		ELKConfiguration.setProperty(ELKConfiguration.KIBANA_URI, KIBANAURI_SAVE);
		ELKConfiguration.setProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF, EXTERNALELK_SAVE);
		ELKConfiguration.setProperty(ELKConfiguration.ELK_SERVER, ELKSERVER_SAVE);
		ELKConfiguration.setProperty(ELKConfiguration.ELK_SCRIPTS_DIR, ELKSCRIPTSDIR_SAVE);
	}

	@Before
	public void initialize() throws IOException {
		final File currentDirFile = new File(".");
		final String helper = currentDirFile.getAbsolutePath();
		final String currentDir = helper.substring(0, currentDirFile.getCanonicalPath().length());
		System.setProperty("catalina.home", currentDir + "/src/test/resources/elkTests");
		saveConf();
	}

	@Test
	public void TestELKConf() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter(ELKConfiguration.KIBANA_URI)).thenReturn("URI_Test");
		Mockito.when(request.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF)).thenReturn("External_Test");
		Mockito.when(request.getParameter(ELKConfiguration.ELK_SERVER)).thenReturn("Server_Test");
		Mockito.when(request.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR)).thenReturn("Script_Test");

		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new ChangeELKConf().doPost(request, response);

		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());
		assertTrue(jsonResponse.getInt("code") == 0);

		assertTrue(ELKConfiguration.getProperty(ELKConfiguration.KIBANA_URI).equals("URI_Test"));
		assertTrue(ELKConfiguration.getProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF).equals("External_Test"));
		assertTrue(ELKConfiguration.getProperty(ELKConfiguration.ELK_SERVER).equals("Server_Test"));
		assertTrue(ELKConfiguration.getProperty(ELKConfiguration.ELK_SCRIPTS_DIR).equals("Script_Test"));
	}
}
