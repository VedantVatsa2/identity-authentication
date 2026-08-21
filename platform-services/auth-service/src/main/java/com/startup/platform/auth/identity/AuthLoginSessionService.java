package com.startup.platform.auth.identity;

import com.startup.platform.auth.session.AuthSessionService;
import com.startup.platform.auth.token.JwtAccessTokenService;
import com.startup.platform.auth.token.JwtKeyProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthLoginSessionService {

        private static final String ISSUER = "https://auth.startup.local";
        private static final String AUDIENCE = "startup-api";

        private static final Duration SESSION_LIFETIME = Duration.ofDays(30);

        private final JwtKeyProvider jwtKeyProvider;
        private final AuthLoginService authLoginService;
        private final AuthSessionService authSessionService;
        private final JwtAccessTokenService jwtAccessTokenService;
        private final UUID clientId;
        private final Clock clock;

        public AuthLoginSessionService(
                        AuthLoginService authLoginService,
                        AuthSessionService authSessionService,
                        JwtAccessTokenService jwtAccessTokenService,
                        JwtKeyProvider jwtKeyProvider,
                        @Value("${auth.client-id}") UUID clientId,
                        Clock clock) {

                this.authLoginService = authLoginService;
                this.authSessionService = authSessionService;
                this.jwtAccessTokenService = jwtAccessTokenService;
                this.jwtKeyProvider = jwtKeyProvider;
                this.clientId = clientId;
                this.clock = clock;
        }

        @Transactional
        public LoginResult login(
                        String email,
                        String rawPassword) {

                AuthUser user = authLoginService.authenticate(
                                email,
                                rawPassword);

                LocalDateTime sessionExpiresAt = LocalDateTime.ofInstant(
                                clock.instant(),
                                clock.getZone())
                                .plus(SESSION_LIFETIME);

                AuthSessionService.CreatedSession createdSession = authSessionService.createSession(
                                user.getUserId(),
                                clientId,
                                sessionExpiresAt);

                String accessToken = jwtAccessTokenService.issue(
                                user.getUserId(),
                                createdSession.session().getSessionId(),
                                ISSUER,
                                AUDIENCE,
                                jwtKeyProvider.privateKey(),
                                jwtKeyProvider.keyId());

                return new LoginResult(
                                user.getUserId(),
                                createdSession.session().getSessionId(),
                                accessToken,
                                createdSession.refreshToken());
        }

        public record LoginResult(
                        UUID userId,
                        UUID sessionId,
                        String accessToken,
                        String refreshToken) {
        }
}