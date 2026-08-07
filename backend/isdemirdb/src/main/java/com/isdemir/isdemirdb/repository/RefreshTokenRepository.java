package com.isdemir.isdemirdb.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Used by the refresh flow (stage 8) to look the incoming token up in the DB.
    Optional<RefreshToken> findByToken(String token);

    // Scheduled cleanup: an expired token can never be used again, so it is deleted.
    // Returns the number of deleted rows.
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
