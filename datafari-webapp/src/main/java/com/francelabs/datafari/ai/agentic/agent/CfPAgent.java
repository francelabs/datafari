package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.CfPTools;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.apache.commons.lang.RandomStringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class CfPAgent {

    private final CfPAgentService agent;

    public CfPAgent(HttpServletRequest request, List<Document> sources) {
        RagConfiguration config = RagConfiguration.getInstance();
        try {
            ChatModel chatModel = RagAPI.getChatModel(config);

            this.agent = AgenticServices
                    .agentBuilder(CfPAgentService.class)
                    .chatModel(chatModel)
                    .tools(new CfPTools(request, sources))
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
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
