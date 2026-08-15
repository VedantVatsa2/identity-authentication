package com.startup.platform.auth.credential;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthCredentialService {

    private final AuthCredentialRepository authCredentialRepository;

    public AuthCredentialService(AuthCredentialRepository authCredentialRepository) {
        this.authCredentialRepository = authCredentialRepository;
    }

    public Optional<AuthCredential> findByUserId(UUID userId) {
        return authCredentialRepository.findById(userId);
    }
}