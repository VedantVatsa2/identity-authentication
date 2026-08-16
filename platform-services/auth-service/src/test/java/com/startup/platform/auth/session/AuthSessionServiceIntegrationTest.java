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
}