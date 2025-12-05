package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.DynamicToolsLoader;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.CustomTool;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.AgentStreamer;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.StreamToolExecutor;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import org.apache.commons.lang.RandomStringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CustomAgent implements IAgent {

    private final CustomAgentService agent;
    private final ChatStream stream;
    private final SourcesAccumulator sourcesAcc;
    private final HttpServletRequest request;
    private final AiRequest params;

  /**
   * The CustomAgent retrieves its tools from the "agentic-custom-tool.json" file.
   * @param request: The original HttpServletRequest
   * @param params: The AiRequest containing the request params (ID, q, lang...)
   * @param stream: The ChatStream object, to stream events
   * @param sourcesAcc: The SourcesAccumulator, to list & stream retrieve sources
   */
    public CustomAgent(HttpServletRequest request, AiRequest params, ChatStream stream, SourcesAccumulator sourcesAcc) {
        RagConfiguration config = RagConfiguration.getInstance();
        this.stream = stream;
        this.sourcesAcc = sourcesAcc;
        this.params = params;
        this.request = request;
        try {
            // Models
            StreamingChatModel streamingChatModel = RagAPI.getStreamingChatModel(config);

            // Tools
            Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

            // Dynamic tool loading from "agentic-custom-tools.json"
            Path toolJson = Paths.get(System.getProperty("catalina.base"), "conf", "agentic-custom-tools.json");
            if (Files.exists(toolJson)) {
              for (DynamicToolsLoader.DynamicTool dyn : DynamicToolsLoader.load(toolJson, request, stream, sourcesAcc)) {
                tools.put(dyn.spec(), dyn.exec());
              }
            }


            // Memory
            ChatMemoryProvider memory = id -> MessageWindowChatMemory.withMaxMessages(20);

            this.agent = AiServices.builder(CustomAgentService.class)
                    .streamingChatModel(streamingChatModel)
                    .chatMemoryProvider(memory)
                    .tools(tools)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String ask(String question) {
        String memoryId = AiService.getMemoryId(stream, params);
        AgentStreamer streamer = new AgentStreamer();
        TokenStream ts = agent.stream(memoryId, question, PromptUtils.getUserLanguage(request));
        return streamer.stream(ts, stream::event);
    }
}
