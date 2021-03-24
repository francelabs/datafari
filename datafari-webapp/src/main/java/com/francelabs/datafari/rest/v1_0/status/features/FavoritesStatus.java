package com.francelabs.datafari.rest.v1_0.status.features;

import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.rest.v1_0.exceptions.NotAuthenticatedException;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FavoritesStatus {
    
    @GetMapping(value = "/rest/v1.0/status/features/favorites", produces = "application/json;charset=UTF-8")
    public String getFavorites(final HttpServletRequest request) {
        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName != null) {
            final JSONObject jsonResponse = new JSONObject();
            final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
            String status = config.getProperty(DatafariMainConfiguration.LIKESANDFAVORTES);
            if (!status.contentEquals("true")) {
                status = "false";
            }
            jsonResponse.put("activated", status);
            return RestAPIUtils.buildOKResponse(jsonResponse);
        } else {
            throw new NotAuthenticatedException();
        }
    }
}
