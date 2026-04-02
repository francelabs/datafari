package com.francelabs.datafari.ai.services.rag;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface MapReduceMergeAllRag {

    @UserMessage(fromResource = "prompts/rag/mapreduce/template-mergeAllRag.txt")
    String rag(@V("userquery") String userquery, @V("snippets") String snippets,
               @V("history") String history, @V("language") String language);
}

/*
You are a helpful RAG assistant. We have provided a list of responses to the user query based on different sources:
######
{{snippets}}
######

Given the context information and not prior knowledge, answer the user query
Do not provide any information that does not belong in documents or in chat history.
If the context does not provide an answer, say that you can’t find the answer.

{{history}}

Answer the user query in {{language}}.

Query: {{userquery}}
Answer:
*/