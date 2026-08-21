package com.startup.platform.auth.verification;

import com.startup.platform.auth.identity.AuthUser;
import com.startup.platform.auth.identity.AuthUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthVerificationService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_BOUND = 1_000_000;
    private static final int OTP_TTL_MINUTES = 10;

    private final AuthVerificationChallengeRepository challengeRepository;
    private final AuthUserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] hmacSecret;

    public AuthVerificationService(
            AuthVerificationChallengeRepository challengeRepository,
            AuthUserRepository userRepository,
            @Value("${auth.verification.hmac-secret}") String hmacSecret) {

        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.hmacSecret = hmacSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public GeneratedVerificationChallenge createEmailVerificationChallenge(
            AuthUser user) {

        LocalDateTime now = LocalDateTime.now();

        String otp = generateOtp();

        AuthVerificationChallenge challenge = new AuthVerificationChallenge(
                UUID.randomUUID(),
                user.getUserId(),
                AuthVerificationChallenge.ChallengeType.EMAIL_VERIFICATION,
                hashOtp(otp),
                now,
                now.plusMinutes(OTP_TTL_MINUTES));

        challengeRepository.save(challenge);

        return new GeneratedVerificationChallenge(
                challenge.getChallengeId(),
                otp);
    }

    @Transactional
    public void verifyEmail(
            UUID challengeId,
            String otp) {

        AuthVerificationChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(VerificationChallengeException::new);

        if (challenge.getStatus() != AuthVerificationChallenge.Status.PENDING) {

            throw new VerificationChallengeException();
        }

        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(challenge.getExpiresAt())) {
            challenge.expire();
            throw new VerificationChallengeException();
        }

        if (challenge.getAttempts() >= AuthVerificationChallenge.MAX_ATTEMPTS) {

            throw new VerificationChallengeException();
        }

        String suppliedHash = hashOtp(otp);

        if (!MessageDigest.isEqual(
                suppliedHash.getBytes(StandardCharsets.UTF_8),
                challenge.getOtpHash().getBytes(StandardCharsets.UTF_8))) {

            challenge.incrementAttempt();
            throw new VerificationChallengeException();
        }

        AuthUser user = userRepository.findById(challenge.getUserId())
                .orElseThrow(VerificationChallengeException::new);

        if (user.getStatus() != AuthUser.Status.PENDING_VERIFICATION) {
            throw new VerificationChallengeException();
        }

        challenge.verify();
        user.activate();
    }

    private String generateOtp() {
        return String.format(
                "%0" + OTP_LENGTH + "d",
                secureRandom.nextInt(OTP_BOUND));
    }

    private String hashOtp(String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    hmacSecret,
                    "HmacSHA256"));

            return HexFormat.of().formatHex(
                    mac.doFinal(
                            otp.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash verification code",
                    exception);
        }
    }

    public record GeneratedVerificationChallenge(
            UUID challengeId,
            String otp) {
    }
}