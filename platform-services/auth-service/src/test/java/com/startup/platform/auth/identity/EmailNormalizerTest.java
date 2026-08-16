package com.startup.platform.auth.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailNormalizerTest {

    @Test
    void shouldNormalizeEmail() {
        String result = EmailNormalizer.normalize(
                "  User@Example.COM  ");

        assertEquals("user@example.com", result);
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailNormalizer.normalize(null));
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailNormalizer.normalize("   "));
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailNormalizer.normalize("invalid-email"));
    }

    @Test
    void shouldRejectEmailWithoutDomain() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailNormalizer.normalize("user@"));
    }

    @Test
    void shouldRejectEmailWithoutLocalPart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailNormalizer.normalize("@example.com"));
    }

    @Test
    void shouldRejectEmailLongerThan320Characters() {
        String email = "a".repeat(310) + "@example.com";

        assertThrows(
                IllegalArgumentException.class,
                () -> EmailNormalizer.normalize(email));
    }
}