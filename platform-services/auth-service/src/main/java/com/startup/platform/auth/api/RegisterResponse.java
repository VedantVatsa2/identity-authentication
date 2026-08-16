package com.startup.platform.auth.api;

import com.startup.platform.auth.identity.AuthUser;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        AuthUser.Status status) {
}
