package com.startup.platform.auth.credential;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthCredentialRepository extends JpaRepository<AuthCredential, UUID> {
}