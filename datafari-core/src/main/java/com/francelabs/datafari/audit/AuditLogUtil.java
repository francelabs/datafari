package com.francelabs.datafari.audit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuditLogUtil {
    private static final Logger logger = LogManager.getLogger(AuditLogUtil.class);

    public static void log(String key, String user, String ip, String message) {
        logger.info(key + "[" + user + ":" + ip + "]: " + message);
    }
}