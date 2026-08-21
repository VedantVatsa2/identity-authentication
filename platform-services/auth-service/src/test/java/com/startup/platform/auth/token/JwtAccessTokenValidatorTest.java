package com.startup.platform.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtAccessTokenValidatorTest {

    private static final String ISSUER = "https://auth.startup.local";

    private static final String AUDIENCE = "startup-api";

    private static final String KEY_ID = "startup-auth-rs256-1";

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
                keyPair.getPrivate(),
                KEY_ID);

        JwtAccessTokenValidator.JwtClaims claims = validator.validate(
                token,
                keyPair.getPublic(),
                ISSUER,
                AUDIENCE,
                KEY_ID);

        assertEquals(userId, claims.userId());
        assertEquals(sessionId, claims.sessionId());
        assertEquals(ISSUER, claims.issuer());
        assertEquals(AUDIENCE, claims.audience());
        assertTrue(claims.roles().isEmpty());
        assertTrue(claims.scopes().isEmpty());
        assertNotNull(claims.jti());

        assertEquals(
                NOW,
                claims.issuedAt());

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
                keyPair.getPrivate(),
                KEY_ID);

        KeyPair anotherKeyPair = generateKeyPair();

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        token,
                        anotherKeyPair.getPublic(),
                        ISSUER,
                        AUDIENCE,
                        KEY_ID));
    }

    @Test
    void shouldRejectWrongIssuer() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate(),
                KEY_ID);

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        token,
                        keyPair.getPublic(),
                        "https://wrong-issuer",
                        AUDIENCE,
                        KEY_ID));
    }

    @Test
    void shouldRejectWrongAudience() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate(),
                KEY_ID);

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        token,
                        keyPair.getPublic(),
                        ISSUER,
                        "wrong-audience",
                        KEY_ID));
    }

    @Test
    void shouldRejectWrongKeyId() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate(),
                KEY_ID);

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        token,
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE,
                        "wrong-key-id"));
    }

    @Test
    void shouldRejectExpiredToken() {

        Clock expiredClock = Clock.fixed(
                NOW.plusSeconds(901),
                ZoneOffset.UTC);

        JwtAccessTokenValidator expiredValidator = new JwtAccessTokenValidator(
                expiredClock);

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate(),
                KEY_ID);

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> expiredValidator.validate(
                        token,
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE,
                        KEY_ID));
    }

    @Test
    void shouldRejectMalformedToken() {

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        "not-a-jwt",
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE,
                        KEY_ID));
    }

    @Test
    void shouldRejectWrongAlgorithm() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate(),
                KEY_ID);

        String[] parts = token.split("\\.", -1);

        String header = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        ("{\"alg\":\"HS256\","
                                + "\"typ\":\"JWT\","
                                + "\"kid\":\""
                                + KEY_ID
                                + "\"}")
                                .getBytes(
                                        StandardCharsets.UTF_8));

        String modifiedToken = header
                + "."
                + parts[1]
                + "."
                + parts[2];

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        modifiedToken,
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE,
                        KEY_ID));
    }

    @Test
    void shouldRejectFutureIssuedToken() {

        Clock futureClock = Clock.fixed(
                NOW.minusSeconds(1),
                ZoneOffset.UTC);

        JwtAccessTokenValidator futureValidator = new JwtAccessTokenValidator(
                futureClock);

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate(),
                KEY_ID);

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> futureValidator.validate(
                        token,
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE,
                        KEY_ID));
    }

    @Test
    void shouldRejectMissingKeyId() {

        String token = tokenService.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ISSUER,
                AUDIENCE,
                keyPair.getPrivate(),
                KEY_ID);

        String[] parts = token.split("\\.", -1);

        String header = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        ("{\"alg\":\"RS256\","
                                + "\"typ\":\"JWT\"}")
                                .getBytes(StandardCharsets.UTF_8));

        String modifiedToken = header
                + "."
                + parts[1]
                + "."
                + parts[2];

        assertThrows(
                JwtAccessTokenValidator.JwtValidationException.class,
                () -> validator.validate(
                        modifiedToken,
                        keyPair.getPublic(),
                        ISSUER,
                        AUDIENCE,
                        KEY_ID));
    }

    private KeyPair generateKeyPair() {

        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            return generator.generateKeyPair();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    exception);
        }
    }
}