package com.startup.platform.auth.credential;

import com.startup.platform.auth.identity.AuthUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_credentials")
public class AuthCredential {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private AuthUser user;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "algorithm", nullable = false, length = 32)
    private String algorithm;

    @Column(name = "password_updated_at", nullable = false)
    private LocalDateTime passwordUpdatedAt;

    protected AuthCredential() {
        // Required by JPA.
    }

    public AuthCredential(
            UUID userId,
            String passwordHash,
            String algorithm,
            LocalDateTime passwordUpdatedAt) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.algorithm = algorithm;
        this.passwordUpdatedAt = passwordUpdatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public AuthUser getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public LocalDateTime getPasswordUpdatedAt() {
        return passwordUpdatedAt;
    }
}