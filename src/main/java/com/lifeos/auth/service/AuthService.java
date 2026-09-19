package com.lifeos.auth.service;

import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.LoginRequest;
import com.lifeos.auth.dto.RefreshTokenRequest;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.entity.RefreshTokenEntity;
import com.lifeos.auth.repository.RefreshTokenRepository;
import com.lifeos.auth.security.JwtTokenProvider;
import com.lifeos.common.exception.UnauthorizedAccessException;
import com.lifeos.common.security.UserPrincipal;
import com.lifeos.user.dto.UserResponse;
import com.lifeos.user.entity.Role;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone())
                .role(Role.ROLE_USER)
                .preferences(new HashMap<>())
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("Successfully registered new user with ID: {}", user.getId());

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = createAndPersistRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .refreshToken(refreshToken)
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UserEntity user = userRepository.findByIdAndIsDeletedFalse(principal.getId())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            String accessToken = jwtTokenProvider.generateAccessToken(principal);
            String refreshToken = createAndPersistRefreshToken(user);

            log.info("User {} successfully authenticated", user.getId());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                    .refreshToken(refreshToken)
                    .user(UserResponse.fromEntity(user))
                    .build();
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new UnauthorizedAccessException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        String tokenHash = jwtTokenProvider.hashToken(rawToken);

        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedAccessException("Invalid or revoked refresh token"));

        if (tokenEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            tokenEntity.setRevoked(true);
            refreshTokenRepository.save(tokenEntity);
            throw new UnauthorizedAccessException("Refresh token has expired. Please login again.");
        }

        // Token rotation: Revoke the current token and issue a fresh pair
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        UserEntity user = tokenEntity.getUser();
        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = createAndPersistRefreshToken(user);

        log.info("Rotated refresh token for user {}", user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .refreshToken(newRefreshToken)
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Revoked all refresh tokens for user {}", userId);
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                        log.info("Revoked specific refresh token for user {}", token.getUser().getId());
                    });
        }
    }

    private String createAndPersistRefreshToken(UserEntity user) {
        String rawToken = jwtTokenProvider.generateRefreshTokenString();
        String tokenHash = jwtTokenProvider.hashToken(rawToken);

        Instant expiresInstant = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());
        OffsetDateTime expiresAt = expiresInstant.atOffset(ZoneOffset.UTC);

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }
}
