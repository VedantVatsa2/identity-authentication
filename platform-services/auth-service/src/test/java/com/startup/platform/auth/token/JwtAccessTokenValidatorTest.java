package com.startup.platform.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtAccessTokenValidatorTest {

    private static final String ISSUER = "https://auth.startup.local";

    private static final String AUDIENCE = "startup-api";

    private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");

    private KeyPair keyPair;
    private Clock clock;
    private JwtAccessTokenService tokenService;
    private JwtAccessTokenValidator validator;

    @BeforeEach
    void setUp() throws Exception {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        keyPair = generator.generateKeyPair();

        clock = Clock.fixed(
                NOW,
                ZoneOffset.UTC);

        tokenService = new JwtAccessTokenService(clock);

        validator = new JwtAccessTokenValidator(clock);
    }

    @Test
    void shouldValidateIssuedToken() {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        String token = tokenService.issue(
                userId,
                sessionId,
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate());

        JwtAccessTokenValidator.JwtClaims claims = validator.validate(
                token,
                keyPair.getPublic(),
                ISSUER,
                AUDIENCE);

        assertEquals(userId, claims.userId());
        assertEquals(sessionId, claims.sessionId());
        assertEquals(ISSUER, claims.issuer());
        assertEquals(AUDIENCE, claims.audience());
        assertTrue(claims.roles().isEmpty());
        assertTrue(claims.scopes().isEmpty());
        assertNotNull(claims.jti());
        assertEquals(NOW, claims.issuedAt());
        assertEquals(
                NOW.plusSeconds(900),
                claims.expiresAt());
    }

    @Test
    void shouldRejectInvalidSignature() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate());

        KeyPair anotherKeyPair = generateKeyPair();

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        token,
                        anotherKeyPair.getPublic(),
                        ISSUER,
                        AUDIENCE));
    }

    @Test
    void shouldRejectWrongIssuer() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate());

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        token,
                        keyPair.getPublic(),
                        "https://wrong-issuer",
                        AUDIENCE));
    }

    @Test
    void shouldRejectWrongAudience() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate());

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        token,
                        keyPair.getPublic(),
                        ISSUER,
                        "wrong-audience"));
    }

    @Test
    void shouldRejectExpiredToken() {

        Clock expiredClock = Clock.fixed(
                NOW.plusSeconds(901),
                ZoneOffset.UTC);

        JwtAccessTokenValidator expiredValidator = new JwtAccessTokenValidator(expiredClock);

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate());

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> expiredValidator.validate(
                        token,
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE));
    }

    @Test
    void shouldRejectMalformedToken() {

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        "not-a-jwt",
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE));
    }

    @Test
    void shouldRejectWrongAlgorithm() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate());

        String[] parts = token.split("\\.", -1);

        String header = java.util.Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
                                .getBytes());

        String modifiedToken = header + "." + parts[1] + "." + parts[2];

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        modifiedToken,
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE));
    }

    @Test
    void shouldRejectFutureIssuedToken() {

        Clock futureClock = Clock.fixed(
                NOW.minusSeconds(1),
                ZoneOffset.UTC);

        JwtAccessTokenValidator futureValidator = new JwtAccessTokenValidator(futureClock);

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate());

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> futureValidator.validate(
                        token,
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE));
    }

    private KeyPair generateKeyPair() {

        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            return generator.generateKeyPair();

        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}