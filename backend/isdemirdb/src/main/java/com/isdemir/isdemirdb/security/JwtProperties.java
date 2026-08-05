package com.isdemir.isdemirdb.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

// Carries the jwt.* settings from application.properties.
//   jwt.secret                     -> signing key (>= 256 bit for HS256)
//   jwt.access-token-expiration-ms  -> access token lifetime (ms)
//   jwt.refresh-token-expiration-ms -> refresh token lifetime (ms)
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationMs;
    private long refreshTokenExpirationMs;
}
