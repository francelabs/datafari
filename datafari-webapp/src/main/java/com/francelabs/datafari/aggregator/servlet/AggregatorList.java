package com.francelabs.datafari.aggregator.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.SearchAggregatorConfiguration;
import com.francelabs.datafari.utils.SearchAggregatorUserConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("/aggregatorList")
public class AggregatorList extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(AggregatorList.class.getName());

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        try {
            final JSONArray result = doGetList(request);
            String finalResponseStr = result.toJSONString();
            final String wrapperFunction = request.getParameter("json.wrf");
            if (wrapperFunction != null) {
                finalResponseStr = wrapperFunction + "(" + finalResponseStr + ")";
            }
            response.setStatus(200);
            response.setCharacterEncoding("utf-8");
            response.setContentType("text/json;charset=utf-8");
            response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
            response.getWriter().write(finalResponseStr);

        } catch (final Exception e) {
            LOGGER.error("aggregatorList unexpected error", e);
            response.setStatus(500);
        }

    }

    public static JSONArray doGetList(final HttpServletRequest request) throws ParseException {
        final SearchAggregatorConfiguration sac = SearchAggregatorConfiguration.getInstance();
        final String jaExternalDatafarisStr = sac.getProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS);
        final boolean activated = Boolean.valueOf(sac.getProperty(SearchAggregatorConfiguration.ACTIVATED));
        final String defaultDatafariString = sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI) == null ? null 
            : sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI);
        final boolean alwaysUseDefault = Boolean.valueOf(sac.getProperty(SearchAggregatorConfiguration.ALWAYS_USE_DEFAULT));

        String[] defaultDatafarisArray = {};
        if (defaultDatafariString != null && defaultDatafariString.trim().length() > 0) {
            defaultDatafarisArray = defaultDatafariString.split(SearchAggregatorConfiguration.SITES_SEPARATOR);
        }
        ArrayList<String> defaultDatafaris = new ArrayList<>(Arrays.asList(defaultDatafarisArray));

        // Retrieve username
        String authenticatedUserName = AuthenticatedUserName.getName(request);
        if (authenticatedUserName == null) {
            authenticatedUserName = "";
        }

        ArrayList<String> userHomeDatafari = null;
        // Retrieve user home Datafari
        SearchAggregatorUserConfig userConfig = SearchAggregatorUserConfig.getInstance();
        userHomeDatafari = userConfig.getDefaultSourceFor(authenticatedUserName);

        if (userHomeDatafari.size() == 0 || alwaysUseDefault) {
            userHomeDatafari.addAll(defaultDatafaris);
        }


        // Retrieve user allowed Datafari
        ArrayList<String> allowedDatafari = userConfig.getAllowedSourcesFor(authenticatedUserName);
        final JSONArray result = new JSONArray();
        if (activated) {
            final JSONParser parser = new JSONParser();
            final JSONArray jaExternalDatafaris = (JSONArray) parser.parse(jaExternalDatafarisStr);
            for (Object json : jaExternalDatafaris) {
                JSONObject externalDatafari = (JSONObject) json;
                Boolean enabled = (Boolean) externalDatafari.get("enabled");
                String label = (String) externalDatafari.get("label");
                // Add an external Datafari only if it is enabled and part of the allowed list for
                // the current user. If no list is available for the current user, we assume he
                // can search on all datafaris (ACLs are always taken into account anyway).
                if (enabled && (allowedDatafari == null || allowedDatafari.contains(label))) {
                    JSONObject externalResult = new JSONObject();
                    externalResult.put("label", label);
                    if (userHomeDatafari.contains(label)) {
                        externalResult.put("selected", true);
                    }
                    result.add(externalResult);
                }
            }
        }
        return result;
    }
}