package com.example.demo.Services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class OrderTransactionService {

    private final ProductRepository productRepository;
    private final OrderRepo orderRepo;
    private final OrderItemsRepository orderItemsRepository;

    public OrderTransactionService(ProductRepository productRepository, OrderRepo orderRepo,
            OrderItemsRepository orderItemsRepository) {
        this.productRepository = productRepository;
        this.orderRepo = orderRepo;
        this.orderItemsRepository = orderItemsRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String executeOrderCreation(Users user, CreateOrderRequest request, RazorpayClient razorpayClient) throws RazorpayException {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }

        List<Integer> productIds = request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .toList();

        Map<Integer, Products> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Products::getProduct_id, Function.identity()));

        BigDecimal serverCalculatedTotal = BigDecimal.ZERO;
        List<Products> productsToUpdate = new ArrayList<>();

        for (OrderItemRequest item : request.items()) {
            if (item == null) {
                throw new IllegalArgumentException("Order item cannot be null");
            }
            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero for product ID: " + item.productId());
            }
            Products product = productMap.get(item.productId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found with ID: " + item.productId());
            }

            // 1. Stock Check
            if (product.getStock() == null || product.getStock() < item.quantity()) {
                throw new OutOfStockException("Insufficient stock for: " + product.getName() 
                        + " (Available: " + (product.getStock() == null ? 0 : product.getStock()) + ")");
            }

            // 2. Stock Decrement
            product.setStock(product.getStock() - item.quantity());
            productsToUpdate.add(product);

            BigDecimal unitPrice = product.getPrice();
            if (unitPrice == null) {
                throw new IllegalStateException("Product price not configured for ID: " + item.productId());
            }
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            serverCalculatedTotal = serverCalculatedTotal.add(itemTotal);
        }

        // Save decremented stock — this executes the @Version check in MySQL
        productRepository.saveAll(productsToUpdate);

        // 3. Register order with Razorpay
        JSONObject preparePayload = new JSONObject();
        preparePayload.put("amount", serverCalculatedTotal.multiply(BigDecimal.valueOf(100)).intValue());
        preparePayload.put("currency", "INR");
        preparePayload.put("receipt", "txn_" + System.currentTimeMillis());

        Order razorpayorder = razorpayClient.orders.create(preparePayload);

        // 4. Save Orders with Status.PENDING
        Orders order = new Orders();
        order.setId(razorpayorder.get("id"));
        order.setUser(user);
        order.setTotal_amount(serverCalculatedTotal);
        order.setStatus(Status.PENDING);
        orderRepo.save(order);

        // 5. Pre-persist OrderItems with snapshot prices
        List<OrderItems> orderItemsList = new ArrayList<>();
        for (OrderItemRequest item : request.items()) {
            Products product = productMap.get(item.productId());
            BigDecimal unitPrice = product.getPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));

            OrderItems orderItem = new OrderItems();
            orderItem.setOrders(order);
            orderItem.setProducts(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setPricePerUnit(unitPrice);
            orderItem.setTotalPrice(itemTotal);
            orderItemsList.add(orderItem);
        }
        orderItemsRepository.saveAll(orderItemsList);

        return razorpayorder.get("id");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelAndRestoreStock(String orderId) {
        Orders order = orderRepo.findById(orderId).orElse(null);
        if (order != null && order.getStatus() == Status.PENDING) {
            order.setStatus(Status.CANCELLED);
            orderRepo.save(order);

            List<OrderItems> items = orderItemsRepository.findByOrders_Id(orderId);
            List<Products> productsToRestore = new ArrayList<>();

            for (OrderItems oi : items) {
                Products p = oi.getProducts();
                if (p != null) {
                    p.setStock(p.getStock() + oi.getQuantity());
                    productsToRestore.add(p);
                }
            }

            if (!productsToRestore.isEmpty()) {
                productRepository.saveAll(productsToRestore);
            }
        }
    }
}
