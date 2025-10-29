package com.francelabs.datafari.atomicupdates;

import java.util.LinkedHashMap;
import java.util.Map;

public class JobExternalStatus {
    public final String job;           // ex: VECTOR
    public String lastExecIso;         // ex: 2025-10-10T13:55:54.678Z
    public String status;              // ex: DONE, FAILED, ...
    public final boolean sourceAvailable; // true if file exists

    public JobExternalStatus(String job, String lastExecIso, String status, boolean sourceAvailable) {
        this.job = job;
        this.lastExecIso = lastExecIso;
        this.status = status;
        this.sourceAvailable = sourceAvailable;
    }

    public Map<String,Object> toMap() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("job", job);
        m.put("lastExec", lastExecIso);
        m.put("status", status);
        m.put("sourceAvailable", sourceAvailable);
        return m;
    }
}