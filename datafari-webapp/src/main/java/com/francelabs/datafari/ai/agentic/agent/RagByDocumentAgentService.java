package com.francelabs.datafari.ai.agentic.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RagByDocumentAgentService {

    @UserMessage("""
        You are the backend agent of the search engine Datafari.
        Use the provided tools (RAG, RAG by document, summarize, search) whenever it is relevant.
        You are only allowed to use one document, with the following ID: {{id}}
        Provide a clear and short answer.
        Question: {{question}}
        """)
    @Agent(outputName = "answer", description = "Answer questions using Datafari's search and RAG tools.")
    String ask(@V("question") String question, @V("id") String id);
}