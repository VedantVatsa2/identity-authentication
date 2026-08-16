package com.startup.platform.auth.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenServiceTest {

    private final RefreshTokenService service = new RefreshTokenService();

    @Test
    void shouldGenerate256BitRefreshToken() {
        String token = service.generate();

        assertNotNull(token);
        assertEquals(64, token.length());
    }

    @Test
    void shouldGenerateDifferentTokens() {
        String first = service.generate();
        String second = service.generate();

        assertNotEquals(first, second);
    }

    @Test
    void shouldProduceSha256Hash() {
        String token = service.generate();

        String hash = service.hash(token);

        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertNotEquals(token, hash);
    }

    @Test
    void shouldProduceSameHashForSameToken() {
        String token = service.generate();

        assertEquals(
                service.hash(token),
                service.hash(token));
    }
}