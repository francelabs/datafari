package com.francelabs.datafari.mcp.tools;

import com.francelabs.datafari.mcp.dto.AgenticRequest;
import com.francelabs.datafari.mcp.dto.AgenticResponse;
import com.francelabs.datafari.mcp.service.DatafariClient;
import com.francelabs.datafari.mcp.utils.AuthenticationUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
@ConditionalOnProperty(
        prefix = "mcp.tool.enable",
        name = "search",
        havingValue = "true",
        matchIfMissing = true
)
public class AgenticTool {

    private final DatafariClient client;
    private final AuthenticationUtils authenticationUtils;

    public AgenticTool(DatafariClient client,
                       AuthenticationUtils authenticationUtils) {
        this.client = client;
        this.authenticationUtils = authenticationUtils;
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

        HttpHeaders authenticationHeaders = authenticationUtils.getForwardedAuthenticationHeaders();
        return client.agentic(request, authenticationHeaders);
    }
}