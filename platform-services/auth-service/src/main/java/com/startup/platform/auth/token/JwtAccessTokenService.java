package com.startup.platform.auth.token;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class JwtAccessTokenService {

        private static final String ALGORITHM = "RS256";

        private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);

        private final Clock clock;

        public JwtAccessTokenService(Clock clock) {
                this.clock = clock;
        }

        public String issue(
                        UUID userId,
                        UUID sessionId,
                        String issuer,
                        String audience,
                        PrivateKey privateKey,
                        String keyId) {

                Instant issuedAt = clock.instant();

                Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_LIFETIME);

                String header = encodeJson(
                                "{"
                                                + "\"alg\":\"" + ALGORITHM + "\","
                                                + "\"typ\":\"JWT\","
                                                + "\"kid\":\"" + escape(keyId) + "\""
                                                + "}");

                String payload = encodeJson(
                                "{"
                                                + "\"sub\":\""
                                                + escape(userId.toString())
                                                + "\","
                                                + "\"session_id\":\""
                                                + escape(sessionId.toString())
                                                + "\","
                                                + "\"roles\":[],"
                                                + "\"scopes\":[],"
                                                + "\"iss\":\""
                                                + escape(issuer)
                                                + "\","
                                                + "\"aud\":\""
                                                + escape(audience)
                                                + "\","
                                                + "\"iat\":"
                                                + issuedAt.getEpochSecond()
                                                + ","
                                                + "\"exp\":"
                                                + expiresAt.getEpochSecond()
                                                + ","
                                                + "\"jti\":\""
                                                + escape(UUID.randomUUID().toString())
                                                + "\""
                                                + "}");

                String signingInput = header + "." + payload;

                return signingInput
                                + "."
                                + sign(signingInput, privateKey);
        }

        private String encodeJson(String json) {

                return Base64.getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(
                                                json.getBytes(
                                                                StandardCharsets.UTF_8));
        }

        private String sign(
                        String signingInput,
                        PrivateKey privateKey) {

                try {
                        Signature signature = Signature.getInstance(
                                        "SHA256withRSA");

                        signature.initSign(privateKey);

                        signature.update(
                                        signingInput.getBytes(
                                                        StandardCharsets.US_ASCII));

                        return Base64.getUrlEncoder()
                                        .withoutPadding()
                                        .encodeToString(
                                                        signature.sign());

                } catch (GeneralSecurityException exception) {

                        throw new IllegalStateException(
                                        "Unable to sign JWT",
                                        exception);
                }
        }

        private String escape(String value) {

                return value
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"");
        }
}