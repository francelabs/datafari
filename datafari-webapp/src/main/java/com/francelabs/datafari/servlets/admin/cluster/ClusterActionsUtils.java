package com.francelabs.datafari.servlets.admin.cluster;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.ClusterActionsConfiguration;
import com.francelabs.datafari.utils.Environment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ClusterActionsUtils {

    private static final String END_OF_REPORT = "END OF SCRIPT";
    private static final String DATAFARI_HOME = Environment.getEnvironmentVariable("DATAFARI_HOME") != null
            ? Environment.getEnvironmentVariable("DATAFARI_HOME")
            : "/opt/datafari";
    private static final String REPORT_BASE_DIR = DATAFARI_HOME + "/logs";
    private static final int MAX_LINES = 3;

    private static final Logger logger = LogManager.getLogger(ClusterActionsUtils.class);

    public static void setUnmanagedDoPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String action = req.getParameter("action");
        final JSONObject jsonResponse = new JSONObject();
        switch (action) {
            case "setUnmanaged":
                String authenticatedUserName = AuthenticatedUserName.getName(req);

                // Log for audit purposes who did the request
                AuditLogUtil.log("ClusterActions", authenticatedUserName, req.getRemoteAddr(),
                        authenticatedUserName + " asked cluster actions UI to be set to "
                        + "unmanaged mode. The next cluster action will be processed bypassing "
                        + "usual sanity checks.");
                ClusterActionsConfiguration config = ClusterActionsConfiguration.getInstance();
                config.setProperty(ClusterActionsConfiguration.FORCE_UNMANAGED_STATE, "true");
                config.saveProperties();
                jsonResponse.put("success", "true");
                jsonResponse.put("message", "Cluster actions are now unmanaged, proceed with caution!");
                break;
            default:
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.put("success", "false");
                jsonResponse.put("message", "Action not recognized");
                break;
        }
        resp.setContentType("application/json");
        final PrintWriter out = resp.getWriter();
        out.print(jsonResponse);
    }

    public static boolean isActionInProgress(ClusterAction action) throws IOException {
        ClusterActionsConfiguration config = ClusterActionsConfiguration.getInstance();
        String lastReportName = config.getProperty(action.getReportParam());
        if (lastReportName != null && lastReportName.trim().length() > 0) {
            final String filePath = REPORT_BASE_DIR + File.separator + lastReportName;
            ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(filePath), StandardCharsets.UTF_8);
            boolean notInProgress = false;
            String line = reader.readLine();
            for (int i = 0; i < MAX_LINES && line != null; i++) {
                notInProgress = notInProgress | line.contentEquals(END_OF_REPORT);
                line = reader.readLine();
            }
            return !notInProgress;
        } else {
            // No report file available, we assume that the action is not in progress.
            return false;
        }
    }

    public static boolean isReportAvailable(ClusterAction action) {
        ClusterActionsConfiguration config = ClusterActionsConfiguration.getInstance();
        String lastRestartReportName = config.getProperty(action.getReportParam());
        return lastRestartReportName != null && lastRestartReportName.trim().length() != 0;
    }

    public static boolean isStandardInstall() {
        return true;
    }

    public static JSONArray canPerformAction() {
        JSONArray result = new JSONArray();
        try {
            if (!isStandardInstall()) {
                JSONObject item = new JSONObject();
                item.put("code", 1);
                item.put("comment", "Installation is not standard, Cluster Actions API disabled");
                result.add(item);
            } 

            // If the install is not standard, do not even bother trying to fetch the reports.
            if (result.isEmpty()) {
                int i = 2;
                for (ClusterAction action : ClusterAction.values()) {
                    try{
                        if (isActionInProgress(action)) {
                            JSONObject item = new JSONObject();
                            item.put("code", i);
                            item.put("comment", action + " in progress");
                            result.add(item);
                        }
                    } catch(FileNotFoundException e) {
                        logger.warn("Report for cluster action " + action + " is missing or has been removed.");
                        JSONObject item = new JSONObject();
                        item.put("code", 10+i);
                        item.put("comment", "Last report for " + action + " is missing.");
                        result.add(item);
                    }
                    i++;
                }
                // If the install is standard and we had no issues with the reports, we can send an
                // OK response.
                if (result.isEmpty()) {
                    JSONObject item = new JSONObject();
                    item.put("code", 0);
                    item.put("comment", "OK");
                    result.add(item);
                }
            }
        } catch (IOException e) {
            JSONObject item = new JSONObject();
            item.put("code", -1);
            item.put("comment", "Unexpected server error");
            result.add(item);
            logger.error("Couldn't evaluate if cluster actions are possible", e);
        }
        return result;
    }

    public static void outputStatus(HttpServletRequest request, HttpServletResponse response, ClusterAction action)
            throws IOException {
        ClusterActionsConfiguration config = ClusterActionsConfiguration.getInstance();
        boolean actionInProgress = false;
        try {
            actionInProgress = isActionInProgress(action);
        } catch (FileNotFoundException e) {
            logger.info("Log file for last " + action + " missing, it may have been deleted. " +
              "Assume action is not in progress.");
        }
        final JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("date", config.getProperty(action.getDateParam()));
        jsonResponse.put("fromUser", config.getProperty(action.getUserParam()));
        jsonResponse.put("fromIp", config.getProperty(action.getIpParam()));
        jsonResponse.put("inProgress", actionInProgress);
        jsonResponse.put("reportAvailable", isReportAvailable(action));
        jsonResponse.put("canPerformAction", canPerformAction());
        jsonResponse.put("forceUnmanaged", config.getProperty(action.getUnmanagedParam()));
        response.setContentType("application/json");
        response.getWriter().print(jsonResponse.toJSONString());
    }

    public static void outputReport(HttpServletRequest request, HttpServletResponse response, ClusterAction action)
            throws IOException {
        ClusterActionsConfiguration config = ClusterActionsConfiguration.getInstance();
        String lastReportName = config.getProperty(action.getReportParam());
        if (lastReportName != null && lastReportName.length() != 0) {
            try {
                final String filePath = REPORT_BASE_DIR + File.separator + lastReportName;
                final File reportFile = new File(filePath);
                response.setContentType("text/plain");
                FileUtils.copyFile(reportFile, response.getOutputStream());
            } catch (FileNotFoundException e) {
                response.setContentType("text/plain");
                response.getOutputStream().print("Previous report is missing, it may have been deleted.");
            }
        } else {
            response.setContentType("text/plain");
            response.getOutputStream().print("No Report Available");
        }
    }
}