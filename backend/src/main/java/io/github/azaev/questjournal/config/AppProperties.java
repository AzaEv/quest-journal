package io.github.azaev.questjournal.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid @NotNull Jwt jwt,
                            @Valid @NotNull Registration registration,
                            @Valid @NotNull Sync sync) {

    public record Jwt(@NotBlank String secret, @NotNull Duration ttl, @NotBlank String issuer) {
        public Jwt {
            if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalArgumentException("app.jwt.secret (JWT_SECRET) must be at least 32 bytes long");
            }
        }
    }

    /** @param inviteCode required for registration when not blank */
    public record Registration(String inviteCode) {
        public boolean inviteRequired() {
            return inviteCode != null && !inviteCode.isBlank();
        }
    }

    public record Sync(@NotNull Duration overlap) {
    }
}
