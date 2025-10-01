package com.francelabs.datafari.ai.agentic.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

// TODO : DELETE
public interface RagStreamAgentService {

    @UserMessage("""
        You are the backend agent of the search engine Datafari.
        Use the provided tools (RAG, RAG by document, summarize, search) whenever it is relevant.
        Provide a clear and short answer.
        Question: {{question}}
        """)
    @Agent(outputName = "answer", description = "Answer questions using Datafari's search and RAG tools.")
    TokenStream stream(@MemoryId String memoryId, @V("question") String question);
}