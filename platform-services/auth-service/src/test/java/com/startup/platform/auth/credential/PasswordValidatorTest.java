package com.startup.platform.auth.credential;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordValidatorTest {

    private final PasswordValidator passwordValidator = new PasswordValidator();

    @Test
    void shouldAcceptPasswordWithinLengthLimits() {
        assertDoesNotThrow(() -> passwordValidator.validate(
                "CorrectHorseBatteryStaple!123"));
    }

    @Test
    void shouldRejectNullPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> passwordValidator.validate(null));
    }

    @Test
    void shouldRejectPasswordShorterThanMinimumLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> passwordValidator.validate("short"));
    }

    @Test
    void shouldRejectPasswordLongerThanMaximumLength() {
        String password = "a".repeat(257);

        assertThrows(
                IllegalArgumentException.class,
                () -> passwordValidator.validate(password));
    }
}