package io.github.azaev.questjournal.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32)
            @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "only latin letters, digits, '_', '.', '-'")
            String username,
            @NotBlank @Size(max = 64) String displayName,
            @NotBlank @Size(min = 8, max = 72) String password,
            String inviteCode) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UserDto(UUID id, String username, String displayName, Instant createdAt) {
        static UserDto from(User user) {
            return new UserDto(user.getId(), user.getUsername(), user.getDisplayName(), user.getCreatedAt());
        }
    }

    public record AuthResponse(String token, Instant expiresAt, UserDto user) {
    }
}
