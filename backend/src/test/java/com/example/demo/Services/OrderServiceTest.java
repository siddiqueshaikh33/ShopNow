package com.example.demo.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Dto.CreateOrderRequest;
import com.example.demo.Dto.OrderItemRequest;
import com.example.demo.Entity.Orders;
import com.example.demo.Entity.Products;
import com.example.demo.Entity.Status;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.CartItemRepository;
import com.example.demo.Repository.OrderItemsRepository;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.ProductRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderItemsRepository orderItemsRepository;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RazorpayClient mockRazorpayClient;

    @Mock
    private OrderClient mockOrderClient;

    private OrderService orderService;
    private Users testUser;

    @BeforeEach
    void setUp() {
        mockRazorpayClient.orders = mockOrderClient;

        orderService = new OrderService(orderItemsRepository, orderRepo, cartItemRepository, productRepository) {
            @Override
            protected RazorpayClient createRazorpayClient() {
                return mockRazorpayClient;
            }
        };

        testUser = new Users();
        testUser.setUser_id(1);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("Should calculate total price strictly on server from DB products")
    void testCreateOrder_ServerSideCalculation() throws RazorpayException {
        // Arrange
        Products product1 = new Products();
        product1.setProduct_id(101);
        product1.setName("Laptop Bag");
        product1.setPrice(new BigDecimal("499.00"));

        Products product2 = new Products();
        product2.setProduct_id(102);
        product2.setName("Wireless Mouse");
        product2.setPrice(new BigDecimal("250.50"));

        when(productRepository.findAllById(List.of(101, 102)))
                .thenReturn(List.of(product1, product2));

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_test_999");

        ArgumentCaptor<JSONObject> payloadCaptor = ArgumentCaptor.forClass(JSONObject.class);
        when(mockOrderClient.create(payloadCaptor.capture())).thenReturn(mockOrder);

        // Act: 2 Laptop Bags (499 * 2 = 998) + 1 Mouse (250.50 * 1 = 250.50) = 1248.50
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(101, 2),
                new OrderItemRequest(102, 1)
        ));

        String orderId = orderService.createOrder(testUser, request);

        // Assert
        assertEquals("order_test_999", orderId);

        // Verify Razorpay payload amount: 1248.50 * 100 = 124850 paise
        JSONObject capturedPayload = payloadCaptor.getValue();
        assertEquals(124850, capturedPayload.getInt("amount"));
        assertEquals("INR", capturedPayload.getString("currency"));

        // Verify Order entity saved to database
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepo).save(orderCaptor.capture());
        Orders savedOrder = orderCaptor.getValue();

        assertEquals("order_test_999", savedOrder.getId());
        assertEquals(testUser, savedOrder.getUser());
        assertEquals(new BigDecimal("1248.50"), savedOrder.getTotal_amount());
        assertEquals(Status.PENDING, savedOrder.getStatus());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if product ID is not found in database")
    void testCreateOrder_ProductNotFound() {
        when(productRepository.findAllById(List.of(999))).thenReturn(List.of());

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(999, 1)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(testUser, request);
        });

        assertEquals("Product not found with ID: 999", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if request is empty")
    void testCreateOrder_EmptyRequest() {
        CreateOrderRequest request = new CreateOrderRequest(List.of());

        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(testUser, request);
        });
    }
}

