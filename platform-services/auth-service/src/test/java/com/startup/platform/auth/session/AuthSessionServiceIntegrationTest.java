package com.startup.platform.auth.session;

import com.startup.platform.auth.identity.AuthUser;
import com.startup.platform.auth.identity.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthSessionServiceIntegrationTest {

        @Autowired
        private AuthSessionService authSessionService;

        @Autowired
        private AuthSessionRepository authSessionRepository;

        @Autowired
        private AuthUserRepository authUserRepository;

        @BeforeEach
        void setUp() {
                authSessionRepository.deleteAll();
                authUserRepository.deleteAll();
        }

        @Test
        void shouldCreateSessionWithHashedRefreshToken() {
                AuthUser user = AuthUser.create("session@example.com");
                authUserRepository.save(user);

                UUID clientId = UUID.randomUUID();
                LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

                AuthSessionService.CreatedSession result = authSessionService.createSession(
                                user.getUserId(),
                                clientId,
                                expiresAt);

                assertNotNull(result.session());
                assertNotNull(result.refreshToken());

                assertEquals(
                                user.getUserId(),
                                result.session().getUserId());

                assertEquals(
                                clientId,
                                result.session().getClientId());

                assertEquals(
                                expiresAt,
                                result.session().getExpiresAt());

                assertNull(result.session().getRevokedAt());

                assertNotEquals(
                                result.refreshToken(),
                                result.session().getRefreshTokenHash());

                assertEquals(
                                64,
                                result.session().getRefreshTokenHash().length());

                AuthSession persisted = authSessionRepository.findById(
                                result.session().getSessionId())
                                .orElseThrow();

                assertEquals(
                                result.session().getRefreshTokenHash(),
                                persisted.getRefreshTokenHash());
        }

        @Test
        void shouldRotateRefreshToken() {
                AuthUser user = AuthUser.create("refresh@example.com");
                authUserRepository.save(user);

                UUID clientId = UUID.randomUUID();
                LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

                AuthSessionService.CreatedSession original = authSessionService.createSession(
                                user.getUserId(),
                                clientId,
                                expiresAt);

                AuthSessionService.RefreshedSession result = authSessionService.refresh(original.refreshToken());

                assertNotNull(result.session());
                assertNotNull(result.refreshToken());

                assertEquals(
                                user.getUserId(),
                                result.session().getUserId());

                assertEquals(
                                clientId,
                                result.session().getClientId());

                assertEquals(
                                expiresAt,
                                result.session().getExpiresAt());

                assertNotEquals(
                                original.refreshToken(),
                                result.refreshToken());

                assertNotEquals(
                                original.session().getSessionId(),
                                result.session().getSessionId());

                assertNotNull(
                                original.session().getRevokedAt());

                assertNull(
                                result.session().getRevokedAt());

                assertNotEquals(
                                original.session().getRefreshTokenHash(),
                                result.session().getRefreshTokenHash());

                assertEquals(
                                2,
                                authSessionRepository.count());
        }

        @Test
        void shouldRejectReuseOfRotatedRefreshToken() {
                AuthUser user = AuthUser.create("refresh-reuse@example.com");
                authUserRepository.save(user);

                UUID clientId = UUID.randomUUID();
                LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

                AuthSessionService.CreatedSession original = authSessionService.createSession(
                                user.getUserId(),
                                clientId,
                                expiresAt);

                authSessionService.refresh(original.refreshToken());

                assertThrows(
                                RefreshTokenException.class,
                                () -> authSessionService.refresh(
                                                original.refreshToken()));
        }

        @Test
        void shouldRejectUnknownRefreshToken() {
                AuthUser user = AuthUser.create("refresh-invalid@example.com");
                authUserRepository.save(user);

                UUID clientId = UUID.randomUUID();
                LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

                authSessionService.createSession(
                                user.getUserId(),
                                clientId,
                                expiresAt);

                assertThrows(
                                RefreshTokenException.class,
                                () -> authSessionService.refresh(
                                                "not-a-valid-refresh-token"));
        }

        @Test
        void shouldRevokeAllActiveSessionsWhenRefreshTokenIsReused() {
                AuthUser user = AuthUser.create("refresh-security@example.com");
                authUserRepository.save(user);

                UUID clientId = UUID.randomUUID();
                LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

                AuthSessionService.CreatedSession first = authSessionService.createSession(
                                user.getUserId(),
                                clientId,
                                expiresAt);

                AuthSessionService.CreatedSession second = authSessionService.createSession(
                                user.getUserId(),
                                clientId,
                                expiresAt);

                authSessionService.refresh(first.refreshToken());

                assertNull(second.session().getRevokedAt());

                assertThrows(
                                RefreshTokenException.class,
                                () -> authSessionService.refresh(
                                                first.refreshToken()));

                AuthSession persistedSecond = authSessionRepository.findById(
                                second.session().getSessionId())
                                .orElseThrow();

                assertNotNull(
                                persistedSecond.getRevokedAt());
        }

        @Test
        void shouldRejectExpiredRefreshToken() {
                AuthUser user = AuthUser.create("refresh-expired@example.com");
                authUserRepository.save(user);

                UUID clientId = UUID.randomUUID();

                AuthSessionService.CreatedSession session = authSessionService.createSession(
                                user.getUserId(),
                                clientId,
                                LocalDateTime.now(Clock.systemUTC()).minusSeconds(1));

                assertThrows(
                                RefreshTokenException.class,
                                () -> authSessionService.refresh(
                                                session.refreshToken()));
        }
}
