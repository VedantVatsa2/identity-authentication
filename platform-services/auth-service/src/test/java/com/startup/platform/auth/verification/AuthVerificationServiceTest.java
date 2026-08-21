package com.startup.platform.auth.verification;

import com.startup.platform.auth.identity.AuthUser;
import com.startup.platform.auth.identity.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthVerificationServiceTest {

    @Mock
    private AuthVerificationChallengeRepository challengeRepository;

    @Mock
    private AuthUserRepository userRepository;

    private AuthVerificationService service;

    @BeforeEach
    void setUp() {
        service = new AuthVerificationService(
                challengeRepository,
                userRepository,
                "test-secret");
    }

    @Test
    void shouldCreateVerificationChallenge() {

        AuthUser user = AuthUser.create("user@example.com");

        when(challengeRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthVerificationService.GeneratedVerificationChallenge result = service.createEmailVerificationChallenge(user);

        assertNotNull(result.challengeId());
        assertNotNull(result.otp());
        assertEquals(6, result.otp().length());
        assertTrue(result.otp().matches("\\d{6}"));

        ArgumentCaptor<AuthVerificationChallenge> captor = ArgumentCaptor.forClass(
                AuthVerificationChallenge.class);

        verify(challengeRepository).save(captor.capture());

        AuthVerificationChallenge challenge = captor.getValue();

        assertEquals(
                user.getUserId(),
                challenge.getUserId());

        assertEquals(
                AuthVerificationChallenge.Status.PENDING,
                challenge.getStatus());

        assertEquals(0, challenge.getAttempts());

        assertNotEquals(
                result.otp(),
                challenge.getOtpHash());
    }

    @Test
    void shouldVerifyCorrectOtpAndActivateUser() {

        AuthUser user = AuthUser.create("user@example.com");

        when(challengeRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthVerificationService.GeneratedVerificationChallenge generated = service
                .createEmailVerificationChallenge(user);

        AuthVerificationChallenge challenge = new AuthVerificationChallenge(
                generated.challengeId(),
                user.getUserId(),
                AuthVerificationChallenge.ChallengeType.EMAIL_VERIFICATION,
                captureOtpHash(generated.otp()),
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10));

        when(challengeRepository.findById(generated.challengeId()))
                .thenReturn(Optional.of(challenge));

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        service.verifyEmail(
                generated.challengeId(),
                generated.otp());

        assertEquals(
                AuthVerificationChallenge.Status.VERIFIED,
                challenge.getStatus());

        assertEquals(
                AuthUser.Status.ACTIVE,
                user.getStatus());
    }

    @Test
    void shouldRejectWrongOtp() {

        AuthUser user = AuthUser.create("user@example.com");

        when(challengeRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthVerificationService.GeneratedVerificationChallenge generated = service
                .createEmailVerificationChallenge(user);

        AuthVerificationChallenge challenge = new AuthVerificationChallenge(
                generated.challengeId(),
                user.getUserId(),
                AuthVerificationChallenge.ChallengeType.EMAIL_VERIFICATION,
                captureOtpHash(generated.otp()),
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10));

        when(challengeRepository.findById(generated.challengeId()))
                .thenReturn(Optional.of(challenge));

        assertThrows(
                VerificationChallengeException.class,
                () -> service.verifyEmail(
                        generated.challengeId(),
                        "000000"));

        assertEquals(1, challenge.getAttempts());
        assertEquals(
                AuthVerificationChallenge.Status.PENDING,
                challenge.getStatus());
    }

    private String captureOtpHash(String otp) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(
                    new javax.crypto.spec.SecretKeySpec(
                            "test-secret".getBytes(),
                            "HmacSHA256"));

            return java.util.HexFormat.of().formatHex(
                    mac.doFinal(otp.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}