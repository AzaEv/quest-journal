package io.github.azaev.questjournal.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** The JWT subject is the user's UUID. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
