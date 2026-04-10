package com.francelabs.datafari.ai.agentic.agents.generic;

import com.francelabs.datafari.ai.agentic.agents.interfaces.IAgentService;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface GenericAgentService extends IAgentService {
    @Override
    @UserMessage(fromResource = "prompts/agentic/agents/generic.txt")
    @Agent(outputKey = "response", description = "Answer questions using Datafari's search and RAG tools.")
    String ask(@MemoryId String memoryId, @V("query") String query, @V("lang") String lang, @V("history") String history);
}

