package com.startup.platform.auth.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
}