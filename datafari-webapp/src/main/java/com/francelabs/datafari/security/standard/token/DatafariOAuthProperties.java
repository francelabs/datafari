package com.francelabs.datafari.security.standard.token;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


/**
 * Configuration properties related to Datafari legacy OAuth token issuance.
 *
 * <p>This class binds the application properties prefixed with
 * {@code datafari.oauth} into a dedicated Spring configuration bean.</p>
 *
 * <p>It centralizes the client credentials used by the legacy
 * {@code /oauth/token} endpoint reintroduced for backward compatibility.
 * These properties are notably used:</p>
 * <ul>
 *   <li>by {@code DatafariClientAuthenticator} to validate the client
 *   credentials sent through the HTTP {@code Authorization: Basic ...} header,</li>
 *   <li>by {@code DatafariTokenEndpoint} to identify the client for which
 *   a token is being issued.</li>
 * </ul>
 *
 * <p>Using a dedicated {@code @ConfigurationProperties} bean instead of
 * scattered {@code @Value} injections keeps the OAuth-related configuration
 * grouped in a single place, makes the code easier to understand, and improves
 * maintainability as the security configuration evolves.</p>
 *
 * <p>The expected properties are:</p>
 * <ul>
 *   <li>{@code datafari.oauth.client-id}</li>
 *   <li>{@code datafari.oauth.client-secret}</li>
 *   <li>{@code datafari.oauth.token-ttl}</li>
 * </ul>
 *
 * <p>If no client identifier is configured, the default value
 * {@link #INVALID_CLIENT_ID} is used as a safe sentinel value indicating that
 * no valid OAuth client has been declared yet.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "datafari.oauth")
public class DatafariOAuthProperties {
  /**
   * Sentinel value used when no OAuth client identifier has been configured.
   *
   * <p>This value is intentionally invalid and allows the application to detect
   * an unconfigured or incomplete OAuth setup more explicitly than a null
   * reference would.</p>
   */
  public static final String INVALID_CLIENT_ID = "no-client";
  /**
   * Identifier of the client allowed to request a token from the legacy
   * {@code /oauth/token} endpoint.
   */
  private String clientId = INVALID_CLIENT_ID;
  /**
   * Secret associated with the configured OAuth client identifier.
   *
   * <p>This value is used together with the client identifier to validate
   * incoming HTTP Basic client credentials on the token endpoint.</p>
   */
  private String clientSecret = "";

  /**
   * Expiration time of the access token obtained from the token endpoint
   */
  private long tokenTtl = 900L;

  /**
   * Returns the configured OAuth client identifier.
   *
   * @return the configured client identifier, or {@link #INVALID_CLIENT_ID}
   *         if none has been defined
   */
  public String getClientId() {
    return clientId;
  }
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  /**
   * Returns the configured OAuth client secret.
   *
   * @return the configured client secret
   */
  public String getClientSecret() {
    return clientSecret;
  }
  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  /**
   * Returns the configured Expiration time of the access token obtained from the token endpoint.
   * @return
   */
  public long getTokenTtl() {
    return tokenTtl;
  }
  public void setTokenTtl(long tokenTtl) {
    this.tokenTtl = tokenTtl;
  }
}
