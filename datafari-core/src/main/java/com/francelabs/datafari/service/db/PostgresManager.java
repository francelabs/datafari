package com.francelabs.datafari.service.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresManager {

    private static PostgresManager instance;
    private static final String URL = "jdbc:postgresql://localhost:5432/datafari";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    private PostgresManager() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL Driver not found", e);
        }
    }

    public static synchronized PostgresManager getInstance() {
        if (instance == null) {
            instance = new PostgresManager();
        }
        return instance;
    }

    public Connection getSession() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Optionnel : méthode de test de connexion
    public boolean testConnection() {
        try (Connection conn = getSession()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}