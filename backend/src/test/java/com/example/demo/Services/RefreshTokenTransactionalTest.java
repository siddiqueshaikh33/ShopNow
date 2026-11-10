package com.example.demo.Services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.Entity.Provider;
import com.example.demo.Entity.RefreshToken;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.RefreshTokenRepository;
import com.example.demo.Repository.UserRepository;

@SpringBootTest
class RefreshTokenTransactionalTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private Users testUser;
    private final java.util.List<Long> createdTokenIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {
        testUser = new Users();
        testUser.setUsername("tx_test_user_" + UUID.randomUUID().toString().substring(0, 8));
        testUser.setEmail(testUser.getUsername() + "@example.com");
        testUser.setPassword("testSecretPassword123");
        testUser.setRole(Role.CUSTOMER);
        testUser.setProvider(Provider.LOCAL);
        testUser.setCreated_at(LocalDateTime.now());
        testUser.setUpdated_at(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        for (Long id : createdTokenIds) {
            try {
                refreshTokenRepository.deleteById(id);
            } catch (Exception ignored) {
            }
        }
        createdTokenIds.clear();

        if (testUser != null && testUser.getUser_id() != 0) {
            try {
                userRepository.deleteById(testUser.getUser_id());
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("Transaction must commit revocation for all user sessions on token reuse detection even when SecurityException is thrown")
    void testRotateRefreshToken_TheftDetection_PersistsRevocationDespiteSecurityException() {
        // Create an active token for this user (representing another active device)
        String activeRaw = UUID.randomUUID().toString();
        RefreshToken activeToken = new RefreshToken(
                testUser,
                RefreshTokenService.hashToken(activeRaw),
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        activeToken = refreshTokenRepository.save(activeToken);
        createdTokenIds.add(activeToken.getId());

        // Create a stolen, already revoked token
        String stolenRaw = UUID.randomUUID().toString();
        RefreshToken stolenToken = new RefreshToken(
                testUser,
                RefreshTokenService.hashToken(stolenRaw),
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        stolenToken.setRevoked(true);
        stolenToken = refreshTokenRepository.save(stolenToken);
        createdTokenIds.add(stolenToken.getId());

        // Act: Attempt to rotate the stolen token -> throws SecurityException
        assertThrows(SecurityException.class, () ->
                refreshTokenService.rotateRefreshToken(stolenRaw)
        );

        // Assert: The active token MUST be revoked in the database (proves transaction committed, not rolled back)
        RefreshToken reloadedActiveToken = refreshTokenRepository.findById(activeToken.getId()).orElse(null);
        assertNotNull(reloadedActiveToken);
        assertTrue(reloadedActiveToken.isRevoked(), "Active session should have been revoked in DB despite SecurityException");
    }

    @Test
    @DisplayName("Transaction must commit revocation for expired token even when SecurityException is thrown")
    void testRotateRefreshToken_ExpiredToken_PersistsRevocationDespiteSecurityException() {
        String expiredRaw = UUID.randomUUID().toString();
        RefreshToken expiredToken = new RefreshToken(
                testUser,
                RefreshTokenService.hashToken(expiredRaw),
                Instant.now().minus(1, ChronoUnit.DAYS)
        );
        expiredToken.setRevoked(false);
        expiredToken = refreshTokenRepository.save(expiredToken);
        createdTokenIds.add(expiredToken.getId());

        // Act: Attempt to rotate expired token -> throws SecurityException
        assertThrows(SecurityException.class, () ->
                refreshTokenService.rotateRefreshToken(expiredRaw)
        );

        // Assert: The expired token MUST now be revoked in the database
        RefreshToken reloadedExpiredToken = refreshTokenRepository.findById(expiredToken.getId()).orElse(null);
        assertNotNull(reloadedExpiredToken);
        assertTrue(reloadedExpiredToken.isRevoked(), "Expired token should be marked revoked in DB despite SecurityException");
    }
}
