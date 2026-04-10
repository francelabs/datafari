package com.francelabs.datafari.ai.services.synthesis;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface IterativeSynthesizer {

    @UserMessage(fromResource = "prompts/synthesis/template-synthesis-iterative.txt")
    String refineSynthesis(@V("synthesis") String synthesis, @V("snippets") String snippets, @V("language") String language);
}

/*
## Instructions
You are a helpful search engine assistant. You generated a synthesis of provided documents, based on the pre-generated summaries.
Use the new documents below to refine the synthesis if relevant. If not relevant, return the original synthesis.
The synthesis must be in {{language}}.

## Synthesis
{{synthesis}}

## Additional documents
{{snippets}}
*/