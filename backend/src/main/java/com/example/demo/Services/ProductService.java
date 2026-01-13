package com.example.demo.Services;


import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.Entity.Categories;
import com.example.demo.Entity.Products;
import com.example.demo.Repository.CategoriesRepository;
import com.example.demo.Repository.ProductImgRepository;
import com.example.demo.Repository.ProductRepository;

@Service
public class ProductService {
    
	private ProductRepository productRepository;
	
	private ProductImgRepository productImgRepository;
	
	private CategoriesRepository categoriesRepository;

	public ProductService(ProductRepository productRepository, ProductImgRepository productImgRepository,
			CategoriesRepository categoriesRepository) {
		super();
		this.productRepository = productRepository;
		this.productImgRepository = productImgRepository;
		this.categoriesRepository = categoriesRepository;
	}
	
	public Categories getCategoryByCategoryName(String categoryName) {
		Optional<Categories> categoryOptional = categoriesRepository.findByCategoryName(categoryName);
		
		if(!categoryOptional.isPresent()) {
			throw new RuntimeException("Category Not Found");
		}
		
		return categoryOptional.get();
	}
	
	
	public List<Products> getProductsByCategoryId(String catergoryName) {
		if(catergoryName != null && !catergoryName.isEmpty()) {
			Categories categoryObject = getCategoryByCategoryName(catergoryName);
			System.out.println("cat:" + productRepository.findByCategories_CategoryId(categoryObject.getCategory_id()));
			return productRepository.findByCategories_CategoryId(categoryObject.getCategory_id());
		} else {
		    return productRepository.findAll();
		}
}
	
	public String getProductImgByProductId(int Product_id) {
		return productImgRepository.findImageUrlByProductId(Product_id);
	}
}
