package com.francelabs.datafari.mcp.tools;

import com.francelabs.datafari.mcp.dto.SearchRequest;
import com.francelabs.datafari.mcp.dto.SearchResponse;
import com.francelabs.datafari.mcp.service.DatafariClient;
import com.francelabs.datafari.mcp.utils.AuthenticationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "mcp.tool.enable",
        name = "search",
        havingValue = "true",
        matchIfMissing = true // Enabled by default
)
public class SearchTool {

    private static final Logger LOGGER = LogManager.getLogger(SearchTool.class.getName());

    private final DatafariClient searchClient;
    private final AuthenticationUtils authenticationUtils;

    public SearchTool(DatafariClient client, AuthenticationUtils authenticationUtils) {
        this.searchClient = client;
        this.authenticationUtils = authenticationUtils;
    }

    @McpTool(name = "datafari_search", description = "Search documents in Datafari")
    public SearchResponse search(
            @McpToolParam(description = "Search query", required = true)
            String query,

            @McpToolParam(description = "Maximum number of results to return", required = false)
            Integer rows
    ) {

        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        if (rows != null) request.setRows(rows);

        HttpHeaders authenticationHeaders = authenticationUtils.getForwardedAuthenticationHeaders();

        return searchClient.search(request, authenticationHeaders);
    }
}