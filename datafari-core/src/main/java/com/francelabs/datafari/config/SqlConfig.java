package com.francelabs.datafari.config;

import com.francelabs.datafari.service.db.SqlService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class SqlConfig {


  @Bean
  public DataSource dataSource(
      @Value("${spring.datasource.url}") String url,
      @Value("${spring.datasource.username}") String user,
      @Value("${spring.datasource.password}") String pass
  ) {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(url);
    cfg.setUsername(user);
    cfg.setPassword(pass);
    cfg.setMaximumPoolSize(20);
    cfg.setMinimumIdle(2);
    cfg.setPoolName("datafari-pool");
    cfg.setDriverClassName("org.postgresql.Driver");
    return new HikariDataSource(cfg);
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbc) {
    return new NamedParameterJdbcTemplate(jdbc);
  }

  @Bean
  public SqlService sqlService(JdbcTemplate jdbc) {
    
    return new SqlService(jdbc);
  }
}