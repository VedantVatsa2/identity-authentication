package com.startup.platform.auth.verification;

public class VerificationChallengeException extends RuntimeException {

    public VerificationChallengeException() {
        super("Invalid or expired verification code.");
    }
}