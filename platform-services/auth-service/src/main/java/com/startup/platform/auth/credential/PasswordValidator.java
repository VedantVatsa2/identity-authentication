package com.startup.platform.auth.credential;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 256;

    public void validate(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }

        if (rawPassword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password is too short");
        }

        if (rawPassword.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password is too long");
        }
    }
}