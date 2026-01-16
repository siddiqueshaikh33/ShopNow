package com.example.demo.Controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entity.Users;
import com.example.demo.Services.OrderListService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class OrderListController {

	private OrderListService orderListService;
	
	public OrderListController(OrderListService orderListService) {
		super();
		this.orderListService = orderListService;
	}

	
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> fetchOrderList(HttpServletRequest req) {
		try {
		Users user = (Users)  req.getAttribute("Authorized_User");
		if(user == null) {
	    return ResponseEntity.badRequest().body(Map.of("error", "Unauthorized User"));
		}
		
	   return ResponseEntity.ok().body(orderListService.getOrderList(user));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Failed to send order List"));
		}
}
}
