package com.example.demo.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Dto.TokenPairResponse;
import com.example.demo.Entity.RefreshToken;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthService authService;

    private RefreshTokenService refreshTokenService;
    private Users testUser;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, authService);

        testUser = new Users();
        testUser.setUser_id(101);
        testUser.setUsername("alice");
        testUser.setRole(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Should create fresh refresh token with SHA-256 hash, 7-day expiration, and revoked=false")
    void testCreateRefreshToken_Success() {
        String rawToken = refreshTokenService.createRefreshToken(testUser);

        assertNotNull(rawToken);
        assertEquals(36, rawToken.length()); // Standard UUID string length

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertEquals(RefreshTokenService.hashToken(rawToken), saved.getTokenHash());
        assertFalse(saved.isRevoked());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now().plus(6, ChronoUnit.DAYS)));
    }

    @Test
    @DisplayName("Should rotate token: invalidate old token, issue new token pair")
    void testRotateRefreshToken_Success() {
        String rawToken = "old-raw-token-uuid-123456";
        String hash = RefreshTokenService.hashToken(rawToken);

        RefreshToken existing = new RefreshToken(testUser, hash, Instant.now().plus(5, ChronoUnit.DAYS));
        existing.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(existing));
        when(authService.validateCookie(testUser)).thenReturn("new-access-jwt-token");

        TokenPairResponse result = refreshTokenService.rotateRefreshToken(rawToken);

        assertNotNull(result);
        assertEquals("new-access-jwt-token", result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        assertEquals(testUser, result.getUser());

        // Verify old token was marked as revoked
        assertTrue(existing.isRevoked());
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    @DisplayName("Should detect reuse of revoked token, terminate all user sessions, and throw SecurityException")
    void testRotateRefreshToken_ReuseDetected_RevokesAllSessions() {
        String stolenRawToken = "stolen-raw-token-uuid-999999";
        String hash = RefreshTokenService.hashToken(stolenRawToken);

        RefreshToken revokedToken = new RefreshToken(testUser, hash, Instant.now().plus(5, ChronoUnit.DAYS));
        revokedToken.setRevoked(true); // Already revoked!

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(revokedToken));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                refreshTokenService.rotateRefreshToken(stolenRawToken)
        );

        assertTrue(ex.getMessage().contains("Compromised session detected"));

        // Verify all sessions were revoked for this user
        verify(refreshTokenRepository).revokeAllByUserId(101);
        verify(authService, never()).validateCookie(any());
    }

    @Test
    @DisplayName("Should reject expired refresh token and mark as revoked")
    void testRotateRefreshToken_Expired_ThrowsSecurityException() {
        String rawToken = "expired-token-uuid";
        String hash = RefreshTokenService.hashToken(rawToken);

        RefreshToken expiredToken = new RefreshToken(testUser, hash, Instant.now().minus(1, ChronoUnit.DAYS));
        expiredToken.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(expiredToken));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                refreshTokenService.rotateRefreshToken(rawToken)
        );

        assertTrue(ex.getMessage().contains("expired"));
        assertTrue(expiredToken.isRevoked());
        verify(refreshTokenRepository).save(expiredToken);
    }

    @Test
    @DisplayName("Should reject non-existent refresh token")
    void testRotateRefreshToken_NotFound_ThrowsSecurityException() {
        String unknownToken = "unknown-token-uuid";
        String hash = RefreshTokenService.hashToken(unknownToken);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () ->
                refreshTokenService.rotateRefreshToken(unknownToken)
        );
    }

    @Test
    @DisplayName("Should revoke token on logout")
    void testRevokeRefreshToken_Success() {
        String rawToken = "active-token-uuid";
        String hash = RefreshTokenService.hashToken(rawToken);

        RefreshToken token = new RefreshToken(testUser, hash, Instant.now().plus(6, ChronoUnit.DAYS));
        token.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        refreshTokenService.revokeRefreshToken(rawToken);

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }
}

