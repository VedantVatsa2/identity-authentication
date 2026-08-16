package com.startup.platform.auth.identity;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }

        if (normalized.length() > 320) {
            throw new IllegalArgumentException("Email is too long");
        }

        if (!isValidFormat(normalized)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        return normalized;
    }

    private static boolean isValidFormat(String email) {
        int atIndex = email.indexOf('@');

        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }

        if (atIndex == email.length() - 1) {
            return false;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        return !localPart.isBlank()
                && !domain.isBlank()
                && !domain.startsWith(".")
                && !domain.endsWith(".")
                && !domain.contains("..");
    }
}