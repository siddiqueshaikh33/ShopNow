package com.example.demo.Controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.Services.AuthService;
import com.example.demo.Services.ProductService;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("End-to-End: Strict IP limit blocks 6th consecutive login attempt with HTTP 429")
    void testStrictIpRateLimit_BlocksOn6thAttempt() throws Exception {
        String testIp = "198.51.100.1";
        String loginJson = "{\"username\":\"testuser\",\"password\":\"wrongpass\"}";

        when(authService.authenicate(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // 5 requests allowed to reach the controller (returns 401 from controller)
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson)
                    .with(request -> {
                        request.setRemoteAddr(testIp);
                        return request;
                    }))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("X-Rate-Limit-Remaining"));
        }

        // 6th request blocked at the filter level before reaching the controller
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson)
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-Rate-Limit-Remaining", "0"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    @DisplayName("End-to-End: General IP limit allows browsing and returns X-Rate-Limit-Remaining header")
    void testGeneralIpRateLimit_AllowsBrowsing() throws Exception {
        String browsingIp = "198.51.100.2";

        mockMvc.perform(get("/api/products")
                .with(request -> {
                    request.setRemoteAddr(browsingIp);
                    return request;
                }))
                .andExpect(header().exists("X-Rate-Limit-Remaining"));
    }

    @Test
    @DisplayName("End-to-End: Username rate limiting blocks rotating-IP brute-force attack across multiple IPs")
    void testUsernameRateLimit_BlocksRotatingIpAttack() throws Exception {
        String targetUsername = "corporate_admin";
        String loginJson = "{\"username\":\"" + targetUsername + "\",\"password\":\"wrong\"}";

        when(authService.authenicate(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // Attacker uses 5 DIFFERENT IPs to attack this single username
        for (int i = 1; i <= 5; i++) {
            String ip = "100.64.0." + i; // Unique IP each time!
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson)
                    .with(request -> {
                        request.setRemoteAddr(ip);
                        return request;
                    }))
                    .andExpect(status().isUnauthorized());
        }

        // 6th request from yet another fresh IP (100.64.0.99) is blocked because USERNAME is locked!
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson)
                .with(request -> {
                    request.setRemoteAddr("100.64.0.99");
                    return request;
                }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }
}

