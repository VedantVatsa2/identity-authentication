package com.startup.platform.auth.identity;

import com.startup.platform.auth.credential.Argon2PasswordHasher;
import com.startup.platform.auth.credential.AuthCredential;
import com.startup.platform.auth.credential.AuthCredentialRepository;
import com.startup.platform.auth.outbox.AuthOutboxEvent;
import com.startup.platform.auth.outbox.AuthOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Transactional
class AuthRegistrationServiceIntegrationTest {

        @Autowired
        private AuthRegistrationService registrationService;

        @Autowired
        private AuthUserRepository authUserRepository;

        @Autowired
        private AuthCredentialRepository authCredentialRepository;

        @MockitoSpyBean
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

        @Test
        void shouldNormalizeEmailDuringRegistration() {
                String rawEmail = "  Registration-Test@Example.COM  ";
                String rawPassword = "CorrectHorseBatteryStaple!123";

                AuthUser user = registrationService.register(
                                rawEmail,
                                rawPassword);

                assertEquals(
                                "registration-test@example.com",
                                user.getEmail());

                AuthUser persistedUser = authUserRepository
                                .findById(user.getUserId())
                                .orElseThrow();

                assertEquals(
                                "registration-test@example.com",
                                persistedUser.getEmail());
        }

        @Test
        void shouldRejectInvalidEmailBeforeCreatingUser() {
                String invalidEmail = "invalid-email";
                String rawPassword = "CorrectHorseBatteryStaple!123";

                assertThrows(
                                IllegalArgumentException.class,
                                () -> registrationService.register(
                                                invalidEmail,
                                                rawPassword));

                assertEquals(0, authUserRepository.count());
                assertEquals(0, authCredentialRepository.count());
                assertEquals(0, authOutboxEventRepository.count());
        }

        @Test
        void shouldRejectInvalidPasswordBeforeCreatingUser() {
                String email = "invalid-password@example.com";
                String invalidPassword = "short";

                assertThrows(
                                IllegalArgumentException.class,
                                () -> registrationService.register(
                                                email,
                                                invalidPassword));

                assertEquals(0, authUserRepository.count());
                assertEquals(0, authCredentialRepository.count());
                assertEquals(0, authOutboxEventRepository.count());
        }

        @Test
        void shouldRejectDuplicateEmail() {
                String email = "duplicate@example.com";
                String rawPassword = "CorrectHorseBatteryStaple!123";

                registrationService.register(email, rawPassword);

                assertThrows(
                                EmailAlreadyRegisteredException.class,
                                () -> registrationService.register(
                                                email,
                                                rawPassword));

                assertEquals(1, authUserRepository.count());
                assertEquals(1, authCredentialRepository.count());
                assertEquals(1, authOutboxEventRepository.count());
        }

        @Test
        void shouldRejectDuplicateEmailAfterNormalization() {
                String rawPassword = "CorrectHorseBatteryStaple!123";

                registrationService.register(
                                "Duplicate@Example.COM",
                                rawPassword);

                assertThrows(
                                EmailAlreadyRegisteredException.class,
                                () -> registrationService.register(
                                                "  duplicate@example.com  ",
                                                rawPassword));

                assertEquals(1, authUserRepository.count());
                assertEquals(1, authCredentialRepository.count());
                assertEquals(1, authOutboxEventRepository.count());
        }

        @Test
        void shouldRollbackUserCredentialAndOutboxWhenRegistrationFailsAfterPersistence() {
                TestTransaction.flagForCommit();
                TestTransaction.end();

                String email = "rollback-test@example.com";
                String rawPassword = "CorrectHorseBatteryStaple!123";

                doThrow(new RuntimeException("Simulated outbox persistence failure"))
                                .when(authOutboxEventRepository)
                                .save(any(AuthOutboxEvent.class));

                assertThrows(
                                RuntimeException.class,
                                () -> registrationService.register(email, rawPassword));

                TestTransaction.start();

                assertEquals(0, authUserRepository.count());
                assertEquals(0, authCredentialRepository.count());
                assertEquals(0, authOutboxEventRepository.count());
        }
}
