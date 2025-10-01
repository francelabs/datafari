package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.DatafariTools;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.stream.AgentStreamer;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.ToolMaps;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.ai.config.RagConfiguration;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import org.apache.commons.lang.RandomStringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CfPAgent implements IAgent {

    private final CfPAgentService agent;
    private final ChatStream stream;
    private final SourcesAccumulator sourcesAcc;

    public CfPAgent(HttpServletRequest request, ChatStream stream, SourcesAccumulator sourcesAcc) {
        RagConfiguration config = RagConfiguration.getInstance();
        this.stream = stream;
        this.sourcesAcc = sourcesAcc;
        try {
            // Models
            StreamingChatModel streamingChatModel = RagAPI.getStreamingChatModel(config);

            // Tools
            Object datafariTools = new DatafariTools(request, stream, sourcesAcc);
            Map<ToolSpecification, ToolExecutor> tools = ToolMaps.build(datafariTools, stream);

            // Memory
            ChatMemoryProvider memory = id -> MessageWindowChatMemory.withMaxMessages(20);

            this.agent = AiServices.builder(CfPAgentService.class)
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
        String memoryId = RandomStringUtils.randomAlphanumeric(20).toUpperCase();
        AgentStreamer streamer = new AgentStreamer();
        TokenStream ts = agent.stream(memoryId, question);
        return streamer.stream(ts, stream::event);
    }
}
