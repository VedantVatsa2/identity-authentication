package com.startup.platform.auth.credential;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthCredentialService {

    private final AuthCredentialRepository authCredentialRepository;
    private final Argon2PasswordHasher passwordHasher;

    public AuthCredentialService(
            AuthCredentialRepository authCredentialRepository,
            Argon2PasswordHasher passwordHasher) {
        this.authCredentialRepository = authCredentialRepository;
        this.passwordHasher = passwordHasher;
    }

    public Optional<AuthCredential> findByUserId(UUID userId) {
        return authCredentialRepository.findById(userId);
    }

    public AuthCredential createCredential(UUID userId, String rawPassword) {
        String passwordHash = passwordHasher.hash(rawPassword);

        AuthCredential credential = new AuthCredential(
                userId,
                passwordHash,
                "ARGON2ID",
                LocalDateTime.now());

        return authCredentialRepository.save(credential);
    }
}