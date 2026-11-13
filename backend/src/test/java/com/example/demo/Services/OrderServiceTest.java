package com.example.demo.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.example.demo.Dto.CreateOrderRequest;
import com.example.demo.Dto.OrderItemRequest;
import com.example.demo.Entity.OrderItems;
import com.example.demo.Entity.Orders;
import com.example.demo.Entity.Products;
import com.example.demo.Entity.Status;
import com.example.demo.Entity.Users;
import com.example.demo.Exception.OutOfStockException;
import com.example.demo.Repository.CartItemRepository;
import com.example.demo.Repository.OrderItemsRepository;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.ProductRepository;
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
    private OrderTransactionService orderTransactionService;

    @Mock
    private RazorpayClient mockRazorpayClient;

    @Mock
    private OrderClient mockOrderClient;

    private OrderService orderService;
    private Users testUser;
    private boolean mockSignatureValid = true;

    @BeforeEach
    void setUp() {
        mockSignatureValid = true;
        mockRazorpayClient.orders = mockOrderClient;

        orderService = new OrderService(orderItemsRepository, orderRepo, cartItemRepository, productRepository, orderTransactionService) {
            @Override
            protected RazorpayClient createRazorpayClient() {
                return mockRazorpayClient;
            }

            @Override
            protected boolean verifyPaymentSignature(JSONObject payload, String secret) {
                return mockSignatureValid;
            }
        };

        testUser = new Users();
        testUser.setUser_id(1);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("Should delegate createOrder to OrderTransactionService")
    void testCreateOrder_Success() throws RazorpayException {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(101, 2)
        ));

        when(orderTransactionService.executeOrderCreation(eq(testUser), eq(request), any()))
                .thenReturn("order_test_999");

        String orderId = orderService.createOrder(testUser, request);

        assertEquals("order_test_999", orderId);
        verify(orderTransactionService, times(1)).executeOrderCreation(eq(testUser), eq(request), any());
    }

    @Test
    @DisplayName("Should retry on OptimisticLockingFailureException and succeed on subsequent attempt")
    void testCreateOrder_RetryOnOptimisticLockingFailure_Succeeds() throws RazorpayException {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(101, 2)
        ));

        when(orderTransactionService.executeOrderCreation(eq(testUser), eq(request), any()))
                .thenThrow(new OptimisticLockingFailureException("Version conflict on product"))
                .thenReturn("order_test_retry_success");

        String orderId = orderService.createOrder(testUser, request);

        assertEquals("order_test_retry_success", orderId);
        verify(orderTransactionService, times(2)).executeOrderCreation(eq(testUser), eq(request), any());
    }

    @Test
    @DisplayName("Should exhaust 3 retries and rethrow OptimisticLockingFailureException")
    void testCreateOrder_MaxRetriesExceeded_ThrowsException() throws RazorpayException {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(101, 2)
        ));

        when(orderTransactionService.executeOrderCreation(any(), any(), any()))
                .thenThrow(new OptimisticLockingFailureException("Persistent version conflict"));

        assertThrows(OptimisticLockingFailureException.class, () -> {
            orderService.createOrder(testUser, request);
        });

        verify(orderTransactionService, times(3)).executeOrderCreation(any(), any(), any());
    }

    @Test
    @DisplayName("Should not retry when OutOfStockException is thrown")
    void testCreateOrder_OutOfStock_DoesNotRetry() throws RazorpayException {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(101, 5)
        ));

        when(orderTransactionService.executeOrderCreation(any(), any(), any()))
                .thenThrow(new OutOfStockException("Insufficient stock for: Laptop Bag (Available: 1)"));

        assertThrows(OutOfStockException.class, () -> {
            orderService.createOrder(testUser, request);
        });

        verify(orderTransactionService, times(1)).executeOrderCreation(any(), any(), any());
    }

    @Test
    @DisplayName("Should cancel order and restore stock when user owns the order")
    void testCancelOrder_Success() {
        String orderId = "order_test_123";
        Orders order = new Orders();
        order.setId(orderId);
        order.setUser(testUser);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        orderService.cancelOrder(orderId, testUser);

        verify(orderTransactionService).cancelAndRestoreStock(orderId);
    }

    @Test
    @DisplayName("Should throw RuntimeException if unauthorized user attempts to cancel order")
    void testCancelOrder_Unauthorized() {
        String orderId = "order_test_123";
        Users otherUser = new Users();
        otherUser.setUser_id(999);

        Orders order = new Orders();
        order.setId(orderId);
        order.setUser(otherUser);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.cancelOrder(orderId, testUser);
        });

        assertEquals("Unauthorized order access", exception.getMessage());
        verify(orderTransactionService, never()).cancelAndRestoreStock(any());
    }

    @Test
    @DisplayName("Should auto-cancel expired pending orders older than 15 minutes")
    void testCancelExpiredPendingOrders() {
        Orders order1 = new Orders();
        order1.setId("order_1");
        Orders order2 = new Orders();
        order2.setId("order_2");

        when(orderRepo.findPendingOrdersBefore(eq(Status.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(order1, order2));

        orderService.cancelExpiredPendingOrders();

        verify(orderTransactionService).cancelAndRestoreStock("order_1");
        verify(orderTransactionService).cancelAndRestoreStock("order_2");
    }

    @Test
    @DisplayName("Should update status to SUCCESS and delete only purchased cart items in verifyOrder")
    void testVerifyOrder_Success_UpdatesStatusAndDeletesOnlyPurchasedCartItems() {
        String orderId = "order_test_999";
        Orders existingOrder = new Orders();
        existingOrder.setId(orderId);
        existingOrder.setUser(testUser);
        existingOrder.setStatus(Status.PENDING);
        existingOrder.setTotal_amount(new BigDecimal("749.50"));

        Products product1 = new Products();
        product1.setProduct_id(101);
        OrderItems item1 = new OrderItems();
        item1.setOrders(existingOrder);
        item1.setProducts(product1);

        Products product2 = new Products();
        product2.setProduct_id(102);
        OrderItems item2 = new OrderItems();
        item2.setOrders(existingOrder);
        item2.setProducts(product2);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderItemsRepository.findByOrders_Id(orderId)).thenReturn(List.of(item1, item2));

        boolean result = orderService.verifyOrder("pay_123", orderId, "sig_valid", testUser);

        assertTrue(result);
        assertEquals(Status.SUCCESS, existingOrder.getStatus());
        verify(orderRepo).save(existingOrder);
        verify(cartItemRepository).deleteCartItemsByUserIdAndProductIds(1, List.of(101, 102));
        verify(cartItemRepository, never()).fetchAllCartItems(any());
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Should return false when payment signature is invalid")
    void testVerifyOrder_InvalidSignature_ReturnsFalse() {
        mockSignatureValid = false;

        boolean result = orderService.verifyOrder("pay_fake", "order_test_999", "sig_bad", testUser);

        assertFalse(result);
        verify(orderRepo, never()).save(any());
        verify(cartItemRepository, never()).deleteCartItemsByUserIdAndProductIds(any(), any());
    }

    @Test
    @DisplayName("Should return false when order is CANCELLED to prevent zombie order double-selling")
    void testVerifyOrder_CancelledOrder_ReturnsFalseAndDoesNotMutateOrder() {
        String orderId = "order_test_cancelled";
        Orders cancelledOrder = new Orders();
        cancelledOrder.setId(orderId);
        cancelledOrder.setUser(testUser);
        cancelledOrder.setStatus(Status.CANCELLED);
        cancelledOrder.setTotal_amount(new BigDecimal("100.00"));

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(cancelledOrder));

        boolean result = orderService.verifyOrder("pay_123", orderId, "sig_valid", testUser);

        assertFalse(result);
        assertEquals(Status.CANCELLED, cancelledOrder.getStatus());
        verify(orderRepo, never()).save(cancelledOrder);
        verify(cartItemRepository, never()).deleteCartItemsByUserIdAndProductIds(any(), any());
    }

    @Test
    @DisplayName("Should return true idempotently when order is already marked SUCCESS")
    void testVerifyOrder_AlreadySuccess_ReturnsTrueIdempotently() {
        String orderId = "order_test_already_success";
        Orders successOrder = new Orders();
        successOrder.setId(orderId);
        successOrder.setUser(testUser);
        successOrder.setStatus(Status.SUCCESS);
        successOrder.setTotal_amount(new BigDecimal("100.00"));

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(successOrder));

        boolean result = orderService.verifyOrder("pay_123", orderId, "sig_valid", testUser);

        assertTrue(result);
        assertEquals(Status.SUCCESS, successOrder.getStatus());
        verify(orderRepo, never()).save(successOrder);
    }

    @Test
    @DisplayName("Should delete child order items before deleting pending parent orders older than 7 days")
    void testHandleOrderPendingDeletion_DeletesItemsThenOrders() {
        when(orderItemsRepository.deleteByOrdersStatusAndCreatedAtBefore(any(), any())).thenReturn(5);
        when(orderRepo.deletePendingStatus(any(), any())).thenReturn(2);

        orderService.handleOrderPendingDeletion();

        verify(orderItemsRepository).deleteByOrdersStatusAndCreatedAtBefore(eq(Status.PENDING), any(LocalDateTime.class));
        verify(orderRepo).deletePendingStatus(eq(Status.PENDING), any(LocalDateTime.class));
    }
}
