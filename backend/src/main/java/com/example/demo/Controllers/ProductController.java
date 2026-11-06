package com.example.demo.Controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Dto.ProductResponseDto;
import com.example.demo.Entity.Users;
import com.example.demo.Services.ProductService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public ResponseEntity<?> fetchProduct(@RequestParam(required = false) String categoryName,
			HttpServletRequest req) {
		try {
			Users authorizeUsers = (Users) req.getAttribute("Authorized_User");
			if (authorizeUsers == null) {
				return ResponseEntity.status(401).body(Map.of("Error", "Unauthorize User"));
			}

			List<ProductResponseDto> productList = productService.getProductDtosByCategoryId(categoryName);

			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("user", Map.of(
				"username", authorizeUsers.getUsername(),
				"role", authorizeUsers.getRole().name()
			));
			responseMap.put("product", productList);

			return ResponseEntity.ok(responseMap);

		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}
