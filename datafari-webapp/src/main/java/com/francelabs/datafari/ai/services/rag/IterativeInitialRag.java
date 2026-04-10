package com.francelabs.datafari.ai.services.rag;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface IterativeInitialRag {

    @UserMessage(fromResource = "prompts/rag/iterative/template-refine-initial.txt")
    String rag(@V("userquery") String userquery, @V("snippets") String snippets,
               @V("history") String history, @V("language") String language);
}

/*
Context information is below.
######
{{snippets}}
######

Given the context information and not prior knowledge, answer the query.
Your response must be accurate, concise, and must not include any invented information.
If the documents do not contain an answer, say that you can’t find the answer.

{{history}}

Now, answer the user’s question in {{language}} using only this information.

Query: {{userquery}}
Answer:
*/