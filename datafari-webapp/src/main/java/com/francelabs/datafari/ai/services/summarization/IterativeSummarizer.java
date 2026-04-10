package com.francelabs.datafari.ai.services.summarization;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface IterativeSummarizer {

    @UserMessage(fromResource = "prompts/summarization/template-summarization-iterative.txt")
    String refineSynthesis(@V("summary") String summary, @V("snippets") String snippets, @V("language") String language);
}

/*
## Instructions
Here is the summary of a partial document. Use the new chunks below to refine the summary if relevant. If not relevant, return the original summary.

## Summary
{{summary}}

## Additional content
{{snippets}}
*/