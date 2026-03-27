package com.francelabs.datafari.security.token;

public record LegacyAccessToken(String value, long expiresIn) {
}