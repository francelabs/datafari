package com.francelabs.datafari.ai.services.rag;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface IterativeRefineRag {

    @UserMessage(fromResource = "prompts/rag/iterative/template-refine-refining.txt")
    String refine(@V("userquery") String userquery, @V("lastresponse") String lastresponse, @V("snippets") String snippets,
               @V("history") String history, @V("language") String language);
}

/*
The original query is as follows: {{userquery}}
We have provided a previous answer to the query:
######
{{lastresponse}}
######

We have the opportunity to refine the existing answer (only if needed) with some more context below.
######
{{snippets}}
######

Using only context and the previous response and not prior knowledge, answer the user query.
If the context and the previous response do not contain an answer, say that you can’t find the answer.

{{history}}

Always answer in {{language}}.

Query: {{userquery}}
Answer:

*/