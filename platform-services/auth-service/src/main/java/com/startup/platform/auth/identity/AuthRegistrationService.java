package com.startup.platform.auth.identity;

import com.startup.platform.auth.credential.AuthCredentialService;
import com.startup.platform.auth.credential.PasswordValidator;
import com.startup.platform.auth.outbox.AuthOutboxEvent;
import com.startup.platform.auth.outbox.AuthOutboxEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.startup.platform.auth.verification.AuthVerificationService;

import java.util.UUID;

@Service
public class AuthRegistrationService {

    private final AuthUserRepository authUserRepository;
    private final AuthCredentialService authCredentialService;
    private final AuthOutboxEventRepository authOutboxEventRepository;
    private final PasswordValidator passwordValidator;
    private final AuthVerificationService authVerificationService;

    public AuthRegistrationService(
            AuthUserRepository authUserRepository,
            AuthCredentialService authCredentialService,
            AuthOutboxEventRepository authOutboxEventRepository,
            PasswordValidator passwordValidator,
            AuthVerificationService authVerificationService) {

        this.authUserRepository = authUserRepository;
        this.authCredentialService = authCredentialService;
        this.authOutboxEventRepository = authOutboxEventRepository;
        this.passwordValidator = passwordValidator;
        this.authVerificationService = authVerificationService;
    }

    @Transactional
    public AuthUser register(String email, String rawPassword) {

        String normalizedEmail = EmailNormalizer.normalize(email);

        passwordValidator.validate(rawPassword);

        if (authUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        AuthUser user = AuthUser.create(normalizedEmail);

        try {
            authUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }

        authCredentialService.createCredential(
                user.getUserId(),
                rawPassword);

        AuthVerificationService.GeneratedVerificationChallenge challenge = authVerificationService
                .createEmailVerificationChallenge(user);

        AuthOutboxEvent event = new AuthOutboxEvent(
                UUID.randomUUID(),
                "AUTH_USER",
                createRegistrationPayload(user, challenge),
                AuthOutboxEvent.Status.PENDING,
                0);

        authOutboxEventRepository.save(event);

        return user;
    }

    private String createRegistrationPayload(
            AuthUser user,
            AuthVerificationService.GeneratedVerificationChallenge challenge) {

        return """
                {
                  "type": "USER_REGISTERED",
                  "user_id": "%s",
                  "email": "%s",
                  "verification_challenge_id": "%s",
                  "verification_otp": "%s"
                }
                """.formatted(
                user.getUserId(),
                user.getEmail(),
                challenge.challengeId(),
                challenge.otp());
    }
}