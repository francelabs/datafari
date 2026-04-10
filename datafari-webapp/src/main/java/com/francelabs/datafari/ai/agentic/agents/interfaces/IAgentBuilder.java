package com.francelabs.datafari.ai.agentic.agents.interfaces;

public interface IAgentBuilder {

    IAgentService agent = null;
    IStreamingAgentService streamingAgent = null;

    default IAgentService buildAgent() {
        return agent;
    }
    default IStreamingAgentService buildStreamAgent() {
        return streamingAgent;
    }
}
