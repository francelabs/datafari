package com.francelabs.datafari.ai.agentic.agent;

import com.francelabs.datafari.ai.agentic.tools.DatafariTools;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatModel;
import org.json.simple.JSONArray;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class DatafariAgent {

    private final DatafariAgentService agent;
    private final DatafariSingleDocAgentService agentOneDoc;

    public DatafariAgent(HttpServletRequest request, List<Document> sources) {
        RagConfiguration config = RagConfiguration.getInstance();
        try {
            ChatModel chatModel = RagAPI.getChatModel(config);

            this.agent = AgenticServices
                    .agentBuilder(DatafariAgentService.class)
                    .chatModel(chatModel)
                    .tools(new DatafariTools(request, sources))
//                    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))
                    .build();
            this.agentOneDoc = AgenticServices
                    .agentBuilder(DatafariSingleDocAgentService.class)
                    .chatModel(chatModel)
                    .tools(new DatafariTools(request, sources))
//                    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))
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
