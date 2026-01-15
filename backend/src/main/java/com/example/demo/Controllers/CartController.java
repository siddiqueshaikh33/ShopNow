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
import com.example.demo.Filter.AuthFilter;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Services.CartService;

import jakarta.servlet.http.HttpServletRequest;



@RestController
@CrossOrigin(originPatterns = "http://localhost:5173" , allowCredentials = "true")
@RequestMapping("/api/cart/")
public class CartController {
	
	 private static final Logger LOGGER = LoggerFactory.getLogger(CartController.class);
	private CartService cartService;
	private UserRepository userRepository;
	
	public CartController(CartService cartService, UserRepository userRepository) {
		super();
		this.cartService = cartService;
		this.userRepository = userRepository;
	}

	
	@PostMapping("add")
	
	public ResponseEntity<?> addCartItems(@RequestBody AddCartDto addCart) {
		try {
			LOGGER.error("running....");
		int count = cartService.addOrUpdateCartItem(addCart);
		 return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("count", count));
		} catch (Exception e) {
			LOGGER.error("errrorr.........");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "ERRORR CART ADD"));
		}
	}
	
	
	@GetMapping("count")
	public ResponseEntity<?> getCartCount(@RequestParam String username) {
		try {
			LOGGER.error("error ohhh ji error");
	    int count = cartService.getTotalCartQuantity(username);
	    return ResponseEntity.status(HttpStatus.OK).body(Map.of("count", count));
		} catch (Exception e) {
			LOGGER.error("dhabba error");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "ERROR COUNTING ERROR"));
		}
	}
    
	@GetMapping("getitems")
	public ResponseEntity<Map<String, Object>> fetch(HttpServletRequest req) {
		Users user = (Users) req.getAttribute("Authorized_User");
		if(user == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "UnAuthorize User"));
		}
		
		Map<String, Object> allCartItems = cartService.fetchAllCartItemsByUserName(user.getUsername());
		return ResponseEntity.ok(allCartItems);	
	}
	
	@PutMapping("update")
	public ResponseEntity<Void> update(HttpServletRequest req, @RequestParam int productId, @RequestParam int quantity) {
		Users user = (Users) req.getAttribute("Authorized_User");
	   cartService.updateCartItem(user.getUser_id(), productId, quantity);
	   return ResponseEntity.ok().build();
	}
	
	@DeleteMapping("delete")
	public ResponseEntity<Void> deleteCartItem(HttpServletRequest request, @RequestParam int productId) {
		try {
		Users user = (Users) request.getAttribute("Authorized_User");
		cartService.deleteCart(user.getUser_id(), productId);
		return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
