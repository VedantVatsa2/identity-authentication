package com.startup.platform.auth.identity;

import com.startup.platform.auth.credential.AuthCredential;
import com.startup.platform.auth.credential.AuthCredentialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthLoginService {

    private final AuthUserService authUserService;
    private final AuthCredentialService authCredentialService;

    public AuthLoginService(
            AuthUserService authUserService,
            AuthCredentialService authCredentialService) {
        this.authUserService = authUserService;
        this.authCredentialService = authCredentialService;
    }

    public AuthUser authenticate(String email, String rawPassword) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        AuthUser user = authUserService.findByEmail(normalizedEmail)
                .orElseThrow(AuthenticationFailedException::new);

        if (user.getStatus() != AuthUser.Status.ACTIVE) {
            throw new AuthenticationFailedException();
        }

        AuthCredential credential = authCredentialService.findByUserId(user.getUserId())
                .orElseThrow(AuthenticationFailedException::new);

        if (!authCredentialService.matchesPassword(
                rawPassword,
                credential.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }

        return user;
    }
}