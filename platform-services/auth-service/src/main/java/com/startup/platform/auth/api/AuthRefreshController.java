package com.startup.platform.auth.api;

import com.startup.platform.auth.identity.AuthRefreshSessionService;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthRefreshController {

    private static final long REFRESH_TOKEN_MAX_AGE = 30L * 24 * 60 * 60;

    private final AuthRefreshSessionService authRefreshSessionService;

    public AuthRefreshController(
            AuthRefreshSessionService authRefreshSessionService) {
        this.authRefreshSessionService = authRefreshSessionService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.startup.platform.auth.session.RefreshTokenException();
        }

        AuthRefreshSessionService.RefreshResult result = authRefreshSessionService.refresh(refreshToken);

        ResponseCookie refreshTokenCookie = ResponseCookie
                .from("refresh_token", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/v1/auth")
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .build();

        RefreshResponse response = new RefreshResponse(
                result.userId(),
                result.sessionId(),
                result.accessToken());

        return ResponseEntity.ok()
                .header("Set-Cookie", refreshTokenCookie.toString())
                .body(response);
    }

    public record RefreshResponse(
            java.util.UUID userId,
            java.util.UUID sessionId,
            String accessToken) {
    }
}