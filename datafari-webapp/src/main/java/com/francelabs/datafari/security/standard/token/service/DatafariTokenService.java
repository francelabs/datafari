package com.francelabs.datafari.security.standard.token.service;

import org.springframework.security.core.Authentication;

/**
 * Service responsible for issuing, storing and resolving Datafari access
 * tokens.
 *
 * <p>This service is the central abstraction behind the legacy token mechanism
 * reintroduced in Datafari. It is used in two complementary situations:</p>
 * <ul>
 *   <li>when a token must be created after successful client and user
 *   authentication on {@code /oauth/token},</li>
 *   <li>when a bearer token must be validated and converted back into an
 *   {@link org.springframework.security.core.Authentication} object for an
 *   incoming protected request.</li>
 * </ul>
 *
 * <p>The concrete implementation is responsible for the persistence strategy
 * used for tokens, for example in a SQL table, and for applying token lifetime
 * and validation rules.</p>
 */
public interface DatafariTokenService {

  /**
   * Issues and persists a new access token for the given authenticated user and
   * client.
   *
   * <p>The created token must be uniquely identifiable, associated with the
   * authenticated principal and its authorities, and bound to the supplied
   * client identifier. Its lifetime is implementation specific.</p>
   *
   * @param authentication authenticated user for whom the token is issued
   * @param clientId identifier of the client requesting the token
   * @return a description of the newly issued token and its lifetime
   */
  LegacyAccessToken issueToken(Authentication authentication, String clientId);

  /**
   * Resolves a raw access token into an authenticated Spring Security
   * {@link org.springframework.security.core.Authentication}.
   *
   * <p>If the token exists, is still valid and can be mapped back to a known
   * principal, the method returns the corresponding authentication object.
   * If the token is unknown, expired or invalid, the method returns
   * {@code null}.</p>
   *
   * @param tokenValue raw bearer token value received from the client
   * @return the reconstructed authentication, or {@code null} if the token is
   *         not valid
   */
  Authentication authenticate(String tokenValue);

  /**
   * Revokes a Datafari access token so it can no longer be used to authenticate
   * protected requests.
   *
   * <p>This method is intended to invalidate a token before its natural
   * expiration time, for example when explicit logout or token invalidation is
   * required.</p>
   *
   * <p>If the supplied token is unknown, the method may return silently without
   * performing any action.</p>
   *
   * @param tokenValue raw access token value to revoke
   */
  void revoke(String tokenValue);

  /**
   * Removes all expired Datafari access tokens from the persistence store.
   *
   * <p>This maintenance operation is typically invoked by a scheduled cleanup
   * task in order to keep the token store compact and to remove entries that
   * are no longer usable.</p>
   *
   * @return the number of expired tokens removed
   */
  int deleteExpiredTokens();

}