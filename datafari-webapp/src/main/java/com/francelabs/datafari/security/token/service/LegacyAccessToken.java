package com.francelabs.datafari.security.token.service;

/**
 * Simple value object representing an issued Datafari access token.
 *
 * <p>This object is returned by the token service after successful token
 * creation and exposes the raw token value together with its validity duration
 * in seconds.</p>
 *
 * @param value raw access token value returned to the client
 * @param expiresIn token lifetime in seconds
 */
public record LegacyAccessToken(String value, long expiresIn) {
}