package com.francelabs.datafari.servlets.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

@RunWith(MockitoJUnitRunner.class)
public class TestQueryElevator {

	final static String elevateFilePath = "src/test/resources/queryElevatorTests/in/solr/solrcloud/tmp/elevate.xml";
	final static String elevateBackupFilePath = "src/test/resources/queryElevatorTests/in/solr/solrcloud/tmp/elevate_backup.xml";

	@Before
	public void initialize() throws IOException {
		final File currentDirFile = new File(".");
		final String helper = currentDirFile.getAbsolutePath();
		final String currentDir = helper.substring(0, currentDirFile.getCanonicalPath().length());
		System.setProperty("DATAFARI_HOME", currentDir + "/src/test/resources/queryElevatorTests/in/");
		saveElevateFile();
	}

	@After
	public void clean() throws IOException {
		restoreElevateFile();
	}

	@Test
	public void TestQueryElevatorGetQueries() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("get")).thenReturn("queries");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new QueryElevator().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());
		final JSONObject jsonExpected = new JSONObject(readFile("src/test/resources/queryElevatorTests/out/getQueries.json"));

		assertTrue(jsonResponse.toString().equals(jsonExpected.toString()));
	}

	@Test
	public void TestQueryElevatorGetDocs() throws ServletException, IOException {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("get")).thenReturn("docs");
		Mockito.when(request.getParameter("query")).thenReturn("txt");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new QueryElevator().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());
		final JSONObject jsonExpected = new JSONObject(readFile("src/test/resources/queryElevatorTests/out/getDocs.json"));

		assertTrue(jsonResponse.toString().equals(jsonExpected.toString()));
	}

	@Test
	public void TestQueryElevatorPostUpNewDoc() throws ServletException, IOException {

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("action")).thenReturn("up");
		Mockito.when(request.getParameter("item")).thenReturn("/localhost/file4.txt");
		Mockito.when(request.getParameter("query")).thenReturn("txt");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new QueryElevator().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());

		assertTrue(jsonResponse.getInt("code") == 0);
		final Diff diff = DiffBuilder.compare(Input.fromFile(elevateFilePath))
				.withTest(Input.fromFile("src/test/resources/queryElevatorTests/out/elevateUpNewDoc.xml")).build();
		assertFalse(diff.toString(), diff.hasDifferences());
	}

	@Test
	public void TestQueryElevatorPostUpExistingDoc() throws ServletException, IOException {

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("action")).thenReturn("up");
		Mockito.when(request.getParameter("item")).thenReturn("/localhost/file3.txt");
		Mockito.when(request.getParameter("query")).thenReturn("txt");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new QueryElevator().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());

		assertTrue(jsonResponse.getInt("code") == 0);
		final Diff diff = DiffBuilder.compare(Input.fromFile(elevateFilePath))
				.withTest(Input.fromFile("src/test/resources/queryElevatorTests/out/elevateUpExistingDoc.xml")).build();
		assertFalse(diff.toString(), diff.hasDifferences());
	}

	@Test
	public void TestQueryElevatorPostDownDoc() throws ServletException, IOException {

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("action")).thenReturn("down");
		Mockito.when(request.getParameter("item")).thenReturn("/localhost/file3.txt");
		Mockito.when(request.getParameter("query")).thenReturn("txt");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new QueryElevator().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());

		assertTrue(jsonResponse.getInt("code") == 0);
		final Diff diff = DiffBuilder.compare(Input.fromFile(elevateFilePath))
				.withTest(Input.fromFile("src/test/resources/queryElevatorTests/out/elevateDownDoc.xml")).build();
		assertFalse(diff.toString(), diff.hasDifferences());
	}

	@Test
	public void TestQueryElevatorPostMultiDocsReplace() throws ServletException, IOException {

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("tool")).thenReturn("modify");
		final String[] postData = { "/localhost/file3.txt", "/localhost/file1.txt" };
		Mockito.when(request.getParameter("docs[]")).thenReturn(postData.toString());
		Mockito.when(request.getParameterValues("docs[]")).thenReturn(postData);
		Mockito.when(request.getParameter("query")).thenReturn("txt");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new QueryElevator().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());

		assertTrue(jsonResponse.getInt("code") == 0);
		final Diff diff = DiffBuilder.compare(Input.fromFile(elevateFilePath))
				.withTest(Input.fromFile("src/test/resources/queryElevatorTests/out/elevateMultiDocsReplace.xml")).build();
		assertFalse(diff.toString(), diff.hasDifferences());
	}

	@Test
	public void TestQueryElevatorPostMultiDocsNew() throws ServletException, IOException {

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("tool")).thenReturn("create");
		final String[] postData = { "/localhost/file5.txt", "/localhost/file6.txt" };
		Mockito.when(request.getParameter("docs[]")).thenReturn(postData.toString());
		Mockito.when(request.getParameterValues("docs[]")).thenReturn(postData);
		Mockito.when(request.getParameter("query")).thenReturn("txt");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new QueryElevator().doPost(request, response);
		writer.flush(); // it may not have been flushed yet...

		final JSONObject jsonResponse = new JSONObject(sw.toString());

		assertTrue(jsonResponse.getInt("code") == 0);
		final Diff diff = DiffBuilder.compare(Input.fromFile(elevateFilePath))
				.withTest(Input.fromFile("src/test/resources/queryElevatorTests/out/elevateMultiDocsNew.xml")).build();
		assertFalse(diff.toString(), diff.hasDifferences());
	}

	private void saveElevateFile() throws IOException {
		final OutputStream os = new FileOutputStream(elevateBackupFilePath);
		final Path path = new File(elevateFilePath).toPath();
		Files.copy(path, os);
	}

	private void restoreElevateFile() throws IOException {
		final Path target = new File(elevateFilePath).toPath();
		final Path source = new File(elevateBackupFilePath).toPath();
		Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
	}

	private String readFile(final String fileName) throws IOException {
		final BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
			final StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}
}
