package com.francelabs.datafari.mcp.tools;

import com.francelabs.datafari.mcp.dto.AgenticRequest;
import com.francelabs.datafari.mcp.dto.AgenticResponse;
import com.francelabs.datafari.mcp.service.DatafariClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
public class AgenticTool {

    private final DatafariClient client;

    public AgenticTool(DatafariClient client) {
        this.client = client;
    }

    @McpTool(name = "datafari_agentic", description = "Ask a question to Datafari Agent")
    public AgenticResponse agentic(
            @McpToolParam(description = "Question", required = true)
            String query,

            @McpToolParam(description = "Language code, for example en, fr, es or de", required = false)
            String lang,

            @McpToolParam(description = "Agent to call, default: generic", required = false)
            String agent
    ) {

        AgenticRequest request = new AgenticRequest();
        request.setQuery(query);
        if (agent != null) request.setAgent(agent);
        if (lang != null) request.setLang(lang);


        String cookieHeader = null;
        HttpServletRequest httpRequest =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
//        String cookieHeader = httpRequest.getHeader("Cookie");
        String authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String jsessionId = authorization.substring("Bearer ".length()).trim();
            cookieHeader = "JSESSIONID=" + jsessionId;
        }

        return client.agentic(request, cookieHeader);
    }
}