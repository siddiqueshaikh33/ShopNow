package com.example.demo.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.JwtRepository;
import com.example.demo.Repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_SECRET = "a-very-long-secret-key-that-is-at-least-64-bytes-long-for-hmac-sha-256-verification-security";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtRepository jwtRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtRepository, passwordEncoder, TEST_SECRET);
    }

    @Test
    @DisplayName("Should seal userId, username, and role into token claims and decode in memory")
    void testCreateNewCookie_ClaimsSealedInToken() {
        String token = authService.createNewCookie(42, "alex", "ADMIN");

        assertNotNull(token);

        Claims claims = authService.extractAllClaims(token);
        assertEquals("alex", claims.getSubject());
        assertEquals(42, claims.get("userId", Integer.class));
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    @DisplayName("Should verify valid token in memory without any database queries")
    void testVerifyToken_ValidToken() {
        String token = authService.createNewCookie(10, "bob", "CUSTOMER");

        assertTrue(authService.verifyToken(token));

        // Verify zero database queries
        verify(jwtRepository, never()).findByToken(anyString());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should reject tampered token with invalid signature")
    void testVerifyToken_TamperedToken_ReturnsFalse() {
        String token = authService.createNewCookie(10, "bob", "CUSTOMER");
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(authService.verifyToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testVerifyToken_ExpiredToken_ReturnsFalse() {
        Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .setSubject("bob")
                .claim("userId", 10)
                .claim("role", "CUSTOMER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 100000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(authService.verifyToken(expiredToken));
    }

    @Test
    @DisplayName("validateCookie should return stateless token without querying or writing to MySQL")
    void testValidateCookie_StatelessNoDbInteraction() {
        Users user = new Users();
        user.setUser_id(55);
        user.setUsername("sarah");
        user.setRole(Role.CUSTOMER);

        String token = authService.validateCookie(user);

        assertNotNull(token);
        Claims claims = authService.extractAllClaims(token);
        assertEquals(55, claims.get("userId", Integer.class));
        assertEquals("sarah", claims.getSubject());
        assertEquals("CUSTOMER", claims.get("role", String.class));

        // Assert zero DB writes or reads
        verify(jwtRepository, never()).save(any());
        verify(jwtRepository, never()).findByUserId(anyInt());
    }

    @Test
    @DisplayName("Should authenticate user with valid BCrypt password")
    void testAuthenticate_Success_BCrypt() {
        Users user = new Users();
        user.setUser_id(1);
        user.setUsername("alice");
        user.setPassword("$2a$10$encryptedHashPasswordExample12345678901234567890");

        when(userRepository.findByUsername("alice")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("secretPassword", user.getPassword())).thenReturn(true);

        Users authenticated = authService.authenicate("alice", "secretPassword");

        assertNotNull(authenticated);
        assertEquals("alice", authenticated.getUsername());
        verify(passwordEncoder).matches("secretPassword", "$2a$10$encryptedHashPasswordExample12345678901234567890");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when BCrypt password does not match")
    void testAuthenticate_InvalidPassword_BCrypt() {
        Users user = new Users();
        user.setUsername("alice");
        user.setPassword("$2a$10$encryptedHashPasswordExample12345678901234567890");

        when(userRepository.findByUsername("alice")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);

        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> authService.authenicate("alice", "wrongPassword")
        );

        assertEquals("Invalid Password", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when user is not found in database")
    void testAuthenticate_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(java.util.Optional.empty());

        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> authService.authenicate("unknown", "anyPass")
        );

        assertEquals("Invalid Username or No User Found Please Register", ex.getMessage());
    }

    @Test
    @DisplayName("Should migrate plaintext password to BCrypt and save user")
    void testAuthenticate_PlaintextPasswordMigration() {
        Users user = new Users();
        user.setUser_id(3);
        user.setUsername("legacyUser");
        user.setPassword("plainText123");

        when(userRepository.findByUsername("legacyUser")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.encode("plainText123")).thenReturn("$2a$10$newlyHashedBCryptValue");

        Users authenticated = authService.authenicate("legacyUser", "plainText123");

        assertNotNull(authenticated);
        assertEquals("$2a$10$newlyHashedBCryptValue", user.getPassword());
        verify(passwordEncoder).encode("plainText123");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should create refreshToken cookie with path /api/auth, HttpOnly, and Strict SameSite")
    void testCreateRefreshTokenCookie_PathAndSecurityAttributes() {
        org.springframework.http.ResponseCookie cookie = authService.createRefreshTokenCookie("sample-raw-token");

        assertNotNull(cookie);
        assertEquals("refreshToken", cookie.getName());
        assertEquals("sample-raw-token", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(java.time.Duration.ofDays(7), cookie.getMaxAge());
    }

    @Test
    @DisplayName("Should clear refreshToken cookie with matching path /api/auth and maxAge 0")
    void testClearRefreshTokenCookie_MatchingPathAndMaxAgeZero() {
        org.springframework.http.ResponseCookie cookie = authService.clearRefreshTokenCookie();

        assertNotNull(cookie);
        assertEquals("refreshToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertEquals(java.time.Duration.ZERO, cookie.getMaxAge());
    }

    @Test
    @DisplayName("Should create cookies with secure=false by default in development")
    void testCreateCookie_SecureFlagFalseByDefault() {
        org.springframework.http.ResponseCookie accessCookie = authService.createAccessTokenCookie("sample-access-token");
        org.springframework.http.ResponseCookie refreshCookie = authService.createRefreshTokenCookie("sample-refresh-token");

        assertFalse(accessCookie.isSecure());
        assertFalse(refreshCookie.isSecure());
    }

    @Test
    @DisplayName("Should create cookies with secure=true when cookie.secure is enabled for HTTPS production")
    void testCreateCookie_SecureFlagTrueWhenConfigured() {
        AuthService prodAuthService = new AuthService(userRepository, jwtRepository, passwordEncoder, TEST_SECRET, true);
        org.springframework.http.ResponseCookie accessCookie = prodAuthService.createAccessTokenCookie("sample-access-token");
        org.springframework.http.ResponseCookie refreshCookie = prodAuthService.createRefreshTokenCookie("sample-refresh-token");

        assertTrue(accessCookie.isSecure());
        assertTrue(refreshCookie.isSecure());
    }
}
