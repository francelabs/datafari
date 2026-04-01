package com.francelabs.datafari.ai.agentic.agents.generic;

import com.francelabs.datafari.ai.agentic.agents.interfaces.IAgentBuilder;
import com.francelabs.datafari.ai.agentic.agents.interfaces.IAgentService;
import com.francelabs.datafari.ai.agentic.agents.interfaces.IStreamingAgentService;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.ToolMaps;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.tool.ToolExecutor;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public class GenericAgentBuilder implements IAgentBuilder {

    private final IAgentService agent;
    private final IStreamingAgentService streamingAgent;

  /**
   * The RAG Agent is an agent specialised in RAG request processing.
   * The available tools are listed in GenericTools.java (Search, RAG by document, summarization...)
   * @param request: The original HttpServletRequest
   * @param params: The AiRequest containing the request params (ID, q, lang...)
   * @param stream: The ChatStream object, to stream events
   * @param sourcesAcc: The SourcesAccumulator, to list & stream retrieve sources
   */
    public GenericAgentBuilder(HttpServletRequest request, AiRequest params, ChatStream stream, SourcesAccumulator sourcesAcc) {
        RagConfiguration config = RagConfiguration.getInstance();
        try {
            // Models
            StreamingChatModel streamingChatModel = AiService.getStreamingChatModel(config);
            ChatModel chatModel = AiService.getChatModel(config);

            // Tools
            Object datafariTools = new GenericTools(request, params, stream, sourcesAcc);
            Map<ToolSpecification, ToolExecutor> tools = ToolMaps.build(datafariTools, stream);

            // Memory
            ChatMemoryProvider memory = memoryId -> MessageWindowChatMemory.withMaxMessages(20);

            this.agent = AgenticServices.agentBuilder(GenericAgentService.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memory)
                .outputKey("response")
                .tools(tools)
                .build();

            this.streamingAgent = AgenticServices.agentBuilder(GenericStreamingAgentService.class)
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
