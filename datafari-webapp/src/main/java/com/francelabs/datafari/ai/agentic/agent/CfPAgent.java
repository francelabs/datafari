package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.CfPTools;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.apache.commons.lang.RandomStringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class CfPAgent {

    private final CfPAgentService agent;
    private final ChatStream stream;
    private final SourcesAccumulator sourcesAcc;

    public CfPAgent(HttpServletRequest request, ChatStream stream, SourcesAccumulator sourcesAcc) {
        RagConfiguration config = RagConfiguration.getInstance();
        this.stream = stream;
        this.sourcesAcc = sourcesAcc;
        try {
            ChatModel chatModel = RagAPI.getChatModel(config);
            StreamingChatModel streamingChatModel = RagAPI.getStreamingChatModel(config);

            this.agent = AgenticServices
                    .agentBuilder(CfPAgentService.class)
                    .chatModel(chatModel)
                    .tools(new CfPTools(request, stream, sourcesAcc))
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String ask(String question) {
        String memoryId = RandomStringUtils.randomAlphanumeric(20).toUpperCase();
        return agent.ask(memoryId, question);
    }
}
