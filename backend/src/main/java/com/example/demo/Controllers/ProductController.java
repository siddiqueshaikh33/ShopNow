package com.example.demo.Controllers;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entity.Products;
import com.example.demo.Entity.Users;
import com.example.demo.Services.ProductService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/products")
@CrossOrigin( origins = "http://localhost:5173",
allowCredentials = "true")
public class ProductController {
     private ProductService productService;

	 public ProductController(ProductService productService) {
		super();
		this.productService = productService;
	 }
     
	 @GetMapping
	 public ResponseEntity<Map<String, Object>> fetchProduct(@RequestParam(required = false) String categoryName,
			 HttpServletRequest req) {
		 try {
		    Users authorizeUsers = (Users) req.getAttribute("Authorized_User");
		    if(authorizeUsers == null) {
		          return ResponseEntity.status(401).body(Map.of("Error", "Unauthorize User"));
		    }
		    System.out.println("category:" + categoryName);
		    List<Products> products = productService.getProductsByCategoryId(categoryName);
		    Map<String, Object> responseMap = new HashMap<>();
		    Map<String, Object> userInfo = new HashMap<>();
		    userInfo.put("username", authorizeUsers.getUsername());
		    userInfo.put("role", authorizeUsers.getRole().name());
		    responseMap.put("user", userInfo);
		    List<Map<String, Object>> productList = new ArrayList<>();
		    for(Products product: products) {
		    	String imageUrl = productService.getProductImgByProductId(product.getProduct_id());
		    	Map<String, Object> productMap = new HashMap<>();
		    	productMap.put("name", product.getName());
		    	productMap.put("product_id", product.getProduct_id());
		    	productMap.put("price", product.getPrices());
		    	productMap.put("stock", product.getStock());
		    	productMap.put("description", product.getDescription());
		    	productMap.put("product_img", imageUrl);
		    	productList.add(productMap);
		    }
		    System.out.println("length:" + productList.size());
		    responseMap.put("product", productList);
		    return ResponseEntity.ok(responseMap);
		    
		 } catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	 }
	 
	 
}
