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

import com.francelabs.datafari.utils.ELKConfiguration;
import com.francelabs.datafari.utils.Environment;

@PrepareForTest(Environment.class)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class TestChangeELKConf {

  final static String resourcePathStr = "src/test/resources/elkTests";
  final static String configHomeTemp = "conf";
  Path tempDirectory = null;

  @Before
  public void initialize() throws IOException {
    // create temp dir
    tempDirectory = Files.createTempDirectory(configHomeTemp);
    FileUtils.copyDirectory(new File(resourcePathStr), tempDirectory.toFile());

    // set datafari_home to temp dir
    PowerMockito.mockStatic(Environment.class);
    Mockito.when(Environment.getEnvironmentVariable("CONFIG_HOME")).thenReturn(tempDirectory.toFile().getAbsolutePath());

  }

  @Test
  public void TestELKConf() throws ServletException, IOException, ParseException {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    Mockito.when(request.getParameter(ELKConfiguration.KIBANA_URI)).thenReturn("URI_Test");
    Mockito.when(request.getParameter(ELKConfiguration.AUTH_USER)).thenReturn("");
    Mockito.when(request.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF)).thenReturn("External_Test");
    Mockito.when(request.getParameter(ELKConfiguration.ELK_SERVER)).thenReturn("Server_Test");
    Mockito.when(request.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR)).thenReturn("Script_Test");

    final StringWriter sw = new StringWriter();
    final PrintWriter writer = new PrintWriter(sw);
    Mockito.when(response.getWriter()).thenReturn(writer);

    new ChangeELKConf().doPost(request, response);

    writer.flush(); // it may not have been flushed yet...

    final JSONParser parser = new JSONParser();
    final JSONObject jsonResponse = (JSONObject) parser.parse(sw.toString());
    assertTrue((Long) jsonResponse.get("code") == 0);

    assertTrue(ELKConfiguration.getInstance().getProperty(ELKConfiguration.KIBANA_URI).equals("URI_Test"));
    assertTrue(ELKConfiguration.getInstance().getProperty(ELKConfiguration.AUTH_USER).equals(""));
    assertTrue(ELKConfiguration.getInstance().getProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF).equals("External_Test"));
    assertTrue(ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_SERVER).equals("Server_Test"));
    assertTrue(ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_SCRIPTS_DIR).equals("Script_Test"));
  }
}
