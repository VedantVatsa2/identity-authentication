package com.startup.platform.auth.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthVerificationChallengeRepository
        extends JpaRepository<AuthVerificationChallenge, UUID> {

    Optional<AuthVerificationChallenge> findFirstByUserIdAndChallengeTypeAndStatusOrderByCreatedAtDesc(
            UUID userId,
            AuthVerificationChallenge.ChallengeType challengeType,
            AuthVerificationChallenge.Status status);
}