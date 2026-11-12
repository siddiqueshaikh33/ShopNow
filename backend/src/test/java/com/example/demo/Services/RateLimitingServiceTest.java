package com.example.demo.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        // 5 strict IP / min, 100 general IP / min, 5 failed logins / 15 min
        rateLimitingService = new RateLimitingService(5, 1, 100, 1, 5, 15);
    }

    @Test
    @DisplayName("Strict IP: Should allow up to 5 requests and block the 6th request")
    void testStrictIpLimit_BlocksOn6thRequest() {
        String testIp = "192.168.1.100";

        for (int i = 1; i <= 5; i++) {
            ConsumptionProbe probe = rateLimitingService.probeConsumeStrictIp(testIp);
            assertTrue(probe.isConsumed(), "Request " + i + " should be allowed");
            assertEquals(5 - i, probe.getRemainingTokens());
        }

        ConsumptionProbe blockedProbe = rateLimitingService.probeConsumeStrictIp(testIp);
        assertFalse(blockedProbe.isConsumed(), "6th request must be blocked");
        assertEquals(0, blockedProbe.getRemainingTokens());
        assertTrue(blockedProbe.getNanosToWaitForRefill() > 0);
    }

    @Test
    @DisplayName("General IP: Should allow up to 100 requests for general browsing")
    void testGeneralIpLimit_AllowsHighVolumeBrowsing() {
        String testIp = "192.168.1.200";

        for (int i = 1; i <= 100; i++) {
            ConsumptionProbe probe = rateLimitingService.probeConsumeGeneralIp(testIp);
            assertTrue(probe.isConsumed(), "Browsing request " + i + " should be allowed");
        }

        ConsumptionProbe blocked = rateLimitingService.probeConsumeGeneralIp(testIp);
        assertFalse(blocked.isConsumed(), "101st request should be blocked");
    }

    @Test
    @DisplayName("IP Isolation: Attacker blocked on IP A does not affect legitimate user on IP B")
    void testIpIsolation_SeparateBucketsPerIp() {
        String attackerIp = "10.0.0.99";
        String legitimateUserIp = "10.0.0.1";

        for (int i = 0; i < 5; i++) {
            rateLimitingService.probeConsumeStrictIp(attackerIp);
        }
        assertFalse(rateLimitingService.probeConsumeStrictIp(attackerIp).isConsumed(), "Attacker must be blocked");

        ConsumptionProbe legitProbe = rateLimitingService.probeConsumeStrictIp(legitimateUserIp);
        assertTrue(legitProbe.isConsumed(), "Legitimate user must not be blocked");
        assertEquals(4, legitProbe.getRemainingTokens());
    }

    @Test
    @DisplayName("Username Limiting: 5 failed logins block the account across different rotating IPs")
    void testUsernameRateLimit_BlocksRotatingIpAttackOnSameAccount() {
        String targetUsername = "victim_admin";

        // Account initially not blocked
        assertNull(rateLimitingService.checkUsernameAllowed(targetUsername));

        // Attacker rotates 5 different IPs against this single username
        for (int i = 1; i <= 5; i++) {
            ConsumptionProbe probe = rateLimitingService.recordFailedLogin(targetUsername);
            assertTrue(probe.isConsumed());
        }

        // Account is now blocked regardless of who is calling!
        EstimationProbe blockedProbe = rateLimitingService.checkUsernameAllowed(targetUsername);
        assertNotNull(blockedProbe, "Account should be blocked after 5 failed attempts");
        assertFalse(blockedProbe.canBeConsumed());
        assertTrue(blockedProbe.getNanosToWaitForRefill() > 0);
    }

    @Test
    @DisplayName("Username Reset: Successful login clears previous failed attempts penalty")
    void testUsernameReset_ClearsLockoutOnCorrectPassword() {
        String username = "customer_alex";

        // 3 failed login attempts
        rateLimitingService.recordFailedLogin(username);
        rateLimitingService.recordFailedLogin(username);
        rateLimitingService.recordFailedLogin(username);

        // Alex enters correct password -> reset
        rateLimitingService.resetUsernameLimit(username);

        // Account is fresh again (not blocked)
        assertNull(rateLimitingService.checkUsernameAllowed(username));
    }
}
