package com.startup.platform.auth.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthSessionService {

    private final AuthSessionRepository authSessionRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthSessionService(
            AuthSessionRepository authSessionRepository,
            RefreshTokenService refreshTokenService) {
        this.authSessionRepository = authSessionRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public CreatedSession createSession(
            UUID userId,
            UUID clientId,
            LocalDateTime expiresAt) {

        String refreshToken = refreshTokenService.generate();
        String refreshTokenHash = refreshTokenService.hash(refreshToken);

        AuthSession session = new AuthSession(
                UUID.randomUUID(),
                userId,
                clientId,
                refreshTokenHash,
                expiresAt,
                null);

        AuthSession savedSession = authSessionRepository.save(session);

        return new CreatedSession(
                savedSession,
                refreshToken);
    }

    @Transactional(readOnly = true)
    public Optional<AuthSession> findById(UUID sessionId) {
        return authSessionRepository.findById(sessionId);
    }

    public record CreatedSession(
            AuthSession session,
            String refreshToken) {
    }
}