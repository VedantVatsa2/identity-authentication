package com.startup.platform.auth.token;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class JwtAccessTokenValidator {

    private final Clock clock;

    public JwtAccessTokenValidator(Clock clock) {
        this.clock = clock;
    }

    public JwtClaims validate(
            String token,
            PublicKey publicKey,
            String expectedIssuer,
            String expectedAudience) {

        try {
            String[] parts = token.split("\\.", -1);

            if (parts.length != 3) {
                throw invalidToken();
            }

            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String encodedSignature = parts[2];

            String header = decode(encodedHeader);
            String payload = decode(encodedPayload);

            if (!header.contains("\"alg\":\"RS256\"")
                    || !header.contains("\"typ\":\"JWT\"")) {
                throw invalidToken();
            }

            byte[] signatureBytes = Base64.getUrlDecoder()
                    .decode(encodedSignature);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(
                    (encodedHeader + "." + encodedPayload)
                            .getBytes(StandardCharsets.US_ASCII));

            if (!signature.verify(signatureBytes)) {
                throw invalidToken();
            }

            String sub = requiredString(payload, "sub");
            String sessionId = requiredString(payload, "session_id");
            String issuer = requiredString(payload, "iss");
            String audience = requiredString(payload, "aud");
            String jti = requiredString(payload, "jti");

            long issuedAtEpoch = requiredLong(payload, "iat");
            long expiresAtEpoch = requiredLong(payload, "exp");

            if (!expectedIssuer.equals(issuer)) {
                throw invalidToken();
            }

            if (!expectedAudience.equals(audience)) {
                throw invalidToken();
            }

            Instant issuedAt = Instant.ofEpochSecond(issuedAtEpoch);
            Instant expiresAt = Instant.ofEpochSecond(expiresAtEpoch);
            Instant now = clock.instant();

            if (!expiresAt.isAfter(now)) {
                throw invalidToken();
            }

            if (issuedAt.isAfter(now)) {
                throw invalidToken();
            }

            UUID userId = UUID.fromString(sub);
            UUID parsedSessionId = UUID.fromString(sessionId);

            List<String> roles = stringArray(payload, "roles");
            List<String> scopes = stringArray(payload, "scopes");

            return new JwtClaims(
                    userId,
                    parsedSessionId,
                    roles,
                    scopes,
                    issuer,
                    audience,
                    issuedAt,
                    expiresAt,
                    jti);

        } catch (JwtValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    private String decode(String value) {
        return new String(
                Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8);
    }

    private String requiredString(
            String payload,
            String claim) {

        String value = stringClaim(payload, claim);

        if (value == null || value.isBlank()) {
            throw invalidToken();
        }

        return value;
    }

    private long requiredLong(
            String payload,
            String claim) {

        String value = numberClaim(payload, claim);

        if (value == null) {
            throw invalidToken();
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw invalidToken();
        }
    }

    private String stringClaim(
            String payload,
            String claim) {

        String marker = "\"" + claim + "\":\"";
        int start = payload.indexOf(marker);

        if (start < 0) {
            return null;
        }

        start += marker.length();

        int end = payload.indexOf("\"", start);

        if (end < 0) {
            return null;
        }

        return payload.substring(start, end);
    }

    private String numberClaim(
            String payload,
            String claim) {

        String marker = "\"" + claim + "\":";
        int start = payload.indexOf(marker);

        if (start < 0) {
            return null;
        }

        start += marker.length();

        int end = start;

        while (end < payload.length()) {
            char character = payload.charAt(end);

            if (!Character.isDigit(character)
                    && character != '-') {
                break;
            }

            end++;
        }

        if (start == end) {
            return null;
        }

        return payload.substring(start, end);
    }

    private List<String> stringArray(
            String payload,
            String claim) {

        String marker = "\"" + claim + "\":[";
        int start = payload.indexOf(marker);

        if (start < 0) {
            throw invalidToken();
        }

        start += marker.length();

        int end = payload.indexOf("]", start);

        if (end < 0) {
            throw invalidToken();
        }

        String value = payload.substring(start, end).trim();

        if (value.isEmpty()) {
            return List.of();
        }

        String[] elements = value.split(",");

        return java.util.Arrays.stream(elements)
                .map(String::trim)
                .map(this::unquote)
                .toList();
    }

    private String unquote(String value) {

        if (value.length() < 2
                || value.charAt(0) != '"'
                || value.charAt(value.length() - 1) != '"') {
            throw invalidToken();
        }

        return value.substring(1, value.length() - 1);
    }

    private JwtValidationException invalidToken() {
        return new JwtValidationException();
    }

    public record JwtClaims(
            UUID userId,
            UUID sessionId,
            List<String> roles,
            List<String> scopes,
            String issuer,
            String audience,
            Instant issuedAt,
            Instant expiresAt,
            String jti) {
    }

    public static class JwtValidationException
            extends RuntimeException {

        public JwtValidationException() {
            super("Invalid access token");
        }
    }
}