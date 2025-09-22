package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.DatafariTools;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.stream.AgentStreamer;
import com.francelabs.datafari.ai.stream.SseBridge;
import com.francelabs.datafari.ai.stream.ToolMaps;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import org.apache.commons.lang.RandomStringUtils;
import org.json.simple.JSONArray;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class DatafariAgent {

    private final DatafariAgentService agent;
    private final DatafariStreamAgentService agentStream;
    private final DatafariSingleDocAgentService agentOneDoc;
    private final SseBridge sse;
    private final SourcesAccumulator sourcesAcc;

    public DatafariAgent(HttpServletRequest request, List<Document> sources, SseBridge sse, SourcesAccumulator sourcesAcc) {
        RagConfiguration config = RagConfiguration.getInstance();
        this.sse = sse;
        this.sourcesAcc = sourcesAcc;
        try {
            ChatModel chatModel = RagAPI.getChatModel(config);
            StreamingChatModel streamingChatModel = RagAPI.getStreamingChatModel(config);
            Object datafariTools = new DatafariTools(request, sources, sse, sourcesAcc);
            Map<ToolSpecification, ToolExecutor> tools = ToolMaps.build(datafariTools, sse);

            // crée l’AI Service en mode streaming + tools
            this.agentStream = AiServices.builder(DatafariStreamAgentService.class)
                    .streamingChatModel(streamingChatModel)
                    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))
                    .tools(tools)
                    .build();

            this.agent = AgenticServices
                    .agentBuilder(DatafariAgentService.class)
                    .chatModel(chatModel)
                    .tools(new DatafariTools(request, sources, null, null))
                    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))
                    .build();

            this.agentOneDoc = AgenticServices
                    .agentBuilder(DatafariSingleDocAgentService.class)
                    .chatModel(chatModel)
                    .tools(new DatafariTools(request, sources, null, null))
                    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))
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
        return streamer.stream(ts, sse); // <-- returns final response
    }

    public String askForDocument(String question, String id) {
        return agentOneDoc.ask(question, id);
    }
}
