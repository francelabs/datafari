package com.francelabs.datafari.servlets.admin;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.francelabs.datafari.utils.ScriptConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class TestChangeELKConf {

	private static final String KIBANAURI = "KibanaURI";
	private static final String EXTERNALELK = "externalELK";
	private static final String ELKSERVER = "ELKServer";
	private static final String ELKSCRIPTSDIR = "ELKScriptsDir";

	private String KIBANAURI_SAVE = "KibanaURI";
	private String EXTERNALELK_SAVE = "externalELK";
	private String ELKSERVER_SAVE = "ELKServer";
	private String ELKSCRIPTSDIR_SAVE = "ELKScriptsDir";

	private void saveConf() throws IOException {
		KIBANAURI_SAVE = ScriptConfiguration.getProperty(KIBANAURI);
		EXTERNALELK_SAVE = ScriptConfiguration.getProperty(EXTERNALELK);
		ELKSERVER_SAVE = ScriptConfiguration.getProperty(ELKSERVER);
		ELKSCRIPTSDIR_SAVE = ScriptConfiguration.getProperty(ELKSCRIPTSDIR);
	}

	@After
	public void restoreConf() {
		ScriptConfiguration.setProperty(KIBANAURI, KIBANAURI_SAVE);
		ScriptConfiguration.setProperty(EXTERNALELK, EXTERNALELK_SAVE);
		ScriptConfiguration.setProperty(ELKSERVER, ELKSERVER_SAVE);
		ScriptConfiguration.setProperty(ELKSCRIPTSDIR, ELKSCRIPTSDIR_SAVE);
	}

	@Before
	public void initialize() throws IOException {
		final File currentDirFile = new File(".");
		final String helper = currentDirFile.getAbsolutePath();
		final String currentDir = helper.substring(0, currentDirFile.getCanonicalPath().length());
		System.setProperty("catalina.home", currentDir + "/tomcat");
		saveConf();
	}

	@Test
	public void TestELKConf() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter(KIBANAURI)).thenReturn("URI_Test");
		Mockito.when(request.getParameter(EXTERNALELK)).thenReturn("External_Test");
		Mockito.when(request.getParameter(ELKSERVER)).thenReturn("Server_Test");
		Mockito.when(request.getParameter(ELKSCRIPTSDIR)).thenReturn("Script_Test");
		final OutputStream os = new ByteArrayOutputStream();
		final PrintWriter writer = new PrintWriter(os);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new ChangeELKConf().doPost(request, response);

		writer.flush(); // it may not have been flushed yet...
		assertTrue(ScriptConfiguration.getProperty(KIBANAURI).contains("URI_Test"));
		assertTrue(ScriptConfiguration.getProperty(EXTERNALELK).contains("External_Test"));
		assertTrue(ScriptConfiguration.getProperty(ELKSERVER).contains("Server_Test"));
		assertTrue(ScriptConfiguration.getProperty(ELKSCRIPTSDIR).contains("Script_Test"));
	}
}
