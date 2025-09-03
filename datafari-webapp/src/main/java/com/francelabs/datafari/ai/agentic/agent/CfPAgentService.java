package com.francelabs.datafari.ai.agentic.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CfPAgentService {

    @UserMessage("""
        You are the backend agent of the search engine Datafari, specialised in CfP (Call for Providers).
        Use the provided tools whenever it is relevant. You can read has many pages has you need of any document.
        Each Call for Providers has multiple associated documents:
        - CCTP (Cahier des Charges Techniques Particuliers): contains detailed information about what is required for the CfP.
        - CCAP (Cahier des Clauses Administratives Particulières): contains administrative information about the CfP.
        - ANNEXE: The annexe document of the CfP.
        Provide a clear and short answer.
        Question: {{question}}
        """)
    @Agent(outputName = "answer", description = "Answer questions using the available tools.")
    String ask(@V("question") String question);
}