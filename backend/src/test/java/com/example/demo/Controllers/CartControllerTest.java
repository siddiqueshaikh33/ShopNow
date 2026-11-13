package com.example.demo.Controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.Services.AuthService;
import com.example.demo.Services.CartService;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @MockBean
    private CartService cartService;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = authService.createNewCookie(42, "legitUser", "CUSTOMER");
    }

    @Test
    @DisplayName("POST /api/cart/add without token returns 401 Unauthorized")
    void addCart_Unauthorized() throws Exception {
        String payload = """
                {
                    "username": "legitUser",
                    "product_id": 10,
                    "quantity": 2
                }
                """;

        mockMvc.perform(post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/cart/add binds strictly to authenticated user ID even if payload has another username")
    void addCart_BindsToAuthenticatedUser_IgnoringSpoofedUsername() throws Exception {
        // Attacker sends username "victimUser" in payload, but token belongs to user 42 ("legitUser")
        String payload = """
                {
                    "username": "victimUser",
                    "product_id": 10,
                    "quantity": 2
                }
                """;

        when(cartService.addOrUpdateCartItem(eq(42), eq(10), eq(2))).thenReturn(5);

        mockMvc.perform(post("/api/cart/add")
                .cookie(new Cookie("token", validToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.count").value(5));

        // Verify that service was invoked with authenticated user ID 42, NOT with victimUser
        verify(cartService).addOrUpdateCartItem(eq(42), eq(10), eq(2));
    }

    @Test
    @DisplayName("GET /api/cart/count without token returns 401 Unauthorized")
    void getCartCount_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/cart/count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/cart/count with token returns authenticated user cart count without query params")
    void getCartCount_Success() throws Exception {
        when(cartService.countCartItem(42)).thenReturn(7);

        mockMvc.perform(get("/api/cart/count")
                .cookie(new Cookie("token", validToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));

        verify(cartService).countCartItem(42);
    }

    @Test
    @DisplayName("PUT /api/cart/update without token returns 401 Unauthorized")
    void updateCart_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/cart/update")
                .param("productId", "10")
                .param("quantity", "3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/cart/delete without token returns 401 Unauthorized")
    void deleteCart_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/cart/delete")
                .param("productId", "10"))
                .andExpect(status().isUnauthorized());
    }
}

