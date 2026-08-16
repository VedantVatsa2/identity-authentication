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
class AuthLoginControllerIntegrationTest {

    private static final String EMAIL = "login-api@example.com";

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
    void shouldLoginThroughApi() throws Exception {
        AuthUser user = authRegistrationService.register(
                EMAIL,
                PASSWORD);

        user.activate();
        authUserRepository.saveAndFlush(user);

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "login-api@example.com",
                          "password": "CorrectHorseBatteryStaple!123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
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

        assertEquals(1, authSessionRepository.count());
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        AuthUser user = authRegistrationService.register(
                EMAIL,
                PASSWORD);

        user.activate();
        authUserRepository.saveAndFlush(user);

        mockMvc.perform(post("/v1/auth/login")

                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "login-api@example.com",
                          "password": "WrongPassword!123"
                        }
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "",
                          "password": ""
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.detail")
                        .isNotEmpty());
    }
}