package com.mysociety.gateway.config;

import jakarta.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    private String jwtHmacSecret;
    private String issuer = "mysociety-identity";

    public String getJwtHmacSecret() {
        return jwtHmacSecret;
    }

    public void setJwtHmacSecret(String jwtHmacSecret) {
        this.jwtHmacSecret = jwtHmacSecret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @PostConstruct
    void validate() {
        if (jwtHmacSecret == null || jwtHmacSecret.length() < 32) {
            throw new IllegalStateException("JWT_HMAC_SECRET must be set to a value of at least 32 characters");
        }
    }

    SecretKeySpec hmacKey() {
        return new SecretKeySpec(jwtHmacSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    }
}
