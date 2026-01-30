package com.example.demo.AdminController;


import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entity.Products;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.ProductRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:5173/", allowCredentials = "true")
public class AdminDeleteController {
	
	private ProductRepository productRepository;
	
	
	public AdminDeleteController(ProductRepository productRepository) {
		super();
		this.productRepository = productRepository;
	}
    
	@GetMapping("/product/{productId}")
	private ResponseEntity<?> findProduct(@PathVariable int productId, HttpServletRequest request) {
		try {
		Users users = (Users) request.getAttribute("Authorized_User");
		
		if(users == null) {
			return ResponseEntity.badRequest().body("Unauthorized Admin");
		}
		
		Products products = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("No product found"));
		
		
		
		return ResponseEntity.ok().body(Map.of("product_name", products.getName(), "category", products.getCategories().getCategory_name()));
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("something went wrong");
	}
	}
   
	@DeleteMapping("/deleteproduct/{productId}")
	private ResponseEntity<?> deleteProduct(@PathVariable int productId, HttpServletRequest request) {
		try {
		Users users = (Users) request.getAttribute("Authorized_User");
		
		if(users == null) {
			return ResponseEntity.badRequest().body("Unauthorized Admin");
		}
		
		Products products = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("No product found"));
		
		productRepository.deleteById(products.getProduct_id());
		
		return ResponseEntity.ok().build();
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("something went wrong");
	}
	}
}
