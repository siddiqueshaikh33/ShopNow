package com.example.demo.AdminController;

import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Dto.AddProductDto;
import com.example.demo.Entity.Categories;
import com.example.demo.Entity.Productimages;
import com.example.demo.Entity.Products;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.CategoriesRepository;
import com.example.demo.Repository.ProductRepository;



import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminAddProductController {

    private ProductRepository productRepository;
    private CategoriesRepository categoriesRepository;
    
    private static final Logger log =
            LoggerFactory.getLogger(AdminAddProductController.class);

    public AdminAddProductController(ProductRepository productRepository, CategoriesRepository categoriesRepository) {
        this.productRepository = productRepository;
        this.categoriesRepository = categoriesRepository;
    }

    @PostMapping("/addProduct")
    public ResponseEntity<?> addProduct(@RequestBody AddProductDto productDto, HttpServletRequest request) {
    	 log.info("Add product request received: {}", productDto);
   
        try {
            Users users = (Users) request.getAttribute("Authorized_User");
            
             if(users == null || !(users.getRole() == Role.ADMIN)) {
            	 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Unauthorized User"));
             }
            
             Categories cat = categoriesRepository.findById(productDto.getCategoryId())
            		 .orElseThrow(() -> new RuntimeException("Category Not Found"));
             
             Products products = new Products();
             products.setName(productDto.getName());
             products.setDescription(productDto.getDescription());
             products.setPrice(productDto.getPrice());
             products.setStock(productDto.getStock());
             products.setCategories(cat);


             if (productDto.getImgUrl() != null && !productDto.getImgUrl().isBlank()) {
                 Productimages image = new Productimages();
                 image.setImageUrl(productDto.getImgUrl());
                 image.setProducts(products);

                 products.setPrimaryImage(image);
             }

             productRepository.save(products);
            return ResponseEntity.ok().body(Map.of("message", "Successfully Added"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }
}



