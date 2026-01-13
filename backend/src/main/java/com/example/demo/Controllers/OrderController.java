package com.example.demo.Controllers;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entity.OrderItems;
import com.example.demo.Entity.Products;
import com.example.demo.Entity.Users;
import com.example.demo.Services.OrderService;
import com.razorpay.RazorpayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/payment")
public class OrderController {
     
    private OrderService orderService;
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

	
	public OrderController(OrderService orderService) {
		super();
		this.orderService = orderService;
	}

    @PostMapping("/create")
	public ResponseEntity<String> setOrder(
	        HttpServletRequest req,
	        @RequestBody Map<String, Object> responseBody) {

	    log.info("🔔 setOrder API called");

	    try {
	        Users user = (Users) req.getAttribute("Authorized_User");

	        if (user == null) {
	            log.error("❌ Authorized_User is NULL");
	            throw new RuntimeException("User is not authorized");
	        }

	        log.info("✅ Authorized user ID: {}", user.getUser_id());

	        // 🔍 Log full request body
	        log.info("📦 Request Body: {}", responseBody);

	        // 🔍 Log total amount
	 
	     

	        BigDecimal totalAmount = new BigDecimal(responseBody.get("totalAmount").toString());

	        log.info("💰 Parsed totalAmount: {}", totalAmount);

	        // 🔍 Log cart items
	        List<Map<String, Object>> cartItemsList =
	                (List<Map<String, Object>>) responseBody.get("cartItems");

	        log.info("🛒 Cart items count: {}", cartItemsList.size());

	        List<OrderItems> orderItemList = cartItemsList.stream().map(item -> {

	            log.info("➡️ Processing cart item: {}", item);

	            Object productIdObj = item.get("productId");
	            Object quantityObj = item.get("quantity");
	            Object priceObj = item.get("pricePerUnit");

	            log.info("   productId: {} (type: {})", productIdObj, productIdObj.getClass().getName());
	            log.info("   quantity: {} (type: {})", quantityObj, quantityObj.getClass().getName());
	            log.info("   pricePerUnit: {} (type: {})", priceObj, priceObj.getClass().getName());

	            OrderItems orderItems = new OrderItems();

	            orderItems.setProducts(new Products((int) productIdObj));
	            orderItems.setQuantity((int) quantityObj);

	            BigDecimal pricePerUnit =
	                    new BigDecimal(priceObj.toString());

	            orderItems.setPricePerUnit(pricePerUnit);
	            orderItems.setTotalPrice(
	                    pricePerUnit.multiply(BigDecimal.valueOf((Integer) quantityObj))
	            );

	            log.info("✅ OrderItem created: productId={}, quantity={}, totalPrice={}",
	                    productIdObj, quantityObj, orderItems.getTotalPrice());

	            return orderItems;

	        }).collect(Collectors.toList());

	        log.info("📑 Total OrderItems created: {}", orderItemList.size());

	        String orderId =
	                orderService.createOrder(user, totalAmount, orderItemList);

	        log.info("🎉 Order successfully created. OrderId: {}", orderId);

	        return ResponseEntity.ok(orderId);

	    } catch (RazorpayException e) {
	        log.error("🔥 RazorpayException while creating order", e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("Error creating Razorpay order: " + e.getMessage());

	    } catch (Exception e) {
	        log.error("🔥 Exception while processing order", e);
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body("Invalid request data: " + e.getMessage());
	    }
	}
    
    
    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody Map<String, String> payment, HttpServletRequest request) {
    	try {
    	Users user = (Users) request.getAttribute("Authorized_User");
    	if(user == null) {
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User Unauthorize!");
    	}
    	
    	boolean isVerified  =  orderService.verifyOrder(payment.get("razorpayPaymentId"), payment.get("razorpayOrderId"), payment.get("razorpaySignature"), user);
    	       if(isVerified) {
    	    	 return  ResponseEntity.ok("Payment Complete Successfull!");
    	       } else {
    	    	  return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment Verification Failed!");
    	       }
    	}
    	catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Payment Failed" + e.getMessage());
		}
	
    	
    }

}
