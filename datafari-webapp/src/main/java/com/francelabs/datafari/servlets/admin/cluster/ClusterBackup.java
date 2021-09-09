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
@WebServlet("/admin/cluster/backup")
public class ClusterBackup extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger(ClusterBackup.class);

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
    public ClusterBackup() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     * 
     *      If an the url contains a parameter action equal to "getReport", then
     *      returns the backup report as plain text.
     * 
     *      Else returns the status in JSON formated as follow:
     * 
     *      <pre>
     * { 
     *   date: String (date) or null, 
     *   fromUser: String or null, 
     *   fromIp: String or null,
     *   inProgress: Boolean, 
     *   reportAvailable: Boolean,
     *   canBackup: Boolean,
     * }
     *      </pre>
     * 
     *      Returns a 403 error code if the installation folders are not standard
     *      and the backup process cannot be executed through this servlet. The JSON
     *      response is the following in this case:
     * 
     *      <pre>
     * { 
     *   error: String 
     * }
     *      </pre>
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final String action = request.getParameter("action");
        if (action != null && action.equals("getReport")) {
            ClusterActionsUtils.outputReport(request, response, ClusterAction.BACKUP);
        } else {
            ClusterActionsUtils.outputStatus(request, response, ClusterAction.BACKUP);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        ClusterActionsUtils.setUnmanagedDoPost(req, resp);
    }

    /**
     * If everything goes OK, returns a 200 (OK) HTTP code. The body is a JSON
     * formated as follows:
     * 
     * <pre>
     * { 
     *   date: String (date) or null, 
     *   fromUser: String or null, 
     *   fromIp: String or null,
     *   inProgress: Boolean, 
     *   report: String or null
     * }
     * </pre>
     * 
     * Returns a 304 (NOT MODIFIED) HTTP code if: 1) A backup has already been
     * requested less than THRESHOLD seconds from the current date. The body is a
     * JSON formated as defined above.
     * 
     * Returns a 400 (BED REQUEST) HTTP code if: 1) A backup is already in progress.
     * The body is a JSON formated as defined above 2) The date given with the query
     * is not NOW. The body is as error JSON as defined below.
     * 
     * Returns a 403 (UNAUTHORIZED) HTTP code if: 1) The installation folders are
     * not standard and the backup process cannot be executed through this servlet.
     * The body is as error JSON as defined below.
     * 
     * Returns a 500 (INTERNAL SERVER ERROR) HTTP code if: 1) Any internal error
     * occurred. The body is as error JSON as defined below.
     * 
     * Errors JSON are formatted as follows:
     * 
     * <pre>
     * { 
     *   error: String 
     * }
     * </pre>
     * 
     */
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

            boolean minimalBackup = false;
            if (requestBody.get("minimalBackup") != null) {
                try {
                    if ((Boolean) requestBody.get("minimalBackup")) {
                        minimalBackup = true;
                    }
                } catch (Exception e) {
                    // Could not cast the property to bool, assume false in this case
                }
            }

            String lastBackupString = config.getProperty(ClusterActionsConfiguration.LAST_BACKUP_DATE);
            Instant putDate = Instant.parse((String) requestBody.get("date"));
            Instant now = Instant.now();
            Instant lastBackup = Instant.MIN;
            if (lastBackupString != null && !lastBackupString.equals("")) {
                Instant.parse(lastBackupString);
            }
            long nowToPutDate = Math.abs(now.until(putDate, ChronoUnit.SECONDS));
            long lastBackupToPutDate = Math.abs(lastBackup.until(putDate, ChronoUnit.SECONDS));
            if (lastBackupToPutDate > THRESHOLD && nowToPutDate < THRESHOLD) {
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
                    AuditLogUtil.log("Backup", authenticatedUserName, req.getRemoteAddr(),
                            "Datafari Backup request received from user " + authenticatedUserName + " from ip "
                                    + req.getRemoteAddr()
                                    + " through the admin UI, answering YES to both questions: Can you "
                                    + "confirm you are aware that Datafari performances will be degraded"
                                    + " while the backup is in progress ? and Do you confirm that all "
                                    + "crawling jobs are stopped ?");

                    String reportName = "cluster-backup-" + dateFormatter.format(putDate) + ".log";

                    // Set the property file containing the last restart date
                    config.setProperty(ClusterActionsConfiguration.LAST_BACKUP_DATE, dateFormatter.format(putDate));
                    config.setProperty(ClusterActionsConfiguration.LAST_BACKUP_IP, req.getRemoteAddr());
                    config.setProperty(ClusterActionsConfiguration.LAST_BACKUP_USER, authenticatedUserName);
                    config.setProperty(ClusterActionsConfiguration.LAST_BACKUP_REPORT, reportName);
                    config.setProperty(ClusterActionsConfiguration.FORCE_UNMANAGED_STATE, "false");
                    config.saveProperties();

                    // Wire up the call to the bash script
                    final String workingDirectory = SCRIPT_WORKING_DIR;
                    final String filePath = REPORT_BASE_DIR + File.separator + reportName;
                    String[] command = { "/bin/bash", "./backupUtils/global_backup_script.sh" };
                    if (minimalBackup) {
                        command = new String[]{"/bin/bash", "./backupUtils/global_backup_script.sh", "minimal"};
                    }
                    final ProcessBuilder p = new ProcessBuilder(command);
                    p.redirectErrorStream(true);
                    p.redirectOutput(new File(filePath));
                    p.directory(new File(workingDirectory));
                    // Not storing the Process, the report file is used to tell when the backup is
                    // done.
                    p.start();

                    jsonResponse.put("success", "true");
                    jsonResponse.put("message", "Backup Started");
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
                    logger.warn("Trying to perform a cluster backup with a date that is not now. " + "Current date: "
                            + dateFormatter.format(now) + "; provided date: " + dateFormatter.format(putDate));
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    jsonResponse.put("success", "false");
                    jsonResponse.put("message",
                            "Can't backup with a date different than now, now and provided date differ by "
                                    + lastBackupToPutDate + "seconds");
                } else if (lastBackupToPutDate <= THRESHOLD) {
                    logger.warn(
                            "Trying to perform a cluster backup with a date that is too close to the last backup. "
                                    + "Current date: " + dateFormatter.format(now) + "; provided date: "
                                    + dateFormatter.format(putDate));
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    jsonResponse.put("success", "false");
                    jsonResponse.put("message", "Server already backup at this date");
                }
            }
        } catch (Exception e) {
            logger.error("Couldn't perform the cluster backup.", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.clear();
            jsonResponse.put("success", "false");
            jsonResponse.put("message", "Unexpected server error while processing the restart query");
        }
        final PrintWriter out = resp.getWriter();
        out.print(jsonResponse);
    }

}