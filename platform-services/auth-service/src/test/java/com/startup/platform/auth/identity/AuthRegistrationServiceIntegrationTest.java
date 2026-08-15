package com.startup.platform.auth.identity;

import com.startup.platform.auth.credential.AuthCredential;
import com.startup.platform.auth.credential.AuthCredentialRepository;
import com.startup.platform.auth.credential.Argon2PasswordHasher;
import com.startup.platform.auth.outbox.AuthOutboxEvent;
import com.startup.platform.auth.outbox.AuthOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthRegistrationServiceIntegrationTest {

    @Autowired
    private AuthRegistrationService registrationService;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private AuthCredentialRepository authCredentialRepository;

    @Autowired
    private AuthOutboxEventRepository authOutboxEventRepository;

    @Autowired
    private Argon2PasswordHasher passwordHasher;

    @Test
    void shouldRegisterUserWithCredentialAndOutboxEvent() {
        String email = "registration-test@example.com";
        String rawPassword = "CorrectHorseBatteryStaple!123";

        AuthUser user = registrationService.register(email, rawPassword);

        assertNotNull(user.getUserId());
        assertEquals(email, user.getEmail());
        assertEquals(AuthUser.Status.PENDING_VERIFICATION, user.getStatus());

        AuthUser persistedUser = authUserRepository
                .findById(user.getUserId())
                .orElseThrow();

        assertEquals(email, persistedUser.getEmail());

        AuthCredential credential = authCredentialRepository
                .findById(user.getUserId())
                .orElseThrow();

        assertNotNull(credential.getPasswordHash());
        assertTrue(credential.getPasswordHash().startsWith("$argon2id$"));
        assertEquals("ARGON2ID", credential.getAlgorithm());
        assertTrue(
                passwordHasher.matches(rawPassword, credential.getPasswordHash()));

        AuthOutboxEvent event = authOutboxEventRepository.findAll()
                .stream()
                .filter(candidate -> candidate.getStatus() == AuthOutboxEvent.Status.PENDING)
                .filter(candidate -> candidate.getPayload().contains(user.getUserId().toString()))
                .findFirst()
                .orElseThrow();

        assertEquals("AUTH_USER", event.getAggregateType());
        assertTrue(event.getPayload().contains("USER_REGISTERED"));
        assertTrue(event.getPayload().contains(user.getUserId().toString()));
    }
}