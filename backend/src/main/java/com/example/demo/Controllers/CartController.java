package com.example.demo.Controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Dto.AddCartDto;
import com.example.demo.Entity.Users;
import com.example.demo.Services.CartService;

import jakarta.servlet.http.HttpServletRequest;



@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/cart")
public class CartController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CartController.class);
	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@PostMapping("/add")
	public ResponseEntity<?> addCartItems(HttpServletRequest req, @RequestBody AddCartDto addCart) {
		try {
			Users user = (Users) req.getAttribute("Authorized_User");
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized User"));
			}

			int count = cartService.addOrUpdateCartItem(user.getUser_id(), addCart.getProduct_id(), addCart.getQuantity());
			return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("count", count));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to add to cart"));
		}
	}

	@GetMapping("/count")
	public ResponseEntity<?> getCartCount(HttpServletRequest req) {
		try {
			Users user = (Users) req.getAttribute("Authorized_User");
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized User"));
			}

			int count = cartService.countCartItem(user.getUser_id());
			return ResponseEntity.ok(Map.of("count", count));
		} catch (Exception e) {
			LOGGER.error("Failed to retrieve cart count: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Failed to retrieve cart count"));
		}
	}
    
	@GetMapping("/getitems")
	public ResponseEntity<?> fetch(HttpServletRequest req) {
		Users user = (Users) req.getAttribute("Authorized_User");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized User"));
		}
		
		Map<String, Object> allCartItems = cartService.fetchAllCartItemsByUserName(user.getUsername());
		return ResponseEntity.ok(allCartItems);	
	}
	
	@PutMapping("/update")
	public ResponseEntity<?> update(HttpServletRequest req, @RequestParam int productId, @RequestParam int quantity) {
		try {
			Users user = (Users) req.getAttribute("Authorized_User");
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized User"));
			}

			cartService.updateCartItem(user.getUser_id(), productId, quantity);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to update quantity"));
		}
	}
	
	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteCartItem(HttpServletRequest request, @RequestParam int productId) {
		try {
			Users user = (Users) request.getAttribute("Authorized_User");
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized User"));
			}

			cartService.deleteCart(user.getUser_id(), productId);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
