package com.francelabs.datafari.mcp.tools;

import com.francelabs.datafari.mcp.dto.SearchRequest;
import com.francelabs.datafari.mcp.dto.SearchResponse;
import com.francelabs.datafari.mcp.service.DatafariClient;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SearchTool {

    private static final Logger LOGGER = LogManager.getLogger(SearchTool.class.getName());

    private final DatafariClient searchClient;

    public SearchTool(DatafariClient searchClient) {
        this.searchClient = searchClient;
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

        String cookieHeader = null;
        HttpServletRequest httpRequest =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
//        String cookieHeader = httpRequest.getHeader("Cookie");
        String authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String jsessionId = authorization.substring("Bearer ".length()).trim();
            cookieHeader = "JSESSIONID=" + jsessionId;
        }


        LOGGER.debug("Cookie ! {}", cookieHeader);

        return searchClient.search(request, cookieHeader);
    }
}