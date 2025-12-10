package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.regular.DatafariTools;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.AgentStreamer;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.ToolMaps;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.tool.ToolExecutor;
import org.apache.commons.lang.RandomStringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Map;

public class RagAgent implements IAgent {

    private final RagAgentService agent;
    private final ChatStream stream;
    private final SourcesAccumulator sourcesAcc;
    private final HttpServletRequest request;
    private final AiRequest params;


  /**
   * The RAG Agent is an agent specialised in RAG request processing.
   * The available tools are listed in RagTools.java (Search, RAG by document, summarization...)
   * @param request: The original HttpServletRequest
   * @param params: The AiRequest containing the request params (ID, q, lang...)
   * @param stream: The ChatStream object, to stream events
   * @param sourcesAcc: The SourcesAccumulator, to list & stream retrieve sources
   */
    public RagAgent(HttpServletRequest request, AiRequest params, ChatStream stream, SourcesAccumulator sourcesAcc) {
        RagConfiguration config = RagConfiguration.getInstance();
        this.stream = stream;
        this.params = params;
        this.sourcesAcc = sourcesAcc;
        this.request = request;
        try {
            // Models
            StreamingChatModel streamingChatModel = RagAPI.getStreamingChatModel(config);

            // Tools
            Object datafariTools = new DatafariTools(request, params, stream, sourcesAcc);
            Map<ToolSpecification, ToolExecutor> tools = ToolMaps.build(datafariTools, stream);

            // Memory
            ChatMemoryProvider memory = memoryId -> MessageWindowChatMemory.withMaxMessages(20);

            this.agent = AiServices.builder(RagAgentService.class)
                    .streamingChatModel(streamingChatModel)
                    .chatMemoryProvider(memory)
                    .tools(tools)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String ask(String question) {
        String memoryId = AiService.getMemoryId(stream, params);
        AgentStreamer streamer = new AgentStreamer();
        String history = readChatHistory();
        TokenStream ts = agent.stream(memoryId, question, PromptUtils.getUserLanguage(request), history);
        return streamer.stream(ts, stream::event);
    }


  /**
   *
   * @return a String containing the chat history (if any), with assistant and user messages
   */
  String readChatHistory() {

      RagConfiguration config = RagConfiguration.getInstance();
      if (params.history == null || !config.getBooleanProperty(RagConfiguration.CHAT_MEMORY_ENABLED)) {
          return "";
      }

      final int size = params.history.size();
      final int maxSize = config.getIntegerProperty(RagConfiguration.CHAT_MEMORY_HISTORY_SIZE);
      final int from = Math.max(0, size - maxSize);
      StringBuilder sb = new StringBuilder();

      for (int i = from; i < size; i++) {

          AiRequest.ChatMessage message = params.history.get(i);
          String role = (message != null && message.role != null) ? message.role.name().toUpperCase() : "USER";
          String text = (message != null && message.message != null) ? message.message : "";

          // Avoid multiline in compact history
          text = text.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ');
          text = text.replace('\n', ' ').trim();

          sb.append(role).append(": ").append(text).append("\n");
      }

      return sb.toString();
    }

}
