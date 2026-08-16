package com.startup.platform.auth.identity;

import com.startup.platform.auth.credential.AuthCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.startup.platform.auth.credential.AuthCredentialService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthLoginServiceIntegrationTest {

    @Autowired
    private AuthLoginService authLoginService;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private AuthCredentialRepository authCredentialRepository;

    @Autowired
    private AuthCredentialService authCredentialService;

    @BeforeEach
    void setUp() {
        authCredentialRepository.deleteAll();
        authUserRepository.deleteAll();
    }

    @Test
    void shouldAuthenticateActiveUserWithValidCredentials() {
        String email = "login@example.com";
        String password = "CorrectHorseBatteryStaple!123";

        AuthUser user = AuthUser.create(email);
        user.activate();
        authUserRepository.save(user);

        authCredentialService.createCredential(user.getUserId(), password);

        AuthUser authenticatedUser = authLoginService.authenticate(email, password);

        assertEquals(user.getUserId(), authenticatedUser.getUserId());
        assertEquals(email, authenticatedUser.getEmail());
        assertEquals(AuthUser.Status.ACTIVE, authenticatedUser.getStatus());
    }

    @Test
    void shouldRejectWrongPassword() {
        String email = "wrong-password@example.com";
        String password = "CorrectHorseBatteryStaple!123";

        AuthUser user = AuthUser.create(email);
        user.activate();
        authUserRepository.save(user);

        authCredentialService.createCredential(user.getUserId(), password);

        assertThrows(
                AuthenticationFailedException.class,
                () -> authLoginService.authenticate(
                        email,
                        "WrongPassword!123"));
    }

    @Test
    void shouldRejectUnknownEmail() {
        assertThrows(
                AuthenticationFailedException.class,
                () -> authLoginService.authenticate(
                        "unknown@example.com",
                        "CorrectHorseBatteryStaple!123"));
    }

    @Test
    void shouldRejectPendingVerificationUser() {
        String email = "pending@example.com";
        String password = "CorrectHorseBatteryStaple!123";

        AuthUser user = AuthUser.create(email);
        authUserRepository.save(user);

        authCredentialService.createCredential(user.getUserId(), password);

        assertThrows(
                AuthenticationFailedException.class,
                () -> authLoginService.authenticate(email, password));
    }

    @Test
    void shouldAuthenticateUsingNormalizedEmail() {
        String storedEmail = "normalized@example.com";
        String password = "CorrectHorseBatteryStaple!123";

        AuthUser user = AuthUser.create(storedEmail);
        user.activate();
        authUserRepository.save(user);

        authCredentialService.createCredential(user.getUserId(), password);

        AuthUser authenticatedUser = authLoginService.authenticate(
                "  NORMALIZED@EXAMPLE.COM  ",
                password);

        assertEquals(user.getUserId(), authenticatedUser.getUserId());
    }
}