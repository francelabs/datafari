package com.francelabs.datafari.ai.services.rag;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface QueryRewriter {

    @UserMessage(fromResource = "prompts/rag/rewriteSearchQuery.txt")
    String rewriteQuery(@V("userquery") String userquery, @V("conversation") String conversation);
}

/*
Below is a history of the conversation so far, and a new question asked by the user that needs to be answered by searching in a knowledge base.
######
Conversation history:
{{conversation}}
######
New question:
- user: {{userquery}}
######

You have access to a Search Engine index with 100's of documents.
Generate a search query based on the conversation and the new question.
Do not include cited source filenames and document names e.g info.txt or doc.pdf in the search query terms.
Do not include any text inside [] or <<>> in the search query terms.
Do not include any special characters like '+'.
If the question is not in English, translate the question to English before generating the search query.
If you cannot generate a search query, return just the number 0.
*/