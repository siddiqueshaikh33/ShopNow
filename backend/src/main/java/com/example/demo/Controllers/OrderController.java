package com.example.demo.Controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Dto.CreateOrderRequest;
import com.example.demo.Entity.Users;
import com.example.demo.Services.OrderService;
import com.razorpay.RazorpayException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/payment")
public class OrderController {

    private final OrderService orderService;

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> setOrder(
            HttpServletRequest req,
            @RequestBody CreateOrderRequest request) {

        log.info("🔔 setOrder API called with {} items", request.items() != null ? request.items().size() : 0);

        try {
            Users user = (Users) req.getAttribute("Authorized_User");

            if (user == null) {
                log.error("❌ Authorized_User is NULL");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authorized");
            }

            log.info("✅ Authorized user ID: {}", user.getUser_id());

            String orderId = orderService.createOrder(user, request);

            log.info("🎉 Order successfully created. OrderId: {}", orderId);

            return ResponseEntity.ok(orderId);

        } catch (RazorpayException e) {
            log.error("🔥 RazorpayException while creating order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating Razorpay order: " + e.getMessage());

        } catch (IllegalArgumentException e) {
            log.error("🔥 Invalid order request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request data: " + e.getMessage());

        } catch (Exception e) {
            log.error("🔥 Exception while processing order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process order: " + e.getMessage());
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
