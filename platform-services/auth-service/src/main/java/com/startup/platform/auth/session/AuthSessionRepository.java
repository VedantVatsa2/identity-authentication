package com.startup.platform.auth.session;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AuthSession session
            where session.refreshTokenHash = :refreshTokenHash
            """)
    Optional<AuthSession> findByRefreshTokenHashForUpdate(
            String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AuthSession session
            where session.userId = :userId
              and session.revokedAt is null
            """)
    List<AuthSession> findActiveByUserIdForUpdate(
            UUID userId);
}