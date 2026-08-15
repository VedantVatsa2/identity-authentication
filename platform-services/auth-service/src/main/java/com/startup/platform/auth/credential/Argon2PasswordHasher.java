package com.startup.platform.auth.credential;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public Argon2PasswordHasher() {
        this.passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}