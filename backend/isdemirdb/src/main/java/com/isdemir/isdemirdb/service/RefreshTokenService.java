package com.isdemir.isdemirdb.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isdemir.isdemirdb.entity.RefreshToken;
import com.isdemir.isdemirdb.exception.InvalidCredentialsException;
import com.isdemir.isdemirdb.repository.RefreshTokenRepository;
import com.isdemir.isdemirdb.security.JwtProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// The life cycle of the refresh token lives in this layer. Unlike the access token, the
// refresh token is NOT a JWT; it is an opaque (random UUID) value stored in the DB. That is
// what makes it revocable on logout (stage 9) by setting aktif=false.
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    // Called on a successful login (stage 7): produces a new refresh token and stores it.
    public RefreshToken create(Integer userId) {
        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plus(
                Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMs())));
        refreshToken.setActive(true);

        return refreshTokenRepository.save(refreshToken);
    }

    // Validates the token arriving in the refresh flow (stage 8): does it exist in the DB, is
    // it active, has it expired? Returns the token when everything is valid, otherwise 401
    // (InvalidCredentialsException).
    public RefreshToken validateAndGet(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token bulunamadı."));
                
        if (refreshToken.getExpiresAt() != null
                && refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token süresi dolmuş.");
        }

        if (refreshToken.getActive() == null || !refreshToken.getActive()) {
            throw new InvalidCredentialsException("Refresh token aktif değil.");
        }



        return refreshToken;
    }

    // Stage 9: logout. Revokes the refresh token by setting aktif=false; it can no longer
    // produce a new access token. When the token is not in the DB -> 401.
    public void logout(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token bulunamadı."));

        refreshToken.setActive(false);
        refreshTokenRepository.save(refreshToken);
    }

    // Periyodik temizlik: suresi dolmus refresh tokenlar DB'de durmasin diye silinir.
    // Araligi application.properties -> jwt.cleanup-cron ile degistirilebilir
    // (varsayilan: her gece 03:00).
    @Scheduled(cron = "${jwt.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        long deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Suresi dolmus refresh token temizligi: {} kayit silindi", deleted);
    }
}
