package com.francelabs.datafari.ai.agentic.agents.interfaces;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;

public interface IAgentService {

    String ask(@MemoryId String memoryId, @V("query") String query, @V("lang") String lang, @V("history") String history);
}