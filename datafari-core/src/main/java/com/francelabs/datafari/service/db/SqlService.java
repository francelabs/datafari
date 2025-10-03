package com.francelabs.datafari.service.db;

import javax.annotation.PostConstruct;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlService {

    private static SqlService INSTANCE; // Static bridge for non-Spring singletons

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public SqlService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @PostConstruct
    void registerAsSingletonBridge() {
        // stored when Spring build the bean
        INSTANCE = this;
    }

    /** Static acces from legacy services in singleton */
    public static SqlService get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SqlService not initialized by Spring yet.");
        }
        return INSTANCE;
    }

    public JdbcTemplate jdbc() { return jdbcTemplate; }
    public NamedParameterJdbcTemplate named() { return namedJdbcTemplate; }

    public JdbcTemplate getJdbcTemplate() { return jdbcTemplate; }
    public NamedParameterJdbcTemplate getNamedJdbcTemplate() { return namedJdbcTemplate; }
}