package com.startup.platform.auth.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_verification_challenges")
public class AuthVerificationChallenge {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false, length = 32)
    private ChallengeType challengeType;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    protected AuthVerificationChallenge() {
    }

    public AuthVerificationChallenge(
            UUID challengeId,
            UUID userId,
            ChallengeType challengeType,
            String otpHash,
            LocalDateTime createdAt,
            LocalDateTime expiresAt) {

        this.challengeId = challengeId;
        this.userId = userId;
        this.challengeType = challengeType;
        this.otpHash = otpHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.status = Status.PENDING;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ChallengeType getChallengeType() {
        return challengeType;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public Status getStatus() {
        return status;
    }

    public void incrementAttempt() {
        attempts++;

        if (attempts >= MAX_ATTEMPTS) {
            status = Status.LOCKED;
        }
    }

    public void verify() {
        status = Status.VERIFIED;
    }

    public void expire() {
        status = Status.EXPIRED;
    }

    public enum ChallengeType {
        EMAIL_VERIFICATION
    }

    public enum Status {
        PENDING,
        VERIFIED,
        EXPIRED,
        LOCKED
    }
}