package com.francelabs.datafari.mcp.tools;

import com.francelabs.datafari.mcp.dto.SummarizeRequest;
import com.francelabs.datafari.mcp.dto.SummarizeResponse;
import com.francelabs.datafari.mcp.service.DatafariClient;
//import org.springframework.ai.mcp.server.annotation.McpTool;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SummarizeTool {

    private final DatafariClient client;

    public SummarizeTool(DatafariClient client) {
        this.client = client;
    }

    @McpTool(name = "datafari_summarize", description = "Summarize a document from Datafari")
    public SummarizeResponse summarize(SummarizeRequest request) {

        HttpServletRequest httpRequest =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
//        String cookieHeader = httpRequest.getHeader("Cookie");
        String authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String jsessionId = authorization.substring("Bearer ".length()).trim();
        String cookieHeader = "JSESSIONID=" + jsessionId;

        return client.summarize(request, cookieHeader);
    }
}