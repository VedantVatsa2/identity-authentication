package com.startup.platform.auth.api;

import com.startup.platform.auth.identity.AuthLoginSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthLoginController {

    private final AuthLoginSessionService authLoginSessionService;

    public AuthLoginController(
            AuthLoginSessionService authLoginSessionService) {
        this.authLoginSessionService = authLoginSessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthLoginSessionService.LoginResult result = authLoginSessionService.login(
                request.email(),
                request.password());

        ResponseCookie refreshTokenCookie = ResponseCookie
                .from("refresh_token", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/v1/auth")
                .maxAge(30L * 24 * 60 * 60)
                .build();

        LoginResponse response = new LoginResponse(
                result.userId(),
                result.sessionId(),
                result.accessToken());

        return ResponseEntity.ok()
                .header(
                        "Set-Cookie",
                        refreshTokenCookie.toString())
                .body(response);
    }
}