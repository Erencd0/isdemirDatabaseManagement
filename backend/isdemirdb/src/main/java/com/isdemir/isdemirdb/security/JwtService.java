package com.isdemir.isdemirdb.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

// Stage 5: producing the JWT access token.
// Stage 6: validating a token (signature + expiry) and reading its content (subject, roles).
// The token carries the user (subject = kullanici_adi) and the roles; it is signed with HS256.
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    // Called on a successful login. subject = kullanici_adi; extra claims: userId, roles.
    public String generateAccessToken(User user, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpirationMs());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSignKey())
                .compact();
    }

    // Is the token valid? Returns false (never throws) when the signature is wrong, the token
    // is malformed or it has expired.
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // subject = kullanici_adi
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // The "roles" claim (List<String>). Empty list when it is missing.
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        return (roles instanceof List) ? (List<String>) roles : List.of();
    }

    // Verifies the signature of the token and extracts the claims. Throws when invalid.
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Builds the HMAC-SHA signing key from the secret.
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
