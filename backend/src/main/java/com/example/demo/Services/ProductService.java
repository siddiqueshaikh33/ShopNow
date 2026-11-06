package com.example.demo.Services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.Dto.ProductResponseDto;
import com.example.demo.Entity.Categories;
import com.example.demo.Entity.Products;
import com.example.demo.Repository.CategoriesRepository;
import com.example.demo.Repository.ProductImgRepository;
import com.example.demo.Repository.ProductRepository;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductImgRepository productImgRepository;
	private final CategoriesRepository categoriesRepository;

	public ProductService(ProductRepository productRepository, ProductImgRepository productImgRepository,
			CategoriesRepository categoriesRepository) {
		this.productRepository = productRepository;
		this.productImgRepository = productImgRepository;
		this.categoriesRepository = categoriesRepository;
	}

	public Categories getCategoryByCategoryName(String categoryName) {
		Optional<Categories> categoryOptional = categoriesRepository.findByCategoryName(categoryName);

		if (!categoryOptional.isPresent()) {
			throw new RuntimeException("Category Not Found");
		}

		return categoryOptional.get();
	}

	public List<Products> getProductsByCategoryId(String categoryName) {
		if (categoryName != null && !categoryName.isEmpty()) {
			Categories categoryObject = getCategoryByCategoryName(categoryName);
			return productRepository.findByCategories_CategoryIdWithPrimaryImage(categoryObject.getCategoryId());
		} else {
			return productRepository.findAllWithPrimaryImage();
		}
	}

	public List<ProductResponseDto> getProductDtosByCategoryId(String categoryName) {
		List<Products> products = getProductsByCategoryId(categoryName);
		return products.stream()
				.map(ProductResponseDto::fromEntity)
				.toList();
	}

	public String getProductImgByProductId(int productId) {
		return productImgRepository.findImageUrlByProductId(productId);
	}
}
