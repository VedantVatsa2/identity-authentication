package com.startup.platform.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email must not be blank") String email,

        @NotBlank(message = "Password must not be blank") @Size(min = 8, max = 256, message = "Password length must be between 8 and 256 characters") String password) {
}
