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

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.utils.AdvancedSearchConfiguration;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Exact {

    @GetMapping(value = "/rest/v1.0/fields/exact", produces = "application/json;charset=UTF-8")
    public String getExactFields(final HttpServletRequest request) {
        final JSONObject jsonResponse = new JSONObject();
        final String strExactFields = AdvancedSearchConfiguration.getInstance()
                .getProperty(AdvancedSearchConfiguration.EXACT_FIELDS, "");
        if (strExactFields.trim().length() > 0) {
            final List<String> exactFieldsList = Arrays.asList(strExactFields.split(","));
            jsonResponse.put("exactFieldsList", exactFieldsList);
        } else {
            jsonResponse.put("exactFieldsList", new JSONArray());
        }
        return RestAPIUtils.buildOKResponse(jsonResponse);

    }

}
