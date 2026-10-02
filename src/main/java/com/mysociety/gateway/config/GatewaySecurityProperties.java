package com.mysociety.gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

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
        // If the property wasn't provided, attempt to load a local .env file (developer convenience).
        if (jwtHmacSecret == null || jwtHmacSecret.length() < 32) {
            // Search for .env in current directory and up to 5 parent directories to accommodate IDE working dirs.
            Path dir = Paths.get(".").toAbsolutePath().normalize();
            for (int i = 0; i <= 5 && dir != null; i++) {
                Path envPath = dir.resolve(".env");
                if (Files.exists(envPath)) {
                    try {
                        for (String line : Files.readAllLines(envPath)) {
                            String l = line.trim();
                            if (l.startsWith("JWT_HMAC_SECRET=")) {
                                String value = l.substring("JWT_HMAC_SECRET=".length()).trim();
                                if (!value.isEmpty()) {
                                    jwtHmacSecret = value;
                                    break;
                                }
                            }
                        }
                    } catch (IOException ignored) {
                        // Ignore read errors; will validate below and throw if still missing.
                    }
                    if (jwtHmacSecret != null && jwtHmacSecret.length() >= 32) {
                        break;
                    }
                }
                dir = dir.getParent();
            }

            // If still not found, attempt to locate .env relative to this class' code location (handles IDE run configurations with different working dirs)
            if (jwtHmacSecret == null || jwtHmacSecret.length() < 32) {
                try {
                    Path codeLocation = Paths.get(GatewaySecurityProperties.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
                    Path searchDir = codeLocation;
                    if (Files.isRegularFile(codeLocation)) {
                        searchDir = codeLocation.getParent();
                    }
                    for (int i = 0; i <= 5 && searchDir != null; i++) {
                        Path envPath = searchDir.resolve(".env");
                        if (Files.exists(envPath)) {
                            try {
                                for (String line : Files.readAllLines(envPath)) {
                                    String l = line.trim();
                                    if (l.startsWith("JWT_HMAC_SECRET=")) {
                                        String value = l.substring("JWT_HMAC_SECRET=".length()).trim();
                                        if (!value.isEmpty()) {
                                            jwtHmacSecret = value;
                                            break;
                                        }
                                    }
                                }
                            } catch (IOException ignored) {
                            }
                        }
                        if (jwtHmacSecret != null && jwtHmacSecret.length() >= 32) {
                            break;
                        }
                        searchDir = searchDir.getParent();
                    }
                } catch (Exception ignored) {
                    // ignore and fall through to final validation error if not found
                }
            }
        }

        if (jwtHmacSecret == null || jwtHmacSecret.length() < 32) {
            throw new IllegalStateException("JWT_HMAC_SECRET must be set to a value of at least 32 characters");
        }
    }

    SecretKeySpec hmacKey() {
        return new SecretKeySpec(jwtHmacSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    }
}
