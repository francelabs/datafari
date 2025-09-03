package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.CustomTools;
import com.francelabs.datafari.ai.agentic.tools.DatafariTools;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

import javax.servlet.http.HttpServletRequest;

public class CfPAgent {

    private final CfPAgentService agent;

    public CfPAgent(HttpServletRequest request) {
        RagConfiguration config = RagConfiguration.getInstance();
        try {
            ChatModel chatModel = RagAPI.getChatModel(config);

            this.agent = AgenticServices
                    .agentBuilder(CfPAgentService.class)
                    .chatModel(chatModel)
                    .tools(new CustomTools(request))
//                    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String ask(String question) {
        return agent.ask(question);
    }
}
