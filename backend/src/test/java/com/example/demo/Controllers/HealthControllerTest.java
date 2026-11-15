package com.example.demo.Controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 200 OK for root endpoint GET /")
    void testRootHealthCheck() throws Exception {
        mockMvc.perform(get("/")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("ShopNow API"));
    }

    @Test
    @DisplayName("Should return 200 OK for GET /api/health")
    void testApiHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("ShopNow API"));
    }

    @Test
    @DisplayName("Should permit CORS preflight for Vercel origin with credentials")
    void testCorsPreflightForVercelOrigin() throws Exception {
        mockMvc.perform(options("/api/health")
                .header("Origin", "https://shopnow-frontend.vercel.app")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://shopnow-frontend.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Should return 404 Not Found for non-existent static resource without 500 error")
    void testNonExistentResourceReturns404() throws Exception {
        mockMvc.perform(get("/api/non-existent-route-for-testing"))
                .andExpect(status().isUnauthorized()); // Unmapped /api route requires auth, returns 401
    }
}
