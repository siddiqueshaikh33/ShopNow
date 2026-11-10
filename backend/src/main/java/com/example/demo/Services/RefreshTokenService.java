package com.example.demo.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.Dto.TokenPairResponse;
import com.example.demo.Entity.RefreshToken;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;

@Service
public class RefreshTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthService authService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AuthService authService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authService = authService;
    }

    @Transactional
    public String createRefreshToken(Users user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS)
        );

        refreshTokenRepository.save(refreshToken);
        LOGGER.info("Created new refresh token for user ID: {}", user.getUser_id());
        return rawToken;
    }

    @Transactional(dontRollbackOn = SecurityException.class)
    public TokenPairResponse rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is missing");
        }

        String incomingHash = hashToken(rawRefreshToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(() -> new SecurityException("Invalid refresh token"));

        // THEFT DETECTION: If a previously revoked token is reused, invalidate ALL tokens for this user!
        if (token.isRevoked()) {
            LOGGER.error("CRITICAL SECURITY ALERT: Revoked refresh token reuse detected for user ID: {}. Revoking all sessions!",
                    token.getUser().getUser_id());
            refreshTokenRepository.revokeAllByUserId(token.getUser().getUser_id());
            throw new SecurityException("Compromised session detected. All user sessions have been terminated. Please log in again.");
        }

        // Expiration check
        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            LOGGER.warn("Expired refresh token for user ID: {}", token.getUser().getUser_id());
            throw new SecurityException("Refresh token has expired. Please log in again.");
        }

        // 1. Invalidate the current refresh token
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        // 2. Issue a brand new refresh token
        String newRawRefreshToken = createRefreshToken(token.getUser());

        // 3. Issue a brand new access token
        String newAccessToken = authService.validateCookie(token.getUser());

        LOGGER.info("Successfully rotated token pair for user ID: {}", token.getUser().getUser_id());
        return new TokenPairResponse(newAccessToken, newRawRefreshToken, token.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String hash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            LOGGER.info("Revoked refresh token for user ID: {}", token.getUser().getUser_id());
        });
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

