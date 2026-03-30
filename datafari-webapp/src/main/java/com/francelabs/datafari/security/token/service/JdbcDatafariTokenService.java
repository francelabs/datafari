package com.francelabs.datafari.security.token.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JdbcDatafariTokenService implements DatafariTokenService {
  private static final Logger LOGGER = LogManager.getLogger(JdbcDatafariTokenService.class);

  private static final long TOKEN_TTL_SECONDS = 900L; // 15 minutes

  private final JdbcTemplate jdbcTemplate;

  public JdbcDatafariTokenService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // FIXME use JPA to access DB and do CRUD operations
  @Override
  public LegacyAccessToken issueToken(Authentication authentication, String clientId) {
    LOGGER.debug("Start issuing Tolen");

    String tokenValue = UUID.randomUUID().toString();
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(TOKEN_TTL_SECONDS);

    LOGGER.debug("token created: {} - expiresAt: {}", tokenValue, expiresAt);

    String username = authentication.getName();
    String authorities = serializeAuthorities(authentication.getAuthorities());

    LOGGER.debug("Prepare to update table with username: {} - authorities: {}", username, authorities);

    jdbcTemplate.update(
        "INSERT INTO datafari_access_token (token_value, username, client_id, authorities, issued_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
        tokenValue,
        username,
        clientId,
        authorities,
        Timestamp.from(now),
        Timestamp.from(expiresAt)
    );

    return new LegacyAccessToken(tokenValue, TOKEN_TTL_SECONDS);
  }

  @Override
  public Authentication authenticate(String tokenValue) {
    LOGGER.debug("Start authenticate");
    List<TokenRecord> results = jdbcTemplate.query(
        "SELECT username, client_id, authorities, expires_at FROM datafari_access_token WHERE token_value = ?",
        (rs, rowNum) -> new TokenRecord(
            rs.getString("username"),
            rs.getString("client_id"),
            rs.getString("authorities"),
            rs.getTimestamp("expires_at").toInstant()
        ),
        tokenValue
    );

    if (results.isEmpty()) {
      return null;
    }

    TokenRecord record = results.get(0);

    if (record.expiresAt().isBefore(Instant.now())) {
      LOGGER.debug("Token expired");
      jdbcTemplate.update("DELETE FROM datafari_access_token WHERE token_value = ?", tokenValue);
      return null;
    }

    LOGGER.debug("User found: {}", record.username());

    Collection<? extends GrantedAuthority> authorities = deserializeAuthorities(record.authorities());

    LOGGER.debug("Grants found: {}", authorities);

    return UsernamePasswordAuthenticationToken.authenticated(
        record.username(),
        tokenValue,
        authorities
    );
  }

  private String serializeAuthorities(Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(","));
  }

  private List<GrantedAuthority> deserializeAuthorities(String authorities) {
    if (authorities == null || authorities.isBlank()) {
      return List.of();
    }

    return Arrays.stream(authorities.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }

  private record TokenRecord(String username, String clientId, String authorities, Instant expiresAt) {
  }
}