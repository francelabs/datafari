package com.francelabs.datafari.rest.v1_0.aggregator;

import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.aggregator.servlet.AggregatorList;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Aggregator {

    private static final Logger logger = LogManager.getLogger(Aggregator.class.getName());
    private static final String ERROR_MSG = "Error while retrieving aggregator information.";

    @GetMapping(value = "/rest/v1.0/aggregator", produces = "application/json;charset=UTF-8")
    protected String getUser(final HttpServletRequest request) {
        try {
            JSONObject responseContent = new JSONObject();
            JSONArray aggregatorList = AggregatorList.doGetList(request);
            responseContent.put("aggregatorList", aggregatorList);
            return RestAPIUtils.buildOKResponse(responseContent);
        } catch (ParseException e) {
            logger.warn(ERROR_MSG,e);
            throw new InternalErrorException();
        }
    }
    
}
