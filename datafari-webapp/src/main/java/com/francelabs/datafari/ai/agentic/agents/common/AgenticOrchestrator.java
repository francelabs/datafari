package com.francelabs.datafari.ai.agentic.agents.common;


import com.francelabs.datafari.ai.agentic.agents.interfaces.IAgentService;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;

public interface AgenticOrchestrator extends IAgentService {

    @Override
    @Agent(description = "Answer user's query using Datafari's agents.")
    String ask(@MemoryId String memoryId, @V("query") String query, @V("lang") String lang, @V("history") String history);

}
