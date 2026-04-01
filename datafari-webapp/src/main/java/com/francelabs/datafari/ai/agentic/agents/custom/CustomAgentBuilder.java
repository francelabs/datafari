package com.francelabs.datafari.ai.agentic.agents.custom;

import com.francelabs.datafari.ai.agentic.agents.interfaces.IAgentBuilder;
import com.francelabs.datafari.ai.agentic.agents.interfaces.IAgentService;
import com.francelabs.datafari.ai.agentic.agents.interfaces.IStreamingAgentService;
import com.francelabs.datafari.ai.agentic.tools.DynamicToolsLoader;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.ChatStream;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.tool.ToolExecutor;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

// TODO : WORK IN PROGRESS
public class CustomAgentBuilder implements IAgentBuilder {

  private final IAgentService agent;
  private final IStreamingAgentService streamingAgent;

  /**
   * The CustomAgentBuilder retrieves its tools from the "agentic-custom-tool.json" file.
   *
   * @param request    : The original HttpServletRequest
   * @param stream     : The ChatStream object, to stream events
   * @param sourcesAcc : The SourcesAccumulator, to list & stream retrieve sources
   */
    public CustomAgentBuilder(HttpServletRequest request, ChatStream stream, SourcesAccumulator sourcesAcc) {
        RagConfiguration config = RagConfiguration.getInstance();
        try {
            // Models
            StreamingChatModel streamingChatModel = AiService.getStreamingChatModel(config);
            ChatModel chatModel = AiService.getChatModel(config);

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

            this.agent = AgenticServices.agentBuilder(CustomAgentService.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memory)
                .outputKey("response")
                .tools(tools)
                .build();

            this.streamingAgent = AgenticServices.agentBuilder(CustomStreamingAgentService.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memory)
                .outputKey("response")
                .tools(tools)
                .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public IAgentService buildAgent() {
        return this.agent;
    }

    public IStreamingAgentService buildStreamAgent() {
        return this.streamingAgent;
    }
}
