package com.example.demo.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Dto.CreateOrderRequest;
import com.example.demo.Dto.OrderItemRequest;
import com.example.demo.Entity.OrderItems;
import com.example.demo.Entity.Orders;
import com.example.demo.Entity.Products;
import com.example.demo.Entity.Status;
import com.example.demo.Entity.Users;
import com.example.demo.Exception.OutOfStockException;
import com.example.demo.Repository.OrderItemsRepository;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.ProductRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@ExtendWith(MockitoExtension.class)
class OrderTransactionServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private OrderItemsRepository orderItemsRepository;

    @Mock
    private RazorpayClient mockRazorpayClient;

    @Mock
    private OrderClient mockOrderClient;

    @InjectMocks
    private OrderTransactionService orderTransactionService;

    private Users testUser;

    @BeforeEach
    void setUp() {
        mockRazorpayClient.orders = mockOrderClient;

        testUser = new Users();
        testUser.setUser_id(1);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("Should decrement stock, check @Version, create Razorpay order, and pre-persist order items")
    void testExecuteOrderCreation_Success() throws RazorpayException {
        Products product1 = new Products();
        product1.setProduct_id(101);
        product1.setName("Laptop Bag");
        product1.setPrice(new BigDecimal("500.00"));
        product1.setStock(10);
        product1.setVersion(0L);

        Products product2 = new Products();
        product2.setProduct_id(102);
        product2.setName("Wireless Mouse");
        product2.setPrice(new BigDecimal("250.00"));
        product2.setStock(5);
        product2.setVersion(0L);

        when(productRepository.findAllById(List.of(101, 102)))
                .thenReturn(List.of(product1, product2));

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_test_123");

        ArgumentCaptor<JSONObject> payloadCaptor = ArgumentCaptor.forClass(JSONObject.class);
        when(mockOrderClient.create(payloadCaptor.capture())).thenReturn(mockOrder);

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(101, 2),
                new OrderItemRequest(102, 1)
        ));

        String orderId = orderTransactionService.executeOrderCreation(testUser, request, mockRazorpayClient);

        assertEquals("order_test_123", orderId);

        // Verify stock decremented
        assertEquals(8, product1.getStock());
        assertEquals(4, product2.getStock());

        // Verify productRepository.saveAll called with updated products
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Products>> productsCaptor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(productsCaptor.capture());
        assertEquals(2, productsCaptor.getValue().size());

        // Verify order saved with PENDING status
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepo).save(orderCaptor.capture());
        Orders savedOrder = orderCaptor.getValue();
        assertEquals("order_test_123", savedOrder.getId());
        assertEquals(Status.PENDING, savedOrder.getStatus());
        assertEquals(new BigDecimal("1250.00"), savedOrder.getTotal_amount());

        // Verify order items saved with snapshot prices
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItems>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemsRepository).saveAll(itemsCaptor.capture());
        assertEquals(2, itemsCaptor.getValue().size());
    }

    @Test
    @DisplayName("Should throw OutOfStockException when requested quantity exceeds available stock")
    void testExecuteOrderCreation_OutOfStock() {
        Products product = new Products();
        product.setProduct_id(101);
        product.setName("Limited Item");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(1);

        when(productRepository.findAllById(List.of(101))).thenReturn(List.of(product));

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(101, 5)
        ));

        OutOfStockException exception = assertThrows(OutOfStockException.class, () -> {
            orderTransactionService.executeOrderCreation(testUser, request, mockRazorpayClient);
        });

        assertEquals("Insufficient stock for: Limited Item (Available: 1)", exception.getMessage());
        assertEquals(1, product.getStock()); // Stock unchanged
        verify(productRepository, never()).saveAll(any());
        verify(orderRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when product not found")
    void testExecuteOrderCreation_ProductNotFound() {
        when(productRepository.findAllById(List.of(999))).thenReturn(List.of());

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(999, 1)
        ));

        assertThrows(IllegalArgumentException.class, () -> {
            orderTransactionService.executeOrderCreation(testUser, request, mockRazorpayClient);
        });
    }

    @Test
    @DisplayName("Should restore stock and set Status.CANCELLED on cancelAndRestoreStock")
    void testCancelAndRestoreStock_Success() {
        String orderId = "order_cancel_123";
        Orders pendingOrder = new Orders();
        pendingOrder.setId(orderId);
        pendingOrder.setStatus(Status.PENDING);

        Products product = new Products();
        product.setProduct_id(101);
        product.setStock(3);

        OrderItems orderItem = new OrderItems();
        orderItem.setOrders(pendingOrder);
        orderItem.setProducts(product);
        orderItem.setQuantity(2);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderItemsRepository.findByOrders_Id(orderId)).thenReturn(List.of(orderItem));

        orderTransactionService.cancelAndRestoreStock(orderId);

        assertEquals(Status.CANCELLED, pendingOrder.getStatus());
        assertEquals(5, product.getStock()); // Restored: 3 + 2 = 5
        verify(orderRepo).save(pendingOrder);
        verify(productRepository).saveAll(List.of(product));
    }

    @Test
    @DisplayName("Should do nothing if order is already cancelled or does not exist")
    void testCancelAndRestoreStock_IdempotentWhenAlreadyCancelled() {
        String orderId = "order_already_cancelled";
        Orders cancelledOrder = new Orders();
        cancelledOrder.setId(orderId);
        cancelledOrder.setStatus(Status.CANCELLED);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(cancelledOrder));

        orderTransactionService.cancelAndRestoreStock(orderId);

        verify(orderRepo, never()).save(any());
        verify(orderItemsRepository, never()).findByOrders_Id(any());
        verify(productRepository, never()).saveAll(any());
    }
}

