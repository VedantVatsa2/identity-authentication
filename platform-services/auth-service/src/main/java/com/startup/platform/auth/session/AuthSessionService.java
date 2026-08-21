package com.startup.platform.auth.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthSessionService {

        private final AuthSessionRepository authSessionRepository;
        private final RefreshTokenService refreshTokenService;
        private final Clock clock;

        public AuthSessionService(
                        AuthSessionRepository authSessionRepository,
                        RefreshTokenService refreshTokenService,
                        Clock clock) {
                this.authSessionRepository = authSessionRepository;
                this.refreshTokenService = refreshTokenService;
                this.clock = clock;
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

        @Transactional(noRollbackFor = RefreshTokenException.class)
        public RefreshedSession refresh(String presentedRefreshToken) {

                String refreshTokenHash = refreshTokenService.hash(presentedRefreshToken);

                LocalDateTime now = LocalDateTime.now(clock);

                AuthSession session = authSessionRepository
                                .findByRefreshTokenHashForUpdate(refreshTokenHash)
                                .orElseThrow(RefreshTokenException::new);

                if (session.getRevokedAt() != null) {
                        revokeAllActiveSessions(session.getUserId(), now);
                        throw new RefreshTokenException();
                }

                if (!session.getExpiresAt().isAfter(now)) {
                        throw new RefreshTokenException();
                }

                session.revoke(now);

                CreatedSession replacement = createSession(
                                session.getUserId(),
                                session.getClientId(),
                                session.getExpiresAt());

                return new RefreshedSession(
                                session,
                                replacement.session(),
                                replacement.refreshToken());
        }

        private void revokeAllActiveSessions(
                        UUID userId,
                        LocalDateTime revokedAt) {

                List<AuthSession> activeSessions = authSessionRepository.findActiveByUserIdForUpdate(userId);

                activeSessions.forEach(session -> session.revoke(revokedAt));
        }

        public record RefreshedSession(
                        AuthSession previousSession,
                        AuthSession session,
                        String refreshToken) {
        }

        @Transactional
        public void logout(String presentedRefreshToken) {

                String refreshTokenHash = refreshTokenService.hash(presentedRefreshToken);

                LocalDateTime now = LocalDateTime.now(clock);

                AuthSession session = authSessionRepository
                                .findByRefreshTokenHashForUpdate(refreshTokenHash)
                                .orElseThrow(RefreshTokenException::new);

                session.revoke(now);
        }

        public record CreatedSession(
                        AuthSession session,
                        String refreshToken) {
        }
}