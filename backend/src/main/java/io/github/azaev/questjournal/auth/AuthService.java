package io.github.azaev.questjournal.auth;

import io.github.azaev.questjournal.auth.AuthDtos.AuthResponse;
import io.github.azaev.questjournal.auth.AuthDtos.LoginRequest;
import io.github.azaev.questjournal.auth.AuthDtos.RegisterRequest;
import io.github.azaev.questjournal.auth.AuthDtos.UserDto;
import io.github.azaev.questjournal.common.ApiException;
import io.github.azaev.questjournal.config.AppProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties props;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       AppProperties props, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (props.registration().inviteRequired() && !inviteMatches(req.inviteCode())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid invite code");
        }
        if (users.existsByUsernameIgnoreCase(req.username())) {
            throw ApiException.conflict("Username is already taken");
        }
        User user = new User(UUID.randomUUID(), req.username(), req.displayName().strip(),
                passwordEncoder.encode(req.password()), now());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) { // two registrations raced for the same name
            throw ApiException.conflict("Username is already taken");
        }
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByUsernameIgnoreCase(req.username())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong username or password"));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return users.findById(userId).map(UserDto::from)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }

    private AuthResponse issueToken(User user) {
        Instant issuedAt = now();
        Instant expiresAt = issuedAt.plus(props.jwt().ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("username", user.getUsername())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, expiresAt, UserDto.from(user));
    }

    private boolean inviteMatches(String candidate) {
        if (candidate == null) {
            return false;
        }
        // constant-time comparison so the code cannot be guessed by timing
        return MessageDigest.isEqual(candidate.getBytes(StandardCharsets.UTF_8),
                props.registration().inviteCode().getBytes(StandardCharsets.UTF_8));
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
