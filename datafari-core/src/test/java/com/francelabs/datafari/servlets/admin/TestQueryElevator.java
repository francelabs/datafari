package com.francelabs.datafari.servlets.admin;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.TestUtils;

@PrepareForTest(Environment.class)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class TestQueryElevator {

  final static String resourcePathStr = "src/test/resources/queryElevatorTests/in";
  final static String datafariHomeTemp = "datafari_home";
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
    final JSONObject jsonExpected = new JSONObject(TestUtils.readResource("/queryElevatorTests/out/getQueries.json"));

    JSONAssert.assertEquals(jsonResponse, jsonExpected, true);

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
    final JSONObject jsonExpected = new JSONObject(TestUtils.readResource("/queryElevatorTests/out/getDocs.json"));

    JSONAssert.assertEquals(jsonResponse, jsonExpected, true);
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

    final Diff diff = DiffBuilder.compare(TestUtils.readFile(new File(tempDirectory.toFile(), "solr/solrcloud/FileShare/conf/elevate.xml")))
        .withTest(TestUtils.readResource("/queryElevatorTests/out/elevateUpNewDoc.xml")).build();
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

    final Diff diff = DiffBuilder.compare(TestUtils.readFile(new File(tempDirectory.toFile(), "solr/solrcloud/FileShare/conf/elevate.xml")))
        .withTest(TestUtils.readResource("/queryElevatorTests/out/elevateUpExistingDoc.xml")).build();
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

    final Diff diff = DiffBuilder.compare(TestUtils.readFile(new File(tempDirectory.toFile(), "solr/solrcloud/FileShare/conf/elevate.xml")))

        .withTest(TestUtils.readResource("/queryElevatorTests/out/elevateDownDoc.xml")).build();
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

    final Diff diff = DiffBuilder.compare(TestUtils.readFile(new File(tempDirectory.toFile(), "solr/solrcloud/FileShare/conf/elevate.xml")))

        .withTest(TestUtils.readResource("/queryElevatorTests/out/elevateMultiDocsReplace.xml")).build();
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

    final Diff diff = DiffBuilder.compare(TestUtils.readFile(new File(tempDirectory.toFile(), "solr/solrcloud/FileShare/conf/elevate.xml")))

        .withTest(TestUtils.readResource("/queryElevatorTests/out/elevateMultiDocsNew.xml")).build();
    assertFalse(diff.toString(), diff.hasDifferences());
  }

}
