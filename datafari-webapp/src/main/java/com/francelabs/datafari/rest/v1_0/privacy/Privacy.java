/*******************************************************************************
 *  * Copyright 2020 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.rest.v1_0.privacy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@RestController
public class Privacy {

    private static final String BASE_FILE_NAME = "privacyPolicyContent";
    private static final String FILE_EXTENSION = ".jsp";
    private final static Logger LOGGER = LogManager.getLogger(Privacy.class.getName());

    @GetMapping(value = "/rest/v1.0/privacy/{lang}", produces = "application/json;charset=UTF-8")
    protected String getCurrentUserAlerts(final HttpServletRequest request, @PathVariable("lang") String lang) {
        final JSONObject jsonResponse = new JSONObject();
        String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

        if (environnement == null) { // If in development environment
            environnement = ExecutionEnvironment.getDevExecutionEnvironment();
        }
        String basePath = environnement + File.separator + "tomcat" + File.separator + "webapps" + File.separator
                + "Datafari" + File.separator + "resources" + File.separator + "privacyPolicyAssets";
        
        try {
            String filepath = basePath + File.separator + BASE_FILE_NAME + "_" + lang + FILE_EXTENSION;
            String fileContent = readFile(filepath, StandardCharsets.UTF_8);
            // Get rid of the first line which is charset definition
            fileContent = fileContent.substring(fileContent.indexOf('\n') + 1);
            jsonResponse.put("htmlPrivacyContent", fileContent);
            return RestAPIUtils.buildOKResponse(jsonResponse);
        } catch (NoSuchFileException e) {
            try {
                // Language specific help file not found, try the generic one
                final String defaultFilePath = basePath + File.separator + BASE_FILE_NAME + FILE_EXTENSION;
                String fileContent;
                fileContent = readFile(defaultFilePath, StandardCharsets.UTF_8);
                // Get rid of the first line which is charset definition
                fileContent = fileContent.substring(fileContent.indexOf('\n') + 1);
                jsonResponse.put("htmlPrivacyContent", fileContent);
                return RestAPIUtils.buildOKResponse(jsonResponse);
            } catch (IOException e1) {
                LOGGER.warn("Unexpected error while retrieving privacy policy content with rest API", e1);
                throw new InternalErrorException("Unexpected error while retrieving privacy policy content");
            }
        } catch (IOException e) {
            LOGGER.warn("Unexpected error while retrieving privacy policy content with rest API", e);
            throw new InternalErrorException("Unexpected error while retrieving privacy policy content");
        }
    }

    /**
     * Reads the file from the fileSystem and output its content as a String.
     *
     * @param path     Path of the file to be read
     * @param encoding Encoding of the file
     * @return The content of the file as a String
     * @throws IOException
     */
    private static String readFile(final String path, final Charset encoding) throws IOException {
        final byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}