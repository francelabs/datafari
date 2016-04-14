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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestQueryElevator {

	@Before
	public void initialize() throws IOException {
		final File currentDirFile = new File(".");
		final String helper = currentDirFile.getAbsolutePath();
		final String currentDir = helper.substring(0, currentDirFile.getCanonicalPath().length());
		System.setProperty("DATAFARI_HOME", currentDir + "/src/test/resources/queryElevatorTests/in/");
	}

	@Test
	public void TestQueryElevatorGetQueries() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("get")).thenReturn("queries");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		final JSONObject jsonResponse;

		new QueryElevator().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		jsonResponse = new JSONObject(sw.toString());

		assertTrue(jsonResponse.has("queries"));
	}
}
