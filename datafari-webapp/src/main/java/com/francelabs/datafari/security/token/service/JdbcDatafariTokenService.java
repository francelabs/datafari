package com.francelabs.datafari.security.token.service;

import com.francelabs.datafari.security.token.DatafariOAuthProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JdbcDatafariTokenService implements DatafariTokenService {
  private static final Logger LOGGER = LogManager.getLogger(JdbcDatafariTokenService.class);
  private final long tokenTtl;
  private final JdbcTemplate jdbcTemplate;

  public JdbcDatafariTokenService(JdbcTemplate jdbcTemplate, DatafariOAuthProperties oAuthProperties) {
    this.jdbcTemplate = jdbcTemplate;
    this.tokenTtl = oAuthProperties.getTokenTtl();
  }

  // TODO use JPA to access DB and do CRUD operations
  @Override
  public LegacyAccessToken issueToken(Authentication authentication, String clientId) {
    LOGGER.debug("Start issuing Tolen");

    String rawToken = UUID.randomUUID().toString();
    String tokenId = hashToken(rawToken);
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(tokenTtl);

    LOGGER.debug("token created: {} - expiresAt: {}", tokenId, expiresAt);

    String username = authentication.getName();
    String authorities = serializeAuthorities(authentication.getAuthorities());

    LOGGER.debug("Prepare to update table with username: {} - authorities: {}", username, authorities);

    jdbcTemplate.update(
        "INSERT INTO datafari_access_token (token_id, username, client_id, authorities, issued_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
        tokenId,
        username,
        clientId,
        authorities,
        Timestamp.from(now),
        Timestamp.from(expiresAt)
    );

    return new LegacyAccessToken(rawToken, tokenTtl);
  }

  @Override
  public Authentication authenticate(String tokenValue) {
    LOGGER.debug("Start authenticate");
    String tokenId = hashToken(tokenValue);
    List<TokenRecord> results = jdbcTemplate.query(
        "SELECT username, client_id, authorities, expires_at FROM datafari_access_token WHERE token_id = ?",
        (rs, rowNum) -> new TokenRecord(
            rs.getString("username"),
            rs.getString("client_id"),
            rs.getString("authorities"),
            rs.getTimestamp("expires_at").toInstant()
        ),
        tokenId
    );

    if (results.isEmpty()) {
      return null;
    }

    TokenRecord record = results.get(0);

    if (record.expiresAt().isBefore(Instant.now())) {
      LOGGER.debug("Token expired");
      jdbcTemplate.update("DELETE FROM datafari_access_token WHERE token_id = ?", tokenId);
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

  @Override
  public void revoke(String tokenValue) {
    String tokenId = hashToken(tokenValue);
    jdbcTemplate.update("DELETE FROM datafari_access_token WHERE token_id = ?", tokenId);
  }

  @Override
  public int deleteExpiredTokens() {
    return jdbcTemplate.update(
        "DELETE FROM datafari_access_token WHERE expires_at < ?",
        Timestamp.from(Instant.now())
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

  private String hashToken(String tokenValue) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(tokenValue.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private record TokenRecord(String username, String clientId, String authorities, Instant expiresAt) {
  }
}