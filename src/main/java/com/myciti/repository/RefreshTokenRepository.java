package com.myciti.repository;

import com.myciti.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    java.util.List<RefreshToken> findByUser_IdAndRevokedAtIsNullOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    void revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.id IN :ids")
    void revokeByIds(@Param("ids") java.util.Collection<Long> ids, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId AND (rt.revokedAt IS NOT NULL OR rt.expiresAt < :now)")
    void deleteRevokedAndExpiredForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revokedAt IS NOT NULL OR rt.expiresAt < :now")
    int deleteAllRevokedAndExpired(@Param("now") Instant now);
}
