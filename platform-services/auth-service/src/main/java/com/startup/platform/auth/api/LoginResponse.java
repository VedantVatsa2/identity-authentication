package com.startup.platform.auth.api;

import java.util.UUID;

public record LoginResponse(
        UUID userId,
        UUID sessionId,
        String accessToken) {
}