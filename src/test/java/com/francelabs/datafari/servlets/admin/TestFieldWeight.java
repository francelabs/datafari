package com.francelabs.datafari.servlets.admin;

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

@RunWith(MockitoJUnitRunner.class)
public class TestFieldWeight {

	final static String solrconfigFilePath = "src/test/resources/fieldWeightTests/in/solr/solr_home/FileShare/conf/solrconfig.xml";
	final static String solrconfigBackupFilePath = "src/test/resources/fieldWeightTests/in/solr/solr_home/FileShare/conf/solrconfig_backup.xml";
	final static String customSearchHandlerFilePath = "src/test/resources/fieldWeightTests/in/solr/solr_home/FileShare/conf/customs_solrconfig/custom_search_handler.incl";
	final static String customSearchHandlerBackupFilePath = "src/test/resources/fieldWeightTests/in/solr/solr_home/FileShare/conf/customs_solrconfig/custom_search_handler_backup.incl";

	final static String customSearchHandlerFilePath2 = "src/test/resources/fieldWeightTests/in_2/solr/solr_home/FileShare/conf/customs_solrconfig/custom_search_handler.incl";
	final static String customSearchHandlerBackupFilePath2 = "src/test/resources/fieldWeightTests/in_2/solr/solr_home/FileShare/conf/customs_solrconfig/custom_search_handler_backup.incl";

	@Before
	public void initialize() throws IOException {
		final File currentDirFile = new File(".");
		final String helper = currentDirFile.getAbsolutePath();
		final String currentDir = helper.substring(0, currentDirFile.getCanonicalPath().length());
		System.setProperty("DATAFARI_HOME", currentDir + "/src/test/resources/fieldWeightTests/in/");
		saveSolrConf();
		saveCustomSearchHandler();
	}

	private void initializeExternal() throws IOException {
		final File currentDirFile = new File(".");
		final String helper = currentDirFile.getAbsolutePath();
		final String currentDir = helper.substring(0, currentDirFile.getCanonicalPath().length());
		System.setProperty("DATAFARI_HOME", currentDir + "/src/test/resources/fieldWeightTests/in_2/");
		saveCustomSearchHandler2();
	}

	private void cleanExternal() throws IOException {
		restoreCustomSearchHandler2();
	}

	@After
	public void clean() throws IOException {
		restoreSolrConf();
		restoreCustomSearchHandler();
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
		final JSONObject jsonExpected = new JSONObject(readFile("src/test/resources/fieldWeightTests/out/getFields.json"));

		assertTrue(jsonResponse.toString().equals(jsonExpected.toString()));
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

		assertTrue(sw.toString().equals("50"));
	}

	@Test
	public void TestFieldWeightGetWeightOneWordExternal() throws ServletException, IOException {
		initializeExternal();
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("qf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		assertTrue(sw.toString().equals("50"));
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

		assertTrue(sw.toString().equals("500"));
	}

	@Test
	public void TestFieldWeightGetWeightMultiWordsExternal() throws ServletException, IOException {
		initializeExternal();
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getParameter("type")).thenReturn("pf");
		Mockito.when(request.getParameter("field")).thenReturn("title_fr");
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(writer);

		new FieldWeight().doGet(request, response);
		writer.flush(); // it may not have been flushed yet...

		assertTrue(sw.toString().equals("500"));
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

		assertTrue(readFile(solrconfigFilePath).equals(readFile("src/test/resources/fieldWeightTests/out/solrconfigPost.xml")));
		assertTrue(readFile(customSearchHandlerFilePath)
				.equals(readFile("src/test/resources/fieldWeightTests/out/custom_search_handler_PostOneWord.incl")));
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

		assertTrue(readFile(solrconfigFilePath).equals(readFile("src/test/resources/fieldWeightTests/out/solrconfigPost.xml")));
		assertTrue(readFile(customSearchHandlerFilePath)
				.equals(readFile("src/test/resources/fieldWeightTests/out/custom_search_handler_PostMultiWords.incl")));
	}

	@Test
	public void TestFieldWeightPostWeightOneWordExternal() throws ServletException, IOException {
		initializeExternal();
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

		assertTrue(readFile(customSearchHandlerFilePath2)
				.equals(readFile("src/test/resources/fieldWeightTests/out/custom_search_handler_PostOneWord.incl")));

		cleanExternal();
	}

	@Test
	public void TestFieldWeightPostWeightMultiWordsExternal() throws ServletException, IOException {
		initializeExternal();
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

		assertTrue(readFile(customSearchHandlerFilePath2)
				.equals(readFile("src/test/resources/fieldWeightTests/out/custom_search_handler_PostMultiWords.incl")));

		cleanExternal();
	}

	private void saveSolrConf() throws IOException {
		final OutputStream os = new FileOutputStream(solrconfigBackupFilePath);
		final Path path = new File(solrconfigFilePath).toPath();
		Files.copy(path, os);
	}

	private void restoreSolrConf() throws IOException {
		final Path target = new File(solrconfigFilePath).toPath();
		final Path source = new File(solrconfigBackupFilePath).toPath();
		Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
	}

	private void saveCustomSearchHandler() throws IOException {
		final OutputStream os = new FileOutputStream(customSearchHandlerBackupFilePath);
		final Path path = new File(customSearchHandlerFilePath).toPath();
		Files.copy(path, os);
	}

	private void restoreCustomSearchHandler() throws IOException {
		final Path target = new File(customSearchHandlerFilePath).toPath();
		final Path source = new File(customSearchHandlerBackupFilePath).toPath();
		Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
	}

	private void saveCustomSearchHandler2() throws IOException {
		final OutputStream os = new FileOutputStream(customSearchHandlerBackupFilePath2);
		final Path path = new File(customSearchHandlerFilePath2).toPath();
		Files.copy(path, os);
	}

	private void restoreCustomSearchHandler2() throws IOException {
		final Path target = new File(customSearchHandlerFilePath2).toPath();
		final Path source = new File(customSearchHandlerBackupFilePath2).toPath();
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
