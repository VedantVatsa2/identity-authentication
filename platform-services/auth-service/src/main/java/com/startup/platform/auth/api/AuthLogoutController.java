package com.startup.platform.auth.api;

import com.startup.platform.auth.session.AuthSessionService;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthLogoutController {

    private final AuthSessionService authSessionService;

    public AuthLogoutController(
            AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            authSessionService.logout(refreshToken);
        }

        ResponseCookie expiredCookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/v1/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header("Set-Cookie", expiredCookie.toString())
                .build();
    }
}