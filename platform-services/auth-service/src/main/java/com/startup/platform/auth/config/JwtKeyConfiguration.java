package com.startup.platform.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfiguration {

    @Bean
    public PrivateKey jwtPrivateKey(
            @Value("${auth.jwt.private-key}") String encodedPrivateKey) {

        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedPrivateKey);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to load JWT private key",
                    exception);
        }
    }
}