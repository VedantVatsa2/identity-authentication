package com.startup.platform.auth.api;

import com.startup.platform.auth.verification.AuthVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthVerificationController {

    private final AuthVerificationService verificationService;

    public AuthVerificationController(
            AuthVerificationService verificationService) {

        this.verificationService = verificationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @Valid @RequestBody VerifyEmailRequest request) {

        verificationService.verifyEmail(
                request.challengeId(),
                request.otp());

        return ResponseEntity.ok(
                Map.of(
                        "status", "ACTIVE",
                        "message", "Email verification successful."));
    }
}