package com.startup.platform.auth.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthOutboxEventService {

    private final AuthOutboxEventRepository authOutboxEventRepository;

    public AuthOutboxEventService(AuthOutboxEventRepository authOutboxEventRepository) {
        this.authOutboxEventRepository = authOutboxEventRepository;
    }

    public Optional<AuthOutboxEvent> findById(UUID eventId) {
        return authOutboxEventRepository.findById(eventId);
    }
}