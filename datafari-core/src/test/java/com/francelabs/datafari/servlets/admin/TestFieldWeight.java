package com.francelabs.datafari.servlets.admin;

import static org.junit.Assert.assertTrue;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.TestUtils;

@PrepareForTest(Environment.class) 
@RunWith(PowerMockRunner.class)
public class TestFieldWeight {

	final static String resourcePathStr = "src/test/resources/fieldWeightTests/in";
	final static String datafariHomeTemp ="datafari_home";
	Path tempDirectory = null;
	
	
	@Before
	public void initialize() throws IOException {
		// create temp dir
		tempDirectory = Files.createTempDirectory(datafariHomeTemp);
        FileUtils.copyDirectory(new File(resourcePathStr), tempDirectory.toFile());
		
		// set datafari_home to temp dir
        PowerMockito.mockStatic(Environment.class);
        Mockito.when(Environment.getEnvironmentVariable("DATAFARI_HOME")).thenReturn(tempDirectory.toFile().getAbsolutePath());
	}


	@Test
	public void TestFieldWeightGetFields() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());
		final JSONObject jsonExpected = new JSONObject(TestUtils.readResource("/fieldWeightTests/out/getFields.json"));

		 JSONAssert.assertEquals(jsonResponse,jsonExpected, true);
	}

	@Test
	public void TestFieldWeightGetWeightOneWord() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("qf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		assertTrue(sw.toString().equals("1"));
	}

	@Test
	public void TestFieldWeightGetWeightOneWordExternal() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("qf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		assertTrue(sw.toString().equals("1"));
	}

	@Test
	public void TestFieldWeightGetWeightMultiWords() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("pf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		assertTrue(sw.toString().equals("1"));
	}

	@Test
	public void TestFieldWeightGetWeightMultiWordsExternal() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("pf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		assertTrue(sw.toString().equals("1"));
	}

	@Test
	public void TestFieldWeightPostWeightOneWord() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("qf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		Mockito.when(request.getParameter("value")).thenReturn("1");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...

		//assertTrue(readFileString(solrconfigFilePath).equals(readFileString("/fieldWeightTests/out/solrconfigPost.xml")));
		//assertTrue(readFileString(customSearchHandlerFilePath).equals(readFileString("/fieldWeightTests/out/custom_search_handler_PostOneWord.incl")));
	}

	@Test
	public void TestFieldWeightPostWeightMultiWords() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("pf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		Mockito.when(request.getParameter("value")).thenReturn("1");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...

		//assertTrue(readFileString(solrconfigFilePath).equals(readFileString("/fieldWeightTests/out/solrconfigPost.xml")));
		//assertTrue(readFileString(customSearchHandlerFilePath).equals(readFileString("/fieldWeightTests/out/custom_search_handler_PostMultiWords.incl")));
	}

	@Test
	public void TestFieldWeightPostWeightOneWordExternal() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("qf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		Mockito.when(request.getParameter("value")).thenReturn("1");
		 
		
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...
		
		//assertTrue(readFileString(customSearchHandlerFilePath).equals(readFileString("/fieldWeightTests/out/custom_search_handler_PostOneWord.incl")));

	}

	@Test
	public void TestFieldWeightPostWeightMultiWordsExternal() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("pf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		Mockito.when(request.getParameter("value")).thenReturn("1");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...

		//assertTrue(readFileString(customSearchHandlerFilePath).equals(readFileString("/fieldWeightTests/out/custom_search_handler_PostMultiWords.incl")));

	}


	
}
