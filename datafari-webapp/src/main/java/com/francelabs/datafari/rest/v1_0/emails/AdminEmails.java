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
package com.francelabs.datafari.rest.v1_0.emails;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.service.db.SavedSearchDataServicePostgres;
import com.francelabs.datafari.utils.DatafariMainConfiguration;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminEmails {

    final static Logger logger = LogManager.getLogger(SavedSearchDataServicePostgres.class.getName());

    @GetMapping(value = "/rest/v1.0/emails/admin", produces = "application/json;charset=UTF-8")
    protected String getCurrentUserSavedSearches(final HttpServletRequest request) {
        final JSONObject jsonResponse = new JSONObject();
        try {
            DatafariMainConfiguration dmc = DatafariMainConfiguration.getInstance();
            String bugsMail = dmc.getProperty(DatafariMainConfiguration.EMAIL_BUGS);
            String dpoMail = dmc.getProperty(DatafariMainConfiguration.EMAIL_DPO);
            String feedbacksMail = dmc.getProperty(DatafariMainConfiguration.EMAIL_FEEDBACKS);
            JSONObject emailObj = new JSONObject();
            emailObj.put("bugs", bugsMail);
            emailObj.put("dpo", dpoMail);
            emailObj.put("feedbacks", feedbacksMail);
            jsonResponse.put("emails", emailObj);
            return RestAPIUtils.buildOKResponse(jsonResponse);
        } catch (Exception e) {
            logger.debug("REST API encountered an exception while retrieving the admins e-mail addresses", e);
            throw new InternalErrorException("Unexpected error while retrieving admins e-mails");
        }
        
    }

}