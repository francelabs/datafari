package com.francelabs.datafari.ai.stream;


import java.util.Map;

/**
 * This ChatStream object can be used for non-streaming requests.
 * All the events sent by the AiServices are ignored.
 */
public class NoopChatStream implements ChatStream {

    @Override
    public void event(String type, Map<String, ?> data) {
        // ignore all stream events
    }
}