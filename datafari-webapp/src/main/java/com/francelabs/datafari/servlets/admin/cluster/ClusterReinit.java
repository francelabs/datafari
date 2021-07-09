package com.francelabs.datafari.servlets.admin.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.ClusterActionsConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.audit.AuditLogUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/admin/cluster/reinitializemcf")
public class ClusterReinit extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger(ClusterReinit.class);

    // Threshold to compare dates, if dates are closer than the threshold, they are
    // considered equal. This is in seconds.
    private static final long THRESHOLD = 10;

    private static final String DATAFARI_HOME = Environment.getEnvironmentVariable("DATAFARI_HOME") != null
            ? Environment.getEnvironmentVariable("DATAFARI_HOME")
            : "/opt/datafari";
    private static final String SCRIPT_WORKING_DIR = DATAFARI_HOME + "/bin";
    private static final String REPORT_BASE_DIR = DATAFARI_HOME + "/logs";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClusterReinit() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final String action = request.getParameter("action");
        if (action != null && action.equals("getReport")) {
            ClusterActionsUtils.outputReport(request, response, ClusterAction.REINIT);
        } else {
            ClusterActionsUtils.outputStatus(request, response, ClusterAction.REINIT);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        ClusterActionsUtils.setUnmanagedDoPost(req, resp);
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        DateTimeFormatter dateFormatter = ClusterActionsConfiguration.dateFormatter;
        ClusterActionsConfiguration config = ClusterActionsConfiguration.getInstance();
        final JSONObject jsonResponse = new JSONObject();
        req.setCharacterEncoding("utf8");
        resp.setContentType("application/json");
        req.getParameter("annotatorActivation");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
            final JSONParser parser = new JSONParser();
            JSONObject requestBody = (JSONObject) parser.parse(br);

            String lastReinitString = config.getProperty(ClusterActionsConfiguration.LAST_REINIT_DATE);
            Instant putDate = Instant.parse((String) requestBody.get("date"));
            Instant now = Instant.now();
            Instant lastReinit = Instant.MIN;
            if (lastReinitString != null && !lastReinitString.equals("")) {
                Instant.parse(lastReinitString);
            }
            long nowToPutDate = Math.abs(now.until(putDate, ChronoUnit.SECONDS));
            long lastReinitToPutDate = Math.abs(lastReinit.until(putDate, ChronoUnit.SECONDS));
            if (lastReinitToPutDate > THRESHOLD && nowToPutDate < THRESHOLD) {
                // If the put date is far enough from the last restart
                // and close enough to the current time => we can restart
                // But we need first to check that the install is standard (mandatory)
                // and that we are either in unmanaged mode, or no other action is in progress
                String unmanagedString = config.getProperty(ClusterActionsConfiguration.FORCE_UNMANAGED_STATE);
                boolean unmanagedMode = unmanagedString != null && unmanagedString.contentEquals("true");
                boolean noActionInProgress = true;
                for (ClusterAction action : ClusterAction.values()) {
                    try {
                        noActionInProgress = noActionInProgress && !ClusterActionsUtils.isActionInProgress(action);
                    } catch (FileNotFoundException e) {
                        logger.info("Last report file for " + action + " not found, actions are not blocked.");
                    }
                }
                if (ClusterActionsUtils.isStandardInstall() && (unmanagedMode || noActionInProgress)) {
                    // retrieve AuthenticatedUserName if user authenticated (it should be as we are
                    // in the admin...)
                    String authenticatedUserName = AuthenticatedUserName.getName(req);

                    // Log for audit purposes who did the request
                    String unmanagedModeNotice = unmanagedMode ? " with UNMANAGED UI state" : "";
                    AuditLogUtil.log("Reinitialization", authenticatedUserName, req.getRemoteAddr(),
                            "Datafari full reinitialization request received from user " + authenticatedUserName + " from ip "
                                    + req.getRemoteAddr() + unmanagedModeNotice
                                    + " through the admin UI, answering YES to both questions: Do you confirm "
                                    + "that you successfully backed up the connectors beforehand (it "
                                    + "is part of the backup process) ? and Do you confirm that "
                                    + "you have understood that you will need to reindex you data ?");

                    String reportName = "cluster-reinitialization-" + dateFormatter.format(putDate) + ".log";

                    // Set the property file containing the last reinitialization date
                    config.setProperty(ClusterActionsConfiguration.LAST_REINIT_DATE, dateFormatter.format(putDate));
                    config.setProperty(ClusterActionsConfiguration.LAST_REINIT_IP, req.getRemoteAddr());
                    config.setProperty(ClusterActionsConfiguration.LAST_REINIT_USER, authenticatedUserName);
                    config.setProperty(ClusterActionsConfiguration.LAST_REINIT_REPORT, reportName);
                    config.setProperty(ClusterActionsConfiguration.FORCE_UNMANAGED_STATE, "false");
                    config.saveProperties();

                    // Wire up the call to the bash script
                    final String workingDirectory = SCRIPT_WORKING_DIR;
                    final String filePath = REPORT_BASE_DIR + File.separator + reportName;
                    final String[] command = { "/bin/bash", "./reinitUtils/global_reinit_restore_mcf.sh" };
                    final ProcessBuilder p = new ProcessBuilder(command);
                    p.redirectErrorStream(true);
                    p.redirectOutput(new File(filePath));
                    p.directory(new File(workingDirectory));
                    // Not storing the Process in any manner, tomcat will be restarted anyway so any
                    // variable would be lost, this would be useless. Could store a process ID but
                    // it is not necessary as the report file will provide us information about the
                    // status.
                    p.start();

                    jsonResponse.put("success", "true");
                    jsonResponse.put("message", "Datafari reinitializing");
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    jsonResponse.put("success", "false");
                    for (ClusterAction action : ClusterAction.values()) {
                        try {
                            if (ClusterActionsUtils.isActionInProgress(action)) {
                                // Only the last action in progress will be added to the result
                                // object, but there should be only one active at a time anyway.
                                jsonResponse.put("message", "A " + action + " is in progress.");
                            };
                        } catch (FileNotFoundException e) {
                            // Nothing to do here, but the file being missing is not a problem, so 
                            // catch it to ensure it does not escalate.
                        }
                    }
                    if(jsonResponse.get("message") == null) {
                        jsonResponse.put("message", "An unidentified error prevented the action completion.");
                    }
                }

            } else {
                // Send an error message.
                if (nowToPutDate >= THRESHOLD) {
                    logger.warn("Trying to perform a cluster reinitialization with a date that is not now. " + "Current date: "
                            + dateFormatter.format(now) + "; provided date: " + dateFormatter.format(putDate));
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    jsonResponse.put("success", "false");
                    jsonResponse.put("message",
                            "Can't reinitialize with a date different than now, now and provided date differ by "
                                    + lastReinitToPutDate + "seconds");
                } else if (lastReinitToPutDate <= THRESHOLD) {
                    logger.warn(
                            "Trying to perform a cluster reinitialization with a date that is too close to the last reinitialization. "
                                    + "Current date: " + dateFormatter.format(now) + "; provided date: "
                                    + dateFormatter.format(putDate));
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    jsonResponse.put("success", "false");
                    jsonResponse.put("message", "Server already reinitialized at this date");
                }
            }
        } catch (Exception e) {
            logger.error("Couldn't perform the cluster reinitialization.", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.clear();
            jsonResponse.put("success", "false");
            jsonResponse.put("message", "Unexpected server error while processing the reinitialization query");
        }
        final PrintWriter out = resp.getWriter();
        out.print(jsonResponse);
    }
}