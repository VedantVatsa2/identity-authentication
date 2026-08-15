package com.startup.platform.auth.credential;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Argon2PasswordHasherTest {

    private final Argon2PasswordHasher passwordHasher = new Argon2PasswordHasher();

    @Test
    void shouldHashPasswordAndMatchOriginalPassword() {
        String rawPassword = "CorrectHorseBatteryStaple!123";

        String hash = passwordHasher.hash(rawPassword);

        assertNotNull(hash);
        assertNotEquals(rawPassword, hash);
        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(passwordHasher.matches(rawPassword, hash));
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String rawPassword = "CorrectHorseBatteryStaple!123";

        String hash = passwordHasher.hash(rawPassword);

        assertFalse(passwordHasher.matches("WrongPassword!123", hash));
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        String rawPassword = "CorrectHorseBatteryStaple!123";

        String firstHash = passwordHasher.hash(rawPassword);
        String secondHash = passwordHasher.hash(rawPassword);

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordHasher.matches(rawPassword, firstHash));
        assertTrue(passwordHasher.matches(rawPassword, secondHash));
    }
}