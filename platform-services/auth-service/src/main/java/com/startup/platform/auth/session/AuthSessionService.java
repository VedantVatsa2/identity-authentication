package com.startup.platform.auth.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthSessionService {

    private final AuthSessionRepository authSessionRepository;

    public AuthSessionService(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    public Optional<AuthSession> findById(UUID sessionId) {
        return authSessionRepository.findById(sessionId);
    }
}