package com.startup.platform.auth.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
}