package com.francelabs.datafari.ai.stream;

import dev.langchain4j.model.chat.response.*;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatafariStreamingResponseHandler implements StreamingChatResponseHandler {

    private final AsyncContext async;
    private final PrintWriter out;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DatafariStreamingResponseHandler(AsyncContext async) throws IOException {
        this.async = async;
        HttpServletResponse resp = (HttpServletResponse) async.getResponse();
        this.out = resp.getWriter();
    }

    // --------- small SSE helpers ---------
    private void send(String event, String data) {
        if (closed.get()) return;
        try {
            // SSE frame
            out.write("event: " + event + "\n");
            for (String line : data.split("\n", -1)) {
                out.write("data: " + line + "\n");
            }
            out.write("\n");
            out.flush();
            ((HttpServletResponse) async.getResponse()).flushBuffer(); // push out of buffers (proxies)
        } catch (IOException e) {
            close(); // client probably disconnected
        }
    }

    private void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                // best-effort "done"
                out.write("event: done\ndata:\n\n");
                out.flush();
            } catch (Exception ignored) {}
            async.complete();
        }
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        send("token", partialResponse);
    }

    @Override
    public void onPartialThinking(PartialThinking partialThinking) {
        send("thinking", partialThinking == null ? "" : partialThinking.toString());
    }

    @Override
    public void onPartialToolCall(PartialToolCall partialToolCall) {
        send("tool_call", partialToolCall == null ? "" : partialToolCall.toString());
    }

    @Override
    public void onCompleteToolCall(CompleteToolCall completeToolCall) {
        send("tool_result", completeToolCall == null ? "" : completeToolCall.toString());
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        // Option : pousser aussi le texte complet (si utile côté client)
        // Certaines implémentations proposent completeResponse.aiMessage().text()
        String summary;
        try {
            summary = String.valueOf(completeResponse);
        } catch (Exception e) {
            summary = "";
        }
        send("complete", summary);
        close(); // termine proprement le flux SSE
    }

    @Override
    public void onError(Throwable error) {
        send("error", (error == null ? "unknown" : error.getMessage()));
        close();
    }
}
