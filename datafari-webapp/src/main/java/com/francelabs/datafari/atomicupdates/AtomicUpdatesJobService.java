package com.francelabs.datafari.atomicupdates;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;


public class AtomicUpdatesJobService {

    public static final String DEFAULT_LAUNCHER = "/opt/datafari/bin/atomicupdates/atomic-updates-launcher.sh";
    public static final String DEFAULT_DATE = "full";

    private final String launcherPath;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    /** Once job at a time. */
    private volatile JobState current;

    public AtomicUpdatesJobService() {
        this(DEFAULT_LAUNCHER);
    }

    public AtomicUpdatesJobService(String launcherPath) {
        this.launcherPath = (launcherPath == null || launcherPath.isBlank()) ? DEFAULT_LAUNCHER : launcherPath;
    }

    // ---------------------
    // Public API
    // ---------------------

    /** True if a job is running. */
    public synchronized boolean isRunning() {
        return current != null && !current.finished;
    }

    /** Returns current job state (may be null). */
    public synchronized JobState getCurrent() {
        return current;
    }

    /** Start an atomic update job with default date ("full"). */
    public synchronized JobState startJob(String job) throws IOException {
        return startJob(job, null);
    }

    /** Start an atomic update job. */
    public synchronized JobState startJob(String job, String date) throws IOException {
        Objects.requireNonNull(job, "job must not be null");
        if (isRunning()) throw new IllegalStateException("A job is already running");

        String effectiveDate = (date == null || date.isBlank()) ? DEFAULT_DATE : date;
        List<String> cmd = List.of("bash", launcherPath, job, effectiveDate);

        ProcessBuilder pb = new ProcessBuilder(cmd)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD);

        Process p = pb.start();

        JobState js = new JobState(job, effectiveDate);
        js.pid = safePid(p);
        current = js;

        // Wait until the end, the free the state.
        exec.submit(() -> {
            int exit;
            try {
                exit = p.waitFor();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                exit = Integer.MIN_VALUE;
            }
            js.exitCode = exit;
            js.finished = true;
            js.status = (exit == 0) ? "SUCCEEDED"
                    : ((exit == Integer.MIN_VALUE) ? "FAILED (interrupted)"
                    : ("FAILED (exit=" + exit + ")"));
            synchronized (this) { current = null; }
        });

        return js;
    }

    /** Try to cancel the running job (SIGTERM). */
    public synchronized void cancelCurrent() {
        if (!isRunning()) return;
        try {
            ProcessHandle.of(current.pid).ifPresent(ProcessHandle::destroy);
        } catch (Exception ignore) {}
    }

    private long safePid(Process p) {
        try { return p.pid(); } catch (Throwable t) { return -1L; }
    }
}