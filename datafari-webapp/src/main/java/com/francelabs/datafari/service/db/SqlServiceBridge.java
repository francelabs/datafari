package com.francelabs.datafari.service.db;

/**
 * Static bridge to access the Spring-managed SqlService.
 * Allows legacy code (using getInstance) to continue working
 * even outside direct Spring injection.
 */
public class SqlServiceBridge {

    private static SqlService sqlService;

    public static void set(SqlService service) {
        sqlService = service;
    }

    public static SqlService get() {
        if (sqlService == null) {
            throw new IllegalStateException("SqlService not initialized by Spring yet.");
        }
        return sqlService;
    }
}