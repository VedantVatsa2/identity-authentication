package com.startup.platform.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyEmailRequest(

        @NotNull(message = "challengeId is required") UUID challengeId,

        @NotBlank(message = "otp is required") @Pattern(regexp = "\\d{6}", message = "otp must be a 6-digit code") String otp) {
}