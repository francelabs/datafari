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
package com.francelabs.datafari.rest.v1_0.fields;

import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.utils.AdvancedSearchConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Label {

    private static final Logger logger = LogManager.getLogger(Label.class.getName());
    private static final String ERROR_MSG = "Unable to parse json for fields labels for advanced search";

    @GetMapping(value = "/rest/v1.0/fields/label", produces = "application/json;charset=UTF-8")
    public String getFieldsLabels(final HttpServletRequest request) {
        final JSONObject jsonResponse = new JSONObject();

        final String jsonMappingFieldNameValues = AdvancedSearchConfiguration.getInstance()
                .getProperty(AdvancedSearchConfiguration.LABELED_FIELDS, "{}");
        final JSONParser parser = new JSONParser();
        try {
            JSONObject json = (JSONObject) parser.parse(jsonMappingFieldNameValues);
            jsonResponse.put("mappingFieldNameValues", json);
            return RestAPIUtils.buildOKResponse(jsonResponse);
        } catch (ParseException e) {
            logger.warn(ERROR_MSG, e);
            JSONObject extra = new JSONObject();
            extra.put("details", ERROR_MSG);
            return RestAPIUtils.buildErrorResponse(500, "Internal Error", extra);
        }
    }

}
