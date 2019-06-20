/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets.admin;

import static org.junit.Assert.assertTrue;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.francelabs.datafari.utils.Environment;

@PrepareForTest(Environment.class)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class TestFieldWeight {

  final static String resourcePathStr = "src/test/resources/fieldWeightTests/in";
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
    Mockito.when(Environment.getEnvironmentVariable("MAIN_DATAFARI_CONFIG_HOME")).thenReturn(tempDirectory.toFile().getAbsolutePath());
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

    // assertTrue(readFileString(solrconfigFilePath).equals(readFileString("/fieldWeightTests/out/solrconfigPost.xml")));
    // assertTrue(readFileString(customSearchHandlerFilePath).equals(readFileString("/fieldWeightTests/out/custom_search_handler_PostOneWord.incl")));
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

    // assertTrue(readFileString(solrconfigFilePath).equals(readFileString("/fieldWeightTests/out/solrconfigPost.xml")));
    // assertTrue(readFileString(customSearchHandlerFilePath).equals(readFileString("/fieldWeightTests/out/custom_search_handler_PostMultiWords.incl")));
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

    // assertTrue(readFileString(customSearchHandlerFilePath).equals(readFileString("/fieldWeightTests/out/custom_search_handler_PostOneWord.incl")));

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

    // assertTrue(readFileString(customSearchHandlerFilePath).equals(readFileString("/fieldWeightTests/out/custom_search_handler_PostMultiWords.incl")));

  }

}
