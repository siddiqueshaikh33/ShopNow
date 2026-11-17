package com.example.demo.Controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.Entity.Provider;
import com.example.demo.Entity.RefreshToken;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.RefreshTokenRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Services.AuthService;
import com.example.demo.Services.RefreshTokenService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RefreshTokenReuseDetectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    private static final String TEST_USERNAME = "reuse_test_buyer";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String CLIENT_IP = "192.168.88.10";

    private Users testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();

        testUser = userRepository.findByUsername(TEST_USERNAME).orElseGet(() -> {
            Users u = new Users();
            u.setUsername(TEST_USERNAME);
            u.setEmail(TEST_USERNAME + "@example.com");
            u.setPassword(passwordEncoder.encode(TEST_PASSWORD));
            u.setRole(Role.CUSTOMER);
            u.setProvider(Provider.LOCAL);
            u.setCreated_at(LocalDateTime.now());
            u.setUpdated_at(LocalDateTime.now());
            return userRepository.saveAndFlush(u);
        });

        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser = userRepository.saveAndFlush(testUser);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-End Proof: Refresh token rotation, reuse theft detection, DB hashing, and cookie security")
    void testRefreshTokenReuseTheftDetection_EndToEnd() throws Exception {
        System.out.println("=================================================");
        System.out.println("STARTING REFRESH TOKEN REUSE DETECTION PROOF");
        System.out.println("=================================================");

        // STEP 1: Log in and save refresh token A
        String loginPayload = String.format("""
                {
                    "username": "%s",
                    "password": "%s"
                }
                """, TEST_USERNAME, TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andReturn();

        assertEquals(200, loginResult.getResponse().getStatus(), "Step 1: Login must succeed with HTTP 200");

        String rawTokenA = extractCookieFromResponse(loginResult, "refreshToken");
        String accessTokenA = extractCookieFromResponse(loginResult, "accessToken");
        List<String> setCookieHeaders = loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);

        assertNotNull(rawTokenA, "Step 1: Refresh token A must be returned in Set-Cookie header");
        assertNotNull(accessTokenA, "Step 1: Access token must be returned in Set-Cookie header");
        System.out.println("Step 1 PASSED: Successfully logged in. Saved Token A = " + maskToken(rawTokenA));

        // STEP 2: Call refresh with A. Save the new token B
        MvcResult refreshResultB = mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .cookie(new Cookie("refreshToken", rawTokenA)))
                .andReturn();

        assertEquals(200, refreshResultB.getResponse().getStatus(), "Step 2: Refresh with Token A must succeed with HTTP 200");

        String rawTokenB = extractCookieFromResponse(refreshResultB, "refreshToken");
        String accessTokenB = extractCookieFromResponse(refreshResultB, "accessToken");

        assertNotNull(rawTokenB, "Step 2: Rotated refresh token B must be returned");
        assertNotEquals(rawTokenA, rawTokenB, "Step 2: Token B must be newly generated and different from Token A");
        System.out.println("Step 2 PASSED: Successfully rotated Token A -> Token B = " + maskToken(rawTokenB));

        // STEP 3: Call refresh with A again (REUSE ATTACK). Report status code and response
        MvcResult reuseResult = mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .cookie(new Cookie("refreshToken", rawTokenA)))
                .andReturn();

        int reuseStatus = reuseResult.getResponse().getStatus();
        String reuseBody = reuseResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertEquals(401, reuseStatus, "Step 3: Reusing revoked Token A must be rejected with HTTP 401");
        assertTrue(reuseBody.contains("Compromised session detected"), "Step 3: Response must indicate theft detection");
        System.out.printf("Step 3 PASSED: Reuse attack rejected. HTTP Status = %d, Response = %s%n", reuseStatus, reuseBody);

        // STEP 4: Call refresh with B. Report whether it is still accepted or whole session was revoked
        MvcResult refreshResultAfterTheft = mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .cookie(new Cookie("refreshToken", rawTokenB)))
                .andReturn();

        int tokenBStatus = refreshResultAfterTheft.getResponse().getStatus();
        String tokenBBody = refreshResultAfterTheft.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertEquals(401, tokenBStatus, "Step 4: Token B must also be rejected with HTTP 401 because the whole session was revoked");
        System.out.printf("Step 4 PASSED: Token B rejected (Whole session revoked). HTTP Status = %d, Response = %s%n", tokenBStatus, tokenBBody);

        // STEP 5: Check database: confirm tokens are stored as SHA-256 hashes, not raw values
        String expectedHashA = RefreshTokenService.hashToken(rawTokenA);
        String expectedHashB = RefreshTokenService.hashToken(rawTokenB);

        Optional<RefreshToken> tokenAInDb = refreshTokenRepository.findByTokenHash(expectedHashA);
        Optional<RefreshToken> tokenBInDb = refreshTokenRepository.findByTokenHash(expectedHashB);

        assertTrue(tokenAInDb.isPresent(), "Step 5: Token A must be present in DB by its SHA-256 hash");
        assertTrue(tokenBInDb.isPresent(), "Step 5: Token B must be present in DB by its SHA-256 hash");

        RefreshToken dbTokenA = tokenAInDb.get();
        RefreshToken dbTokenB = tokenBInDb.get();

        // Must NOT contain the raw UUID token
        assertNotEquals(rawTokenA, dbTokenA.getTokenHash(), "Database must NEVER store raw Token A");
        assertNotEquals(rawTokenB, dbTokenB.getTokenHash(), "Database must NEVER store raw Token B");

        // Must be exact 64-hex SHA-256 digest
        assertEquals(64, dbTokenA.getTokenHash().length(), "Token A hash must be a 64-character hex string (SHA-256)");
        assertEquals(64, dbTokenB.getTokenHash().length(), "Token B hash must be a 64-character hex string (SHA-256)");

        // Both tokens must be marked revoked after theft detection
        assertTrue(dbTokenA.isRevoked(), "Token A must be marked revoked in the database");
        assertTrue(dbTokenB.isRevoked(), "Token B must be marked revoked in the database (session terminated)");

        System.out.println("Step 5 PASSED: Both tokens stored strictly as SHA-256 hashes (length=64 hex). All revoked.");

        // STEP 6: Check access token expiration (exp - iat = 900s) and refresh cookie security attributes
        Claims claims = authService.extractAllClaims(accessTokenA);
        long iatSeconds = claims.getIssuedAt().getTime() / 1000;
        long expSeconds = claims.getExpiration().getTime() / 1000;
        long lifetimeSeconds = expSeconds - iatSeconds;

        assertEquals(900, lifetimeSeconds, "Step 6: Access token lifetime (exp - iat) must be exactly 900 seconds (15 minutes)");

        String refreshSetCookie = setCookieHeaders.stream()
                .filter(c -> c.startsWith("refreshToken="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("refreshToken Set-Cookie header missing"));

        assertTrue(refreshSetCookie.toLowerCase().contains("httponly"), "Step 6: Refresh cookie must include HttpOnly");
        assertTrue(refreshSetCookie.toLowerCase().contains("samesite"), "Step 6: Refresh cookie must include SameSite");
        assertTrue(refreshSetCookie.contains("Path=/api/auth"), "Step 6: Refresh cookie must have Path=/api/auth");

        System.out.println("Step 6 PASSED: Access token exp - iat = 900s (15 min). Refresh cookie: " + refreshSetCookie);

        System.out.println("=================================================");
        System.out.println("ALL 6 VERIFICATION CHECKS PASSED 100% CLEANLY");
        System.out.println("=================================================");
    }

    private String extractCookieFromResponse(MvcResult result, String cookieName) {
        List<String> headers = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        for (String header : headers) {
            if (header.startsWith(cookieName + "=")) {
                int start = cookieName.length() + 1;
                int end = header.indexOf(';', start);
                return end == -1 ? header.substring(start) : header.substring(start, end);
            }
        }
        return null;
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) return token;
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
