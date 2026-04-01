package com.francelabs.datafari.ai.services.synthesis;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface InitialSynthesizer {

    @UserMessage(fromResource = "prompts/synthesis/template-synthesis-initial.txt")
    String writeSynthesis(@V("snippets") String snippets, @V("language") String language);
}

/*
## Instructions
You are a helpful search engine assistant. You are given documents summaries. Write a synthesis in {{language}}.

## Documents
{{snippets}}
*/