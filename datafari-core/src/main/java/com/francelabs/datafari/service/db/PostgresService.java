package com.francelabs.datafari.service.db;

import java.sql.*;

public class PostgresService {

    /**
     * Exécute une requête SELECT SQL générique (avec ou sans paramètres).
     * Retourne un ResultSet à traiter ensuite.
     */
    public ResultSet executeSelect(String sql, Object... params) throws SQLException {
        Connection conn = PostgresManager.getInstance().getSession();
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps.executeQuery(); // À fermer manuellement par l'appelant (rs.close(), conn.close())
    }

    /**
     * Exécute une requête INSERT/UPDATE/DELETE SQL (avec ou sans paramètres).
     * Retourne le nombre de lignes affectées.
     */
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = PostgresManager.getInstance().getSession();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    /**
     * Exécute une requête générique sans résultat (type CREATE TABLE, ALTER, etc).
     */
    public void execute(String sql) throws SQLException {
        try (Connection conn = PostgresManager.getInstance().getSession();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}