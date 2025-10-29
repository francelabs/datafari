package com.francelabs.datafari.ai.agentic.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CustomAgentService {

    @UserMessage("""
        You are the backend agent of the search engine Datafari.
        Use the provided tools whenever it is relevant to answer the user query.
        Provide a clear and short answer in {{lang}}.
        Question: {{question}}
        """)
    @Agent(outputName = "answer", description = "Answer questions using the available tools.")
    TokenStream stream(@MemoryId String memoryId, @V("question") String question, @V("lang") String lang);
}