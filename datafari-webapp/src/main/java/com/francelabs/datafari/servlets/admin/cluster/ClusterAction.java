package com.francelabs.datafari.servlets.admin.cluster;

import com.francelabs.datafari.utils.ClusterActionsConfiguration;

public enum ClusterAction {
    RESTART("Restart", ClusterActionsConfiguration.LAST_RESTART_DATE, ClusterActionsConfiguration.LAST_RESTART_USER, ClusterActionsConfiguration.LAST_RESTART_IP, ClusterActionsConfiguration.LAST_RESTART_REPORT, ClusterActionsConfiguration.FORCE_UNMANAGED_STATE),
    BACKUP("Backup", ClusterActionsConfiguration.LAST_BACKUP_DATE, ClusterActionsConfiguration.LAST_BACKUP_USER, ClusterActionsConfiguration.LAST_BACKUP_IP, ClusterActionsConfiguration.LAST_BACKUP_REPORT, ClusterActionsConfiguration.FORCE_UNMANAGED_STATE),
    REINIT("Reinitialization", ClusterActionsConfiguration.LAST_REINIT_DATE, ClusterActionsConfiguration.LAST_REINIT_USER, ClusterActionsConfiguration.LAST_REINIT_IP, ClusterActionsConfiguration.LAST_REINIT_REPORT, ClusterActionsConfiguration.FORCE_UNMANAGED_STATE);

    private String name;
    private String dateParam;
    private String userParam;
    private String ipParam;
    private String reportParam;
    private String unmanagedParam;

    private ClusterAction(String name, String dateParam, String userParam, String ipParam, String reportParam, String unmanageParam) {
        this.name = name;
        this.dateParam = dateParam;
        this.userParam = userParam;
        this.ipParam = ipParam;
        this.reportParam = reportParam;
        this.unmanagedParam = unmanageParam;
    }

    public String getDateParam() {
        return this.dateParam;
    }

    public String getUserParam() {
        return this.userParam;
    }

    public String getIpParam() {
        return this.ipParam;
    }

    public String getReportParam() {
        return this.reportParam;
    }

    public String getUnmanagedParam() {
        return this.unmanagedParam;
    }

    @Override
    public String toString() {
        return this.name;
    }
}