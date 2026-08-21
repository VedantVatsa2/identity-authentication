package com.startup.platform.auth.api;

import com.startup.platform.auth.token.JwtKeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
public class JwksController {

    private final JwtKeyProvider keyProvider;

    public JwksController(JwtKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {

        RSAPublicKey publicKey = (RSAPublicKey) keyProvider.publicKey();

        return Map.of(
                "keys",
                List.of(
                        Map.of(
                                "kty", "RSA",
                                "use", "sig",
                                "alg", "RS256",
                                "kid", keyProvider.keyId(),
                                "n", encode(publicKey.getModulus()),
                                "e", encode(publicKey.getPublicExponent()))));
    }

    private String encode(BigInteger value) {

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.toByteArray());
    }
}