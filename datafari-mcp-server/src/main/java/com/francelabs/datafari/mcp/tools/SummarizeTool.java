package com.francelabs.datafari.mcp.tools;

import com.francelabs.datafari.mcp.dto.SummarizeRequest;
import com.francelabs.datafari.mcp.dto.SummarizeResponse;
import com.francelabs.datafari.mcp.service.DatafariClient;
//import org.springframework.ai.mcp.server.annotation.McpTool;
import com.francelabs.datafari.mcp.utils.AuthenticationUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@ConditionalOnProperty(
        prefix = "mcp.tool.enable",
        name = "summarize",
        havingValue = "true",
        matchIfMissing = false // Disabled by default
)
public class SummarizeTool {

    private final DatafariClient client;
    private final AuthenticationUtils authenticationUtils;

    public SummarizeTool(DatafariClient client,
                         AuthenticationUtils authenticationUtils) {
        this.client = client;
        this.authenticationUtils = authenticationUtils;
    }

    @McpTool(name = "datafari_summarize", description = "Summarize a document from Datafari")
    public SummarizeResponse summarize(
            @McpToolParam(description = "Document ID", required = true)
            String docId,

            @McpToolParam(description = "Language code, for example en, fr, es or de", required = false)
            String lang
    ) {
        SummarizeRequest request = new SummarizeRequest();
        request.setDocId(docId);
        if (lang != null) request.setLang(lang);

        HttpHeaders authenticationHeaders = authenticationUtils.getForwardedAuthenticationHeaders();

        return client.summarize(request, authenticationHeaders);
    }
}