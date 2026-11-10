package com.example.demo.Controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.Dto.ProductResponseDto;
import com.example.demo.Exception.OutOfStockException;
import com.example.demo.Services.AuthService;
import com.example.demo.Services.OrderService;
import com.example.demo.Services.ProductService;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class ProductAndOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderService orderService;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = authService.createNewCookie(1, "integrationUser", "CUSTOMER");
    }

    @Test
    @DisplayName("GET /api/products without token should return 401 Unauthorized")
    void testGetProducts_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/products with valid token should return 200 OK and product list")
    void testGetProducts_Success() throws Exception {
        ProductResponseDto dto = new ProductResponseDto(
                101, "Running Shoes", "High performance shoes", new BigDecimal("79.99"), 25, "https://example.com/shoes.jpg"
        );
        when(productService.getProductDtosByCategoryId(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/products")
                .cookie(new Cookie("token", validToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("integrationUser"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.product[0].product_id").value(101))
                .andExpect(jsonPath("$.product[0].name").value("Running Shoes"))
                .andExpect(jsonPath("$.product[0].price").value(79.99));
    }

    @Test
    @DisplayName("POST /api/payment/create without token should return 401 Unauthorized")
    void testCreatePayment_Unauthorized() throws Exception {
        String jsonPayload = """
                {
                    "items": [
                        { "productId": 101, "quantity": 1 }
                    ]
                }
                """;

        mockMvc.perform(post("/api/payment/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/payment/create with valid token should return 200 OK and Razorpay order ID")
    void testCreatePayment_Success() throws Exception {
        when(orderService.createOrder(any(), any())).thenReturn("order_rzp_mock_12345");

        String jsonPayload = """
                {
                    "items": [
                        { "productId": 101, "quantity": 2 }
                    ]
                }
                """;

        mockMvc.perform(post("/api/payment/create")
                .cookie(new Cookie("token", validToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("order_rzp_mock_12345"));
    }

    @Test
    @DisplayName("POST /api/payment/create when out of stock should return 409 Conflict")
    void testCreatePayment_OutOfStock_Returns409() throws Exception {
        when(orderService.createOrder(any(), any()))
                .thenThrow(new OutOfStockException("Insufficient stock for product ID 101"));

        String jsonPayload = """
                {
                    "items": [
                        { "productId": 101, "quantity": 999 }
                    ]
                }
                """;

        mockMvc.perform(post("/api/payment/create")
                .cookie(new Cookie("token", validToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isConflict());
    }
}

