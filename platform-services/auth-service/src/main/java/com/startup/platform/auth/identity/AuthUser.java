package com.startup.platform.auth.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_users")
public class AuthUser {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "email", nullable = false, length = 320, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuthUser() {
        // Required by JPA.
    }

    public AuthUser(UUID userId, String email, Status status, LocalDateTime createdAt) {
        this.userId = userId;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static AuthUser create(String email) {
        return new AuthUser(
                UUID.randomUUID(),
                email,
                Status.PENDING_VERIFICATION,
                LocalDateTime.now());
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public enum Status {
        PENDING_VERIFICATION,
        ACTIVE,
        LOCKED
    }

    public void activate() {
        this.status = Status.ACTIVE;
    }
}