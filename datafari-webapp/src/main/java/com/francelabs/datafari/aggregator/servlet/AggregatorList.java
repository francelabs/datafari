package com.francelabs.datafari.aggregator.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.ldap.LdapUsers;
import com.francelabs.datafari.utils.SearchAggregatorConfiguration;
import com.francelabs.datafari.utils.SearchAggregatorUserConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;

@WebServlet("/aggregatorList")
public class AggregatorList extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(AggregatorList.class.getName());

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final SearchAggregatorConfiguration sac = SearchAggregatorConfiguration.getInstance();
        final String jaExternalDatafarisStr = sac.getProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS);
        final boolean activated = Boolean.valueOf(sac.getProperty(SearchAggregatorConfiguration.ACTIVATED));
        final String defaultDatafari = sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI) == null ? ""
                : sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI);

        // Retrieve username
        String authenticatedUserName = "";
        if (request.getUserPrincipal() != null) {
            // Get the username
            if (request.getUserPrincipal() instanceof KeycloakAuthenticationToken) {
                final KeycloakAuthenticationToken keycloakToken = (KeycloakAuthenticationToken) request
                        .getUserPrincipal();
                if (keycloakToken.getDetails() instanceof SimpleKeycloakAccount) {
                    final SimpleKeycloakAccount keycloakAccount = (SimpleKeycloakAccount) keycloakToken.getDetails();
                    authenticatedUserName = keycloakAccount.getKeycloakSecurityContext().getToken()
                            .getPreferredUsername();
                    // TODO: Retrieve user Home Datafari if set
                } else {
                    authenticatedUserName = request.getUserPrincipal().getName().replaceAll("[^\\\\]*\\\\", "");
                }
            } else {
                authenticatedUserName = request.getUserPrincipal().getName().replaceAll("[^\\\\]*\\\\", "");
            }
            if (!authenticatedUserName.contains("@")) {
                final String domain = LdapUsers.getInstance().getUserDomain(authenticatedUserName);
                if (domain != null && !domain.isEmpty()) {
                    authenticatedUserName += "@" + domain;
                }
            }
        }

        String userHomeDatafari = null;
        // Retrieve user home Datafari
        SearchAggregatorUserConfig userConfig = SearchAggregatorUserConfig.getInstance();
        userHomeDatafari = userConfig.getDefaultSourceFor(authenticatedUserName);

        // Revert back to the default Datafari defined by admin if no user defined home
        // exists
        if (userHomeDatafari == null || userHomeDatafari.trim().length() == 0) {
            userHomeDatafari = defaultDatafari;
        }

        // Retrieve user allowed Datafari
        ArrayList<String> allowedDatafari = userConfig.getAllowedSourcesFor(authenticatedUserName);

        try {
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
                        if (label.equals(userHomeDatafari)) {
                            externalResult.put("selected", true);
                        }
                        result.add(externalResult);
                    }
                }
            }
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
}