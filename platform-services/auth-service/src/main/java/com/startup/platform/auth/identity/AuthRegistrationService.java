package com.startup.platform.auth.identity;

import com.startup.platform.auth.credential.AuthCredentialService;
import com.startup.platform.auth.credential.PasswordValidator;
import com.startup.platform.auth.outbox.AuthOutboxEvent;
import com.startup.platform.auth.outbox.AuthOutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthRegistrationService {

    private final AuthUserRepository authUserRepository;
    private final AuthCredentialService authCredentialService;
    private final AuthOutboxEventRepository authOutboxEventRepository;
    private final PasswordValidator passwordValidator;

    public AuthRegistrationService(
            AuthUserRepository authUserRepository,
            AuthCredentialService authCredentialService,
            AuthOutboxEventRepository authOutboxEventRepository,
            PasswordValidator passwordValidator) {
        this.authUserRepository = authUserRepository;
        this.authCredentialService = authCredentialService;
        this.authOutboxEventRepository = authOutboxEventRepository;
        this.passwordValidator = passwordValidator;
    }

    @Transactional
    public AuthUser register(String email, String rawPassword) {

        String normalizedEmail = EmailNormalizer.normalize(email);

        passwordValidator.validate(rawPassword);

        AuthUser user = AuthUser.create(normalizedEmail);

        authUserRepository.save(user);

        authCredentialService.createCredential(
                user.getUserId(),
                rawPassword);

        AuthOutboxEvent event = new AuthOutboxEvent(
                UUID.randomUUID(),
                "AUTH_USER",
                createRegistrationPayload(user),
                AuthOutboxEvent.Status.PENDING,
                0);

        authOutboxEventRepository.save(event);

        return user;
    }

    private String createRegistrationPayload(AuthUser user) {
        return """
                {
                  "type": "USER_REGISTERED",
                  "user_id": "%s",
                  "email": "%s"
                }
                """.formatted(
                user.getUserId(),
                user.getEmail());
    }
}