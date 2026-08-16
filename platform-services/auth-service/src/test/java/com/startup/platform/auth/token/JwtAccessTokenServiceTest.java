package com.startup.platform.auth.token;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtAccessTokenServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-08-16T17:00:00Z");

    private final Clock clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

    private final JwtAccessTokenService service = new JwtAccessTokenService(clock);

    @Test
    void shouldIssueRs256AccessToken() throws Exception {
        KeyPair keyPair = generateKeyPair();

        UUID userId = UUID.randomUUID();

        String token = service.issue(
                userId,
                "https://auth.startup.local",
                "startup-api",
                keyPair.getPrivate());

        String[] parts = token.split("\\.");

        assertEquals(3, parts.length);

        String header = decode(parts[0]);
        String claims = decode(parts[1]);

        assertTrue(header.contains("\"alg\":\"RS256\""));
        assertTrue(header.contains("\"typ\":\"JWT\""));

        assertEquals(
                userId.toString(),
                claim(claims, "sub"));

        assertEquals(
                "https://auth.startup.local",
                claim(claims, "iss"));

        assertEquals(
                "startup-api",
                claim(claims, "aud"));

        assertEquals(
                String.valueOf(FIXED_TIME.getEpochSecond()),
                claim(claims, "iat"));

        assertEquals(
                String.valueOf(
                        FIXED_TIME
                                .plusSeconds(15 * 60)
                                .getEpochSecond()),
                claim(claims, "exp"));

        assertNotNull(claim(claims, "jti"));
    }

    @Test
    void shouldProduceVerifiableRs256Signature() throws Exception {
        KeyPair keyPair = generateKeyPair();

        String token = service.issue(
                UUID.randomUUID(),
                "https://auth.startup.local",
                "startup-api",
                keyPair.getPrivate());

        String[] parts = token.split("\\.");

        String signingInput = parts[0] + "." + parts[1];

        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        Signature verifier = Signature.getInstance("SHA256withRSA");

        verifier.initVerify(keyPair.getPublic());

        verifier.update(
                signingInput.getBytes(StandardCharsets.US_ASCII));

        assertTrue(verifier.verify(signatureBytes));
    }

    @Test
    void shouldGenerateUniqueJti() throws Exception {
        KeyPair keyPair = generateKeyPair();

        String first = service.issue(
                UUID.randomUUID(),
                "https://auth.startup.local",
                "startup-api",
                keyPair.getPrivate());

        String second = service.issue(
                UUID.randomUUID(),
                "https://auth.startup.local",
                "startup-api",
                keyPair.getPrivate());

        String firstClaims = decode(first.split("\\.")[1]);

        String secondClaims = decode(second.split("\\.")[1]);

        assertNotEquals(
                claim(firstClaims, "jti"),
                claim(secondClaims, "jti"));
    }

    private static KeyPair generateKeyPair()
            throws Exception {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        return generator.generateKeyPair();
    }

    private static String decode(String encoded) {
        return new String(
                Base64.getUrlDecoder().decode(encoded),
                StandardCharsets.UTF_8);
    }

    private static String claim(
            String payload,
            String name) {

        String marker = "\"" + name + "\":";
        int start = payload.indexOf(marker);

        assertTrue(
                start >= 0,
                "Missing claim: " + name);

        start += marker.length();

        if (payload.charAt(start) == '"') {
            int end = payload.indexOf('"', start + 1);

            assertTrue(
                    end >= 0,
                    "Unterminated claim: " + name);

            return payload.substring(start + 1, end);
        }

        int end = payload.indexOf(',', start);

        if (end < 0) {
            end = payload.length();
        }

        return payload.substring(start, end);
    }
}