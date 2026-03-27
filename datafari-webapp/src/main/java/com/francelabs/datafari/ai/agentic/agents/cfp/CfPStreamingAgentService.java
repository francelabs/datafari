package com.francelabs.datafari.ai.agentic.agents.cfp;

import com.francelabs.datafari.ai.agentic.agents.interfaces.IStreamingAgentService;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CfPStreamingAgentService extends IStreamingAgentService {

    @Override
    @UserMessage(fromResource = "prompts/agentic/agents/cfp.txt")
    @Agent(outputKey = "response", description = "Answer questions using the available tools.")
    TokenStream stream(@MemoryId String memoryId, @V("query") String query, @V("lang") String lang, @V("history") String history);
}