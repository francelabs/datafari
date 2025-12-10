package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.agentic.tools.regular.CfPTools;
import com.francelabs.datafari.ai.agentic.tools.regular.DatafariTools;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.AgentStreamer;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.ToolMaps;
import com.francelabs.datafari.api.RagAPI;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import org.apache.commons.lang.RandomStringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public class AskUserAgent implements IAgent {

    // TODO : Clean delete this class

    private final AskUserAgentService agent;
    private final ChatStream stream;
    private final SourcesAccumulator sourcesAcc;
    private final HttpServletRequest request;
    private final AiRequest params;

  /**
   * The CfpAgent is an experimental demo Agent, specialised in "Call for Proposals".
   * @param request: The original HttpServletRequest
   * @param stream: The ChatStream object, to stream events
   * @param sourcesAcc: The SourcesAccumulator, to list & stream retrieve sources
   */
    public AskUserAgent(HttpServletRequest request, AiRequest params, ChatStream stream, SourcesAccumulator sourcesAcc) {
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

//            Object datafariTools = new HumanInLoopTools(request, stream, sourcesAcc);
//            Map<ToolSpecification, ToolExecutor> tools = ToolMaps.build(datafariTools, stream);

            // Memory
            ChatMemoryProvider memory = id -> MessageWindowChatMemory.withMaxMessages(20);

            this.agent = AiServices.builder(AskUserAgentService.class)
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
        TokenStream ts = agent.stream(memoryId, question);
        return streamer.stream(ts, stream::event);
    }
}
