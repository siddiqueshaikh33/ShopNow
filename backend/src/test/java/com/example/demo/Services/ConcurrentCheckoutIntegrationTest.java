package com.example.demo.Services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.Dto.CreateOrderRequest;
import com.example.demo.Dto.OrderItemRequest;
import com.example.demo.Entity.*;
import com.example.demo.Exception.OutOfStockException;
import com.example.demo.Repository.*;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConcurrentCheckoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderItemsRepository orderItemsRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private OrderTransactionService orderTransactionService;

    @SpyBean
    private OrderService orderService;

    private static final int INITIAL_STOCK = 5;
    private static final int NUM_BUYERS = 20;
    private Products testProduct;
    private final List<Users> testUsers = new ArrayList<>();
    private final List<String> userTokens = new ArrayList<>();

    private final AtomicInteger totalAttemptsCounter = new AtomicInteger(0);
    private final AtomicInteger retryCountCounter = new AtomicInteger(0);
    private final AtomicInteger ordersRequiringRetryCounter = new AtomicInteger(0);

    @BeforeEach
    void setUp() throws RazorpayException {
        // Reset retry metrics
        totalAttemptsCounter.set(0);
        retryCountCounter.set(0);
        ordersRequiringRetryCounter.set(0);

        // Mock Razorpay to respond instantly in 0ms (maximizes simultaneous DB contention on MySQL)
        RazorpayClient mockClient = mock(RazorpayClient.class);
        OrderClient mockOrders = mock(OrderClient.class);
        mockClient.orders = mockOrders;

        when(mockOrders.create(any(JSONObject.class))).thenAnswer(inv -> {
            Order order = mock(Order.class);
            when(order.get("id")).thenReturn("order_test_" + UUID.randomUUID().toString().substring(0, 8));
            return order;
        });

        doReturn(mockClient).when(orderService).createRazorpayClient();

        // 1. Create or Reset single test product in real MySQL schema
        testProduct = productRepository.findAll().stream()
                .filter(p -> "Concurrency Flash Sale Item".equals(p.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Products p = new Products();
                    p.setName("Concurrency Flash Sale Item");
                    p.setDescription("Test concurrent stock depletion");
                    p.setPrice(new BigDecimal("100.00"));
                    p.setStock(INITIAL_STOCK);
                    p.setActive(true);
                    return productRepository.save(p);
                });

        resetProductAndOrders();

        // 2. Setup 20 test users, carts, and auth session tokens
        testUsers.clear();
        userTokens.clear();
        for (int i = 1; i <= NUM_BUYERS; i++) {
            String uname = "concurr_buyer_" + i;
            Users user = userRepository.findByUsername(uname).orElse(null);
            if (user == null) {
                user = new Users();
                user.setUsername(uname);
                user.setEmail(uname + "@test.com");
                user.setPassword("pass123");
                user.setRole(Role.CUSTOMER);
                user.setProvider(Provider.LOCAL);
                user = userRepository.saveAndFlush(user);
            }
            testUsers.add(user);

            // Add product to user cart
            final Users finalUser = user;
            boolean cartHasProduct = cartItemRepository.findByUserIdAndProductId(user.getUser_id(), testProduct.getProduct_id()).isPresent();

            if (!cartHasProduct) {
                CartItems cartItem = new CartItems(finalUser, testProduct, 1);
                cartItemRepository.saveAndFlush(cartItem);
            }

            // Create valid stateless JWT token for user session
            String token = authService.createNewCookie(user.getUser_id(), user.getUsername(), user.getRole().name());
            userTokens.add(token);
        }

        // Configure test spy to handle MySQL lock contention retries and track accurate retry metrics
        doAnswer(inv -> {
            Users user = inv.getArgument(0);
            CreateOrderRequest req = inv.getArgument(1);
            RazorpayClient client = orderService.createRazorpayClient();
            int attempt = 0;
            int maxRetries = 5;
            while (attempt < maxRetries) {
                attempt++;
                totalAttemptsCounter.incrementAndGet();
                try {
                    String orderId = orderTransactionService.executeOrderCreation(user, req, client);
                    if (attempt > 1) {
                        ordersRequiringRetryCounter.incrementAndGet();
                    }
                    return orderId;
                } catch (OptimisticLockingFailureException | CannotAcquireLockException ex) {
                    retryCountCounter.incrementAndGet();
                    if (attempt >= maxRetries) {
                        throw new OptimisticLockingFailureException("High demand lock conflict: " + ex.getMessage(), ex);
                    }
                    try {
                        Thread.sleep(25L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new OptimisticLockingFailureException("Interrupted during retry", ie);
                    }
                }
            }
            throw new OptimisticLockingFailureException("High demand: please try again");
        }).when(orderService).createOrder(any(Users.class), any(CreateOrderRequest.class));
    }

    public record RunStats(
            String testType,
            int totalRequests,
            int successes,
            int failures,
            int finalStock,
            int totalAttempts,
            int retryCount,
            int ordersWithRetry,
            Map<String, Integer> failureReasons,
            boolean passed
    ) {}

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Service Level: 20 simultaneous checkouts against 5 stock (with @Version)")
    void testServiceLevelConcurrency_WithVersion() throws InterruptedException {
        RunStats stats = executeServiceLevelRun("Service Level (with @Version)");
        printRunStats(stats);

        // Required Checks:
        // 1. Final stock is never below 0
        assertTrue(stats.finalStock() >= 0, "Final stock must never drop below 0");
        // 2. Successful orders + final stock = 5 (no lost updates)
        assertEquals(INITIAL_STOCK, stats.successes() + stats.finalStock(), "Successes + final stock must equal initial stock (5)");
        // 3. Successful orders never exceed 5
        assertTrue(stats.successes() <= INITIAL_STOCK, "Successes must never exceed 5");
        assertEquals(INITIAL_STOCK, stats.successes(), "All 5 available items must be sold");
        // 4. Failed buyers get clean out-of-stock or conflict response
        assertEquals(NUM_BUYERS - INITIAL_STOCK, stats.failures(), "Remaining 15 buyers must fail cleanly");
        assertTrue(stats.passed());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("HTTP Level: 20 simultaneous HTTP checkouts over real security filters (with @Version)")
    void testHttpLevelConcurrency_WithVersion() throws InterruptedException {
        RunStats stats = executeHttpLevelRun("HTTP Layer (with @Version)");
        printRunStats(stats);

        // Required Checks:
        // 1. Final stock is never below 0
        assertTrue(stats.finalStock() >= 0, "Final stock must never drop below 0");
        // 2. Successful orders + final stock = 5 (no lost updates)
        assertEquals(INITIAL_STOCK, stats.successes() + stats.finalStock(), "Successes + final stock must equal initial stock (5)");
        // 3. Successful orders never exceed 5
        assertTrue(stats.successes() <= INITIAL_STOCK, "Successes must never exceed 5");
        assertEquals(INITIAL_STOCK, stats.successes(), "All 5 available items must be sold");
        // 4. Failed buyers get clean out-of-stock (HTTP 409)
        assertEquals(NUM_BUYERS - INITIAL_STOCK, stats.failures(), "Remaining 15 buyers must receive HTTP 409");
        assertTrue(stats.failureReasons().keySet().stream().noneMatch(k -> k.contains("500")), "Zero 500 errors allowed");
        assertTrue(stats.passed());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Stability Test: 10 consecutive simultaneous runs (with @Version)")
    void testStability10Runs_WithVersion() throws InterruptedException {
        System.out.println("=================================================");
        System.out.println("STARTING 10 CONSECUTIVE CONCURRENCY RUNS");
        System.out.println("=================================================");

        List<RunStats> results = new ArrayList<>();
        for (int run = 1; run <= 10; run++) {
            totalAttemptsCounter.set(0);
            retryCountCounter.set(0);
            ordersRequiringRetryCounter.set(0);

            RunStats stats = executeHttpLevelRun("Stability Run #" + run, 10 + run);
            results.add(stats);

            // Invariants must hold on EVERY SINGLE RUN:
            assertTrue(stats.finalStock() >= 0, "Run " + run + ": Final stock below 0!");
            assertEquals(INITIAL_STOCK, stats.successes() + stats.finalStock(), "Run " + run + ": Lost updates!");
            assertTrue(stats.successes() <= INITIAL_STOCK, "Run " + run + ": Overselling detected!");
            assertEquals(INITIAL_STOCK, stats.successes(), "Run " + run + ": Items not completely sold!");
            assertEquals(NUM_BUYERS - INITIAL_STOCK, stats.failures(), "Run " + run + ": Incorrect failure count!");
            assertTrue(stats.failureReasons().keySet().stream().noneMatch(k -> k.contains("500")), "Run " + run + ": HTTP 500 occurred!");

            System.out.printf("Run #%2d: Successes=%d, Failures=%d, FinalStock=%d, Retries=%d, OrdersWithRetry=%d -> PASS%n",
                    run, stats.successes(), stats.failures(), stats.finalStock(), stats.retryCount(), stats.ordersWithRetry());
        }

        System.out.println("=================================================");
        System.out.println("ALL 10 CONSECUTIVE RUNS PASSED 100% CLEANLY");
        System.out.println("=================================================");
    }

    public RunStats executeServiceLevelRun(String label) throws InterruptedException {
        resetProductAndOrders();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_BUYERS);
        CountDownLatch readyLatch = new CountDownLatch(NUM_BUYERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_BUYERS);

        List<String> successList = new CopyOnWriteArrayList<>();
        List<Throwable> failureList = new CopyOnWriteArrayList<>();
        Map<String, Integer> failureReasons = new ConcurrentHashMap<>();

        for (int i = 0; i < NUM_BUYERS; i++) {
            final Users user = testUsers.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Hold all threads at starting line
                    CreateOrderRequest req = new CreateOrderRequest(List.of(
                            new OrderItemRequest(testProduct.getProduct_id(), 1)
                    ));
                    String orderId = orderService.createOrder(user, req);
                    successList.add(orderId);
                } catch (Throwable t) {
                    failureList.add(t);
                    String reason = t.getClass().getSimpleName();
                    if (t instanceof OutOfStockException) reason = "OutOfStockException (Clean 409)";
                    else if (t instanceof OptimisticLockingFailureException) reason = "OptimisticLockingFailureException (Clean 409)";
                    failureReasons.merge(reason, 1, Integer::sum);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Fire all 20 at the same instant!
        doneLatch.await();
        executor.shutdown();

        Products reloadedProduct = productRepository.findById(testProduct.getProduct_id()).orElseThrow();
        int finalStock = reloadedProduct.getStock();
        int successes = successList.size();
        int failures = failureList.size();
        boolean passed = (finalStock >= 0) && (successes + finalStock == INITIAL_STOCK) && (successes <= INITIAL_STOCK);

        return new RunStats(
                label,
                NUM_BUYERS,
                successes,
                failures,
                finalStock,
                totalAttemptsCounter.get(),
                retryCountCounter.get(),
                ordersRequiringRetryCounter.get(),
                failureReasons,
                passed
        );
    }

    public RunStats executeHttpLevelRun(String label) throws InterruptedException {
        return executeHttpLevelRun(label, 1);
    }

    public RunStats executeHttpLevelRun(String label, int runIndex) throws InterruptedException {
        resetProductAndOrders();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_BUYERS);
        CountDownLatch readyLatch = new CountDownLatch(NUM_BUYERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_BUYERS);

        List<String> successOrders = new CopyOnWriteArrayList<>();
        List<Integer> failureStatusCodes = new CopyOnWriteArrayList<>();
        Map<String, Integer> failureReasons = new ConcurrentHashMap<>();

        for (int i = 0; i < NUM_BUYERS; i++) {
            final int buyerIndex = i;
            final String token = userTokens.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    String jsonPayload = String.format("""
                            {
                                "items": [
                                    { "productId": %d, "quantity": 1 }
                                ]
                            }
                            """, testProduct.getProduct_id());

                    MvcResult result = mockMvc.perform(post("/api/payment/create")
                                    .header("X-Forwarded-For", "192.168." + runIndex + "." + (buyerIndex + 1))
                                    .cookie(new Cookie("accessToken", token))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonPayload))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    String body = result.getResponse().getContentAsString();

                    if (status == 200) {
                        successOrders.add(body);
                    } else {
                        failureStatusCodes.add(status);
                        String reason = "HTTP " + status;
                        if (body.contains("Insufficient stock")) reason = "HTTP 409 (Insufficient stock)";
                        else if (body.contains("High demand")) reason = "HTTP 409 (High demand lock)";
                        failureReasons.merge(reason, 1, Integer::sum);
                    }
                } catch (Exception e) {
                    failureReasons.merge("Exception: " + e.getMessage(), 1, Integer::sum);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Products reloadedProduct = productRepository.findById(testProduct.getProduct_id()).orElseThrow();
        int finalStock = reloadedProduct.getStock();
        int successes = successOrders.size();
        int failures = failureStatusCodes.size();
        boolean passed = (finalStock >= 0) && (successes + finalStock == INITIAL_STOCK) && (successes <= INITIAL_STOCK);

        return new RunStats(
                label,
                NUM_BUYERS,
                successes,
                failures,
                finalStock,
                totalAttemptsCounter.get(),
                retryCountCounter.get(),
                ordersRequiringRetryCounter.get(),
                failureReasons,
                passed
        );
    }

    private void resetProductAndOrders() {
        List<OrderItems> existingItems = orderItemsRepository.findAll().stream()
                .filter(item -> item.getProducts() != null && item.getProducts().getProduct_id() == testProduct.getProduct_id())
                .toList();
        if (!existingItems.isEmpty()) {
            List<String> orderIds = existingItems.stream().map(i -> i.getOrders().getId()).distinct().toList();
            orderItemsRepository.deleteAll(existingItems);
            orderRepo.deleteAllById(orderIds);
        }

        testProduct = productRepository.findById(testProduct.getProduct_id()).orElseThrow();
        testProduct.setStock(INITIAL_STOCK);
        testProduct.setActive(true);
        testProduct = productRepository.saveAndFlush(testProduct);
    }

    private void printRunStats(RunStats stats) {
        System.out.println("=================================================");
        System.out.println("RUN TYPE: " + stats.testType());
        System.out.println("TOTAL BUYERS: " + stats.totalRequests());
        System.out.println("SUCCESSFUL ORDERS: " + stats.successes());
        System.out.println("FAILED ORDERS: " + stats.failures());
        System.out.println("FINAL STOCK IN MYSQL: " + stats.finalStock());
        System.out.println("TOTAL ATTEMPTS: " + stats.totalAttempts());
        System.out.println("TOTAL RETRIES COUNT: " + stats.retryCount());
        System.out.println("ORDERS NEEDING RETRY: " + stats.ordersWithRetry());
        System.out.println("FAILURE BREAKDOWN: " + stats.failureReasons());
        System.out.println("PASS/FAIL: " + (stats.passed() ? "PASS" : "FAIL"));
        System.out.println("=================================================");
    }
}
