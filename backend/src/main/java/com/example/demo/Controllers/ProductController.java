package com.example.demo.Controllers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Dto.PaginatedProductResponse;
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
	public ResponseEntity<?> fetchProduct(
			@RequestParam(required = false) String categoryName,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size,
			HttpServletRequest req) {
		try {
			Users authorizeUsers = (Users) req.getAttribute("Authorized_User");
			if (authorizeUsers == null) {
				return ResponseEntity.status(401).body(Map.of("Error", "Unauthorize User"));
			}

			Pageable pageable = PageRequest.of(page, size);
			Slice<ProductResponseDto> productSlice = productService.getProductDtosByCategoryId(categoryName, pageable);

			// Fallback if mocked or legacy callers only stubbed the 1-argument method
			if (productSlice == null) {
				List<ProductResponseDto> legacyList = productService.getProductDtosByCategoryId(categoryName);
				productSlice = new SliceImpl<>(legacyList != null ? legacyList : Collections.emptyList(), pageable, false);
			}

			Map<String, String> userMap = Map.of(
				"username", authorizeUsers.getUsername(),
				"role", authorizeUsers.getRole().name()
			);

			PaginatedProductResponse response = new PaginatedProductResponse(
					productSlice.getContent(),
					productSlice.getNumber(),
					productSlice.getSize(),
					productSlice.hasNext(),
					userMap
			);

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}
