package com.startup.platform.auth.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Service
public class JwtKeyProvider {

    private static final String KEY_ID = "startup-auth-rs256-1";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtKeyProvider(
            @Value("${auth.jwt.private-key}") String encodedPrivateKey) {

        try {
            byte[] keyBytes = Base64.getDecoder()
                    .decode(encodedPrivateKey);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);

            this.privateKey = keyFactory.generatePrivate(
                    privateKeySpec);

            RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) this.privateKey;

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPublicExponent());

            this.publicKey = keyFactory.generatePublic(
                    publicKeySpec);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to load JWT signing key",
                    exception);
        }
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    public String keyId() {
        return KEY_ID;
    }
}