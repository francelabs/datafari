package com.francelabs.datafari.ai.agentic.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AskUserAgentService {

  // TODO : Clean delete this class

    @UserMessage("")
    @Agent(outputName = "answer", description = "Answer questions using the available tools.")
    TokenStream stream(@MemoryId String memoryId, @V("question") String question);
}