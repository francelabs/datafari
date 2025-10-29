package com.francelabs.datafari.atomicupdates;

import java.time.Instant;

public class JobState {
    public final String job;         // ex: "VECTOR", "VECTOR_FORCE", ...
    public final String date;        // ex: "full", "2025-10-01", ...
    public final String startedAt = Instant.now().toString();

    public volatile boolean finished = false;
    public volatile long pid = -1L;
    public volatile Integer exitCode = null; // null until the job is done
    public volatile String status = "RUNNING";

    public JobState(String job, String date) {
        this.job = job;
        this.date = date;
    }
}