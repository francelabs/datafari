package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.DatafariTools;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

import javax.servlet.http.HttpServletRequest;

public class DatafariAgent {

    private final DatafariAgentService agent;
    private final DatafariSingleDocAgentService agentOneDoc;

    public DatafariAgent(HttpServletRequest request) {
        RagConfiguration config = RagConfiguration.getInstance();
        try {
            ChatModel chatModel = RagAPI.getChatModel(config);

            this.agent = AgenticServices
                    .agentBuilder(DatafariAgentService.class)
                    .chatModel(chatModel)
                    .tools(new DatafariTools(request))
//                    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();
            this.agentOneDoc = AgenticServices
                    .agentBuilder(DatafariSingleDocAgentService.class)
                    .chatModel(chatModel)
                    .tools(new DatafariTools(request))
//                    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String ask(String question) {
        return agent.ask(question);
    }

    public String askForDocument(String question, String id) {
        return agentOneDoc.ask(question, id);
    }
}
