package com.francelabs.datafari.ai.services.summarization;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface InitialSummarizer {

    @UserMessage(fromResource = "prompts/summarization/template-summarization-initial.txt")
    String writeSummary(@V("snippets") String snippets, @V("language") String language);
}

/*
## Instructions
You are a helpful search engine assistant. You are given a document or document extract. Summarize its content in {{language}}.

## Content
{{snippets}}
*/