package com.startup.platform.auth.api;

import com.startup.platform.auth.identity.AuthRegistrationService;
import com.startup.platform.auth.identity.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/v1/auth")
public class AuthRegistrationController {

    private final AuthRegistrationService authRegistrationService;

    public AuthRegistrationController(
            AuthRegistrationService authRegistrationService) {
        this.authRegistrationService = authRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthUser user = authRegistrationService.register(
                request.email(),
                request.password());

        RegisterResponse response = new RegisterResponse(
                user.getUserId(),
                user.getStatus());

        return ResponseEntity
                .created(URI.create("/v1/auth/users/" + user.getUserId()))
                .body(response);
    }
}
