package com.startup.platform.auth.api;

import com.startup.platform.auth.credential.AuthCredentialRepository;
import com.startup.platform.auth.identity.AuthRegistrationService;
import com.startup.platform.auth.identity.AuthUser;
import com.startup.platform.auth.identity.AuthUserRepository;
import com.startup.platform.auth.outbox.AuthOutboxEventRepository;
import com.startup.platform.auth.session.AuthSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRefreshControllerIntegrationTest {

    private static final String EMAIL = "refresh-api@example.com";

    private static final String PASSWORD = "CorrectHorseBatteryStaple!123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthRegistrationService authRegistrationService;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private AuthCredentialRepository authCredentialRepository;

    @Autowired
    private AuthOutboxEventRepository authOutboxEventRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @BeforeEach
    void setUp() {
        authSessionRepository.deleteAll();
        authCredentialRepository.deleteAll();
        authOutboxEventRepository.deleteAll();
        authUserRepository.deleteAll();
    }

    @Test
    void shouldRefreshThroughApi() throws Exception {
        AuthUser user = authRegistrationService.register(
                EMAIL,
                PASSWORD);

        user.activate();
        authUserRepository.saveAndFlush(user);

        String loginResponse = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "refresh-api@example.com",
                          "password": "CorrectHorseBatteryStaple!123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");

        String refreshToken = extractRefreshToken(loginResponse);

        String originalSessionId = authSessionRepository.findAll()
                .get(0)
                .getSessionId()
                .toString();

        mockMvc.perform(post("/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie(
                        "refresh_token",
                        refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(user.getUserId().toString()))
                .andExpect(jsonPath("$.sessionId")
                        .isNotEmpty())
                .andExpect(jsonPath("$.sessionId")
                        .value(org.hamcrest.Matchers.not(
                                originalSessionId)))
                .andExpect(jsonPath("$.accessToken")
                        .isNotEmpty())
                .andExpect(jsonPath("$.refreshToken")
                        .doesNotExist())
                .andExpect(header().string(
                        "Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString(
                                        "refresh_token="),
                                org.hamcrest.Matchers.containsString(
                                        "HttpOnly"),
                                org.hamcrest.Matchers.containsString(
                                        "Secure"),
                                org.hamcrest.Matchers.containsString(
                                        "SameSite=Strict"),
                                org.hamcrest.Matchers.containsString(
                                        "Path=/v1/auth"))));

        assertEquals(2, authSessionRepository.count());
    }

    @Test
    void shouldRejectRefreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.detail")
                        .value("Invalid refresh token."));
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie(
                        "refresh_token",
                        "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.detail")
                        .value("Invalid refresh token."));
    }

    @Test
    void shouldRejectReuseOfOldRefreshToken() throws Exception {
        AuthUser user = authRegistrationService.register(
                EMAIL,
                PASSWORD);

        user.activate();
        authUserRepository.saveAndFlush(user);

        String setCookie = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "refresh-api@example.com",
                          "password": "CorrectHorseBatteryStaple!123"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");

        String refreshToken = extractRefreshToken(setCookie);

        mockMvc.perform(post("/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie(
                        "refresh_token",
                        refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie(
                        "refresh_token",
                        refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));

        long activeSessions = authSessionRepository.findAll()
                .stream()
                .filter(session -> session.getRevokedAt() == null)
                .count();

        assertEquals(0, activeSessions);
    }

    private String extractRefreshToken(String setCookie) {
        String prefix = "refresh_token=";

        int start = setCookie.indexOf(prefix);
        if (start < 0) {
            throw new AssertionError(
                    "refresh_token cookie was not returned");
        }

        start += prefix.length();

        int end = setCookie.indexOf(';', start);

        if (end < 0) {
            end = setCookie.length();
        }

        return setCookie.substring(start, end);
    }
}