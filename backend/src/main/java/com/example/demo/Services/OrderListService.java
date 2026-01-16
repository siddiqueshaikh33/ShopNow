package com.example.demo.Services;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.attribute.standard.MediaSize.Other;

import org.springframework.stereotype.Service;

import com.example.demo.Entity.OrderItems;
import com.example.demo.Entity.Orders;
import com.example.demo.Entity.Status;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.OrderItemsRepository;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.ProductImgRepository;
import com.example.demo.Repository.ProductRepository;

@Service
public class OrderListService {
    
	OrderRepo orderRepo;
	
	OrderItemsRepository orderItemsRepository;
	
	ProductRepository productRepository;
	
	ProductImgRepository productImgRepository;
	
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

	public OrderListService(OrderRepo orderRepo, OrderItemsRepository orderItemsRepository,
			ProductRepository productRepository, ProductImgRepository productImgRepository) {
		super();
		this.orderRepo = orderRepo;
		this.orderItemsRepository = orderItemsRepository;
		this.productRepository = productRepository;
		this.productImgRepository = productImgRepository;
	}
    

    public Map<String, Object> getOrderList(Users user) {
        logger.info("Fetching order list for userId: {}, role: {}",
                user.getUser_id(), user.getRole());

        Map<String, Object> response = new HashMap<>();

        try {
            List<Orders> orderItems =
                    orderRepo.findOrdersWithItemsByUserAndStatus(
                            Status.SUCCESS, user.getUser_id());

            logger.info("Orders fetched from DB: {}",
                    orderItems != null ? orderItems.size() : "NULL");

            response.put("role", user.getRole());
            response.put("username", user.getUsername());

            Map<String, List<Map<String, Object>>> orders = new HashMap<>();
            List<Map<String, Object>> orderList = new ArrayList<>();

            if (orderItems == null || orderItems.isEmpty()) {
                logger.warn("No SUCCESS orders found for userId: {}", user.getUser_id());
            }

            for (Orders order : orderItems) {
                logger.debug("Processing orderId: {}", order.getId());

                if (order.getOrderItems() == null) {
                    logger.warn("OrderItems is NULL for orderId: {}", order.getId());
                    continue;
                }

                for (OrderItems oi : order.getOrderItems()) {
                    logger.debug("Processing OrderItemId: {}", oi.getId());

                    if (oi.getProducts() == null) {
                        logger.error("Product is NULL for OrderItemId: {}", oi.getId());
                        continue;
                    }

                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("order_id", order.getId());
                    productMap.put("quantity", oi.getQuantity());
                    productMap.put("total_price", oi.getTotalPrice());
                    productMap.put("product_img", oi.getProducts().getPrimaryImage().getImageUrl());
                    productMap.put("product_id", oi.getProducts().getProduct_id());
                    productMap.put("product_name", oi.getProducts().getName());
                    productMap.put("description", oi.getProducts().getDescription());
                    productMap.put("price_per_unit", oi.getPricePerUnit());

                    orderList.add(productMap);
                }
            }

            orders.put("products", orderList);
            response.put("orders", orders);

            logger.info("Order list prepared successfully. Total products: {}", orderList.size());

        } catch (Exception e) {
            logger.error("Error while fetching order list for userId: {}",
                    user.getUser_id(), e);
            throw e; // or return empty response
        }

        return response;
    }
	
}
