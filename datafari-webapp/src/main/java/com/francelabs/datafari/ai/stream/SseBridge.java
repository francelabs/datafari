package com.francelabs.datafari.ai.stream;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SseBridge {
    private final AsyncContext async;
    private final PrintWriter out;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SseBridge(AsyncContext async) throws IOException {
        this.async = async;
        this.out = ((HttpServletResponse) async.getResponse()).getWriter();
    }

    public void send(String event, String data) {
        if (closed.get()) return;
        try {
            out.write("event: " + event + "\n");
            // Write all data lines:
            String[] lines = data == null ? new String[]{""} : data.split("\n", -1);
            for (String line : lines) out.write("data: " + line + "\n");
            out.write("\n");
            out.flush();
            ((HttpServletResponse) async.getResponse()).flushBuffer();
        } catch (IOException e) {
            close();
        }
    }

    public void done() { send("done", ""); close(); }
    public void error(String message) { send("error", message == null ? "" : message); close(); }

    private void close() {
        if (closed.compareAndSet(false, true)) {
            try { out.flush(); } catch (Exception ignored) {}
            async.complete();
        }
    }
}
