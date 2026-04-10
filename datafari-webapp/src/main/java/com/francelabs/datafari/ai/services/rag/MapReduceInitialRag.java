package com.francelabs.datafari.ai.services.rag;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface MapReduceInitialRag {

    @UserMessage(fromResource = "prompts/rag/iterative/template-refine-initial.txt")
    String rag(@V("userquery") String userquery, @V("snippets") String snippets,
               @V("history") String history, @V("language") String language);
}

/*
You are an AI assistant specialized in answering questions strictly based on the provided documents and chat history (if any).
- Your response must be accurate, concise, and must not include any invented information.
- If the documents do not contain the answer, say that you can’t find the answer.

Below are the documents you must use:
######
{{snippets}}
######

{{history}}

Now, answer the following question in {{language}}, using only the information from the documents or from the chat history (if any):

query: {{userquery}}
answer:

*/