package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.DatafariTools;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.stream.AgentStreamer;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.ToolMaps;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class DatafariAgent {

    private final DatafariAgentService agent;
    private final DatafariStreamAgentService agentStream;
    private final DatafariSingleDocAgentService agentOneDoc;
    private final ChatStream stream;
    private final SourcesAccumulator sourcesAcc;

    public DatafariAgent(HttpServletRequest request, ChatStream stream, SourcesAccumulator sourcesAcc) {
        RagConfiguration config = RagConfiguration.getInstance();
        this.stream = stream;
        this.sourcesAcc = sourcesAcc;
        try {
            // Models
            ChatModel chatModel = RagAPI.getChatModel(config);
            StreamingChatModel streamingChatModel = RagAPI.getStreamingChatModel(config);

            // Tools
            Object datafariTools = new DatafariTools(request, stream, sourcesAcc);
            Map<ToolSpecification, ToolExecutor> tools = ToolMaps.build(datafariTools, stream);

            // Memory
            ChatMemoryProvider memory = id -> MessageWindowChatMemory.withMaxMessages(20);

            this.agentStream = AiServices.builder(DatafariStreamAgentService.class)
                    .streamingChatModel(streamingChatModel)
                    .chatMemoryProvider(memory)
                    .tools(tools)
                    .build();

            this.agent = AgenticServices
                    .agentBuilder(DatafariAgentService.class)
                    .chatModel(chatModel)
                    .tools(new DatafariTools(request, stream, sourcesAcc))
                    .chatMemoryProvider(memory)
                    .build();

            this.agentOneDoc = AgenticServices
                    .agentBuilder(DatafariSingleDocAgentService.class)
                    .chatModel(chatModel)
                    .tools(new DatafariTools(request, stream, sourcesAcc))
                    .chatMemoryProvider(memory)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String ask(String question) {
        return agent.ask(question);
    }

    public String stream(String question) {
        AgentStreamer streamer = new AgentStreamer();
        TokenStream ts = agentStream.stream(question);
        return streamer.stream(ts, stream::event);
    }

    public String askForDocument(String question, String id) {
        return agentOneDoc.ask(question, id);
    }
}
