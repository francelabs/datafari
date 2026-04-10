package com.francelabs.datafari.ai.agentic.agents.common;


import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResponseScorer {

    @UserMessage("""
            You are a reviewer assistant.
            Give a review score between 0.0 and 1.0 for the following
            generated response based on how well it aligns with the style 'professional',
            its language that must be {{lang}}, and its structure.
            The evaluated response must answer the user query as well as possible.
            The evaluated response must not be truncated, and must not contain any prefix such as 'ASSISTANT:'
            Return only the score and nothing else.
            
            The original query is: "{{query}}"
            
            The response to review is: "{{response}}"
            """)
    @Agent(description = "Scores a response based on the style, the language and its content",  outputKey = "score")
    double scoreResponse(@V("response") String response, @V("query") String query, @V("lang") String lang);

}
