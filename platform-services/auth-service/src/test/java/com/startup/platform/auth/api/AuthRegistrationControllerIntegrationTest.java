package com.startup.platform.auth.api;

import com.startup.platform.auth.credential.AuthCredentialRepository;
import com.startup.platform.auth.identity.AuthUserRepository;
import com.startup.platform.auth.outbox.AuthOutboxEventRepository;
import com.startup.platform.auth.verification.AuthVerificationChallengeRepository;
import com.startup.platform.auth.session.AuthSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRegistrationControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AuthUserRepository authUserRepository;

  @Autowired
  private AuthCredentialRepository authCredentialRepository;

  @Autowired
  private AuthOutboxEventRepository authOutboxEventRepository;

  @Autowired
  private AuthVerificationChallengeRepository authVerificationChallengeRepository;

  @Autowired
  private AuthSessionRepository authSessionRepository;

  @BeforeEach
  void setUp() {
    authSessionRepository.deleteAll();
    authCredentialRepository.deleteAll();
    authVerificationChallengeRepository.deleteAll();
    authOutboxEventRepository.deleteAll();
    authUserRepository.deleteAll();
  }

  @Test
  void shouldRegisterUserThroughApi() throws Exception {
    mockMvc.perform(post("/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "api@example.com",
              "password": "CorrectHorseBatteryStaple!123"
            }
            """))
        .andExpect(status().isCreated())
        .andExpect(header().string(
            "Location",
            org.hamcrest.Matchers.startsWith(
                "/v1/auth/users/")))
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.status")
            .value("PENDING_VERIFICATION"));
  }

  @Test
  void shouldRejectInvalidRequest() throws Exception {
    mockMvc.perform(post("/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "",
              "password": "short"
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

  @Test
  void shouldRejectDuplicateEmail() throws Exception {
    String request = """
        {
          "email": "duplicate-api@example.com",
          "password": "CorrectHorseBatteryStaple!123"
        }
        """;

    mockMvc.perform(post("/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code")
            .value("EMAIL_ALREADY_REGISTERED"))
        .andExpect(jsonPath("$.status")
            .value(409));
  }

  @Test
  void shouldNormalizeEmailThroughApi() throws Exception {
    mockMvc.perform(post("/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "  API-NORMALIZED@EXAMPLE.COM  ",
              "password": "CorrectHorseBatteryStaple!123"
            }
            """))
        .andExpect(status().isCreated());

    org.junit.jupiter.api.Assertions.assertTrue(
        authUserRepository.findByEmail(
            "api-normalized@example.com").isPresent());
  }
}
