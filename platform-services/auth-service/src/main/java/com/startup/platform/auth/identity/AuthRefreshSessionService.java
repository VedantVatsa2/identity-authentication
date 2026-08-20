package com.startup.platform.auth.identity;

import com.startup.platform.auth.session.AuthSessionService;
import com.startup.platform.auth.token.JwtAccessTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PrivateKey;
import java.util.UUID;

@Service
public class AuthRefreshSessionService {

    private static final String ISSUER = "https://auth.startup.local";
    private static final String AUDIENCE = "startup-api";

    private final AuthSessionService authSessionService;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final PrivateKey privateKey;

    public AuthRefreshSessionService(
            AuthSessionService authSessionService,
            JwtAccessTokenService jwtAccessTokenService,
            PrivateKey privateKey) {

        this.authSessionService = authSessionService;
        this.jwtAccessTokenService = jwtAccessTokenService;
        this.privateKey = privateKey;
    }

    @Transactional
    public RefreshResult refresh(String refreshToken) {

        AuthSessionService.RefreshedSession result = authSessionService.refresh(refreshToken);

        UUID userId = result.session().getUserId();
        UUID sessionId = result.session().getSessionId();

        String accessToken = jwtAccessTokenService.issue(
                userId,
                sessionId,
                ISSUER,
                AUDIENCE,
                privateKey);

        return new RefreshResult(
                userId,
                sessionId,
                accessToken,
                result.refreshToken());
    }

    public record RefreshResult(
            UUID userId,
            UUID sessionId,
            String accessToken,
            String refreshToken) {
    }
}