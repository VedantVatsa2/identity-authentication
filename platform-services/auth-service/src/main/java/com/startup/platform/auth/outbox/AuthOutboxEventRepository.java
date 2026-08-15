package com.startup.platform.auth.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthOutboxEventRepository extends JpaRepository<AuthOutboxEvent, UUID> {
}