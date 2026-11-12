package com.example.demo.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import com.example.demo.Dto.ProductResponseDto;
import com.example.demo.Entity.Categories;
import com.example.demo.Entity.Productimages;
import com.example.demo.Entity.Products;
import com.example.demo.Repository.CategoriesRepository;
import com.example.demo.Repository.ProductImgRepository;
import com.example.demo.Repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductImgRepository productImgRepository;

	@Mock
	private CategoriesRepository categoriesRepository;

	@InjectMocks
	private ProductService productService;

	private Products productWithImage;
	private Products productWithoutImage;

	@BeforeEach
	void setUp() {
		productWithImage = new Products(1);
		productWithImage.setName("Wireless Mouse");
		productWithImage.setDescription("Ergonomic wireless mouse");
		productWithImage.setPrice(new BigDecimal("29.99"));
		productWithImage.setStock(50);

		Productimages image = new Productimages();
		image.setImageUrl("https://example.com/mouse.jpg");
		productWithImage.setPrimaryImage(image);

		productWithoutImage = new Products(2);
		productWithoutImage.setName("USB Cable");
		productWithoutImage.setDescription("Type-C 1m cable");
		productWithoutImage.setPrice(new BigDecimal("9.99"));
		productWithoutImage.setStock(100);
	}

	@Test
	void getProductDtosByCategoryId_nullCategory_fetchesAllWithPrimaryImageAndNoSecondaryQueries() {
		when(productRepository.findAllWithPrimaryImage()).thenReturn(List.of(productWithImage, productWithoutImage));

		List<ProductResponseDto> result = productService.getProductDtosByCategoryId(null);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).product_id()).isEqualTo(1);
		assertThat(result.get(0).name()).isEqualTo("Wireless Mouse");
		assertThat(result.get(0).product_img()).isEqualTo("https://example.com/mouse.jpg");

		assertThat(result.get(1).product_id()).isEqualTo(2);
		assertThat(result.get(1).name()).isEqualTo("USB Cable");
		assertThat(result.get(1).product_img()).isNull();

		// Verify N+1 prevention: productImgRepository must NEVER be called per-product
		verify(productRepository).findAllWithPrimaryImage();
		verify(productImgRepository, never()).findImageUrlByProductId(1);
		verify(productImgRepository, never()).findImageUrlByProductId(2);
	}

	@Test
	void getProductDtosByCategoryId_withCategory_fetchesByCategoryWithPrimaryImage() {
		Categories electronics = new Categories("Electronics");
		electronics.setCategoryId(10);
		when(categoriesRepository.findByCategoryName("Electronics")).thenReturn(Optional.of(electronics));
		when(productRepository.findByCategories_CategoryIdWithPrimaryImage(10)).thenReturn(List.of(productWithImage));

		List<ProductResponseDto> result = productService.getProductDtosByCategoryId("Electronics");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).name()).isEqualTo("Wireless Mouse");
		assertThat(result.get(0).product_img()).isEqualTo("https://example.com/mouse.jpg");

		verify(productRepository).findByCategories_CategoryIdWithPrimaryImage(10);
		verify(productImgRepository, never()).findImageUrlByProductId(1);
	}

	@Test
	@DisplayName("Pagination: page 0 returns slice with hasNext=true when more items exist")
	void getProductDtosByCategoryId_paginationPage0_hasNextTrue() {
		Pageable pageable = PageRequest.of(0, 1);
		Slice<Products> repositorySlice = new SliceImpl<>(List.of(productWithImage), pageable, true);
		when(productRepository.findAllWithPrimaryImage(pageable)).thenReturn(repositorySlice);

		Slice<ProductResponseDto> result = productService.getProductDtosByCategoryId(null, pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getNumber()).isEqualTo(0);
		assertThat(result.getSize()).isEqualTo(1);
		assertThat(result.hasNext()).isTrue();
		assertThat(result.getContent().get(0).product_id()).isEqualTo(1);
		assertThat(result.getContent().get(0).name()).isEqualTo("Wireless Mouse");

		verify(productRepository).findAllWithPrimaryImage(pageable);
		verify(productImgRepository, never()).findImageUrlByProductId(any(Integer.class));
	}

	@Test
	@DisplayName("Pagination: last page returns slice with hasNext=false")
	void getProductDtosByCategoryId_paginationLastPage_hasNextFalse() {
		Pageable pageable = PageRequest.of(1, 1);
		Slice<Products> repositorySlice = new SliceImpl<>(List.of(productWithoutImage), pageable, false);
		when(productRepository.findAllWithPrimaryImage(pageable)).thenReturn(repositorySlice);

		Slice<ProductResponseDto> result = productService.getProductDtosByCategoryId(null, pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getNumber()).isEqualTo(1);
		assertThat(result.hasNext()).isFalse();
		assertThat(result.getContent().get(0).product_id()).isEqualTo(2);
		assertThat(result.getContent().get(0).name()).isEqualTo("USB Cable");

		verify(productRepository).findAllWithPrimaryImage(pageable);
	}

	@Test
	@DisplayName("Pagination: category filter returns sliced results for that category")
	void getProductDtosByCategoryId_paginationWithCategory() {
		Pageable pageable = PageRequest.of(0, 10);
		Categories electronics = new Categories("Electronics");
		electronics.setCategoryId(10);
		when(categoriesRepository.findByCategoryName("Electronics")).thenReturn(Optional.of(electronics));

		Slice<Products> repositorySlice = new SliceImpl<>(List.of(productWithImage), pageable, false);
		when(productRepository.findByCategories_CategoryIdWithPrimaryImage(10, pageable)).thenReturn(repositorySlice);

		Slice<ProductResponseDto> result = productService.getProductDtosByCategoryId("Electronics", pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.hasNext()).isFalse();
		assertThat(result.getContent().get(0).name()).isEqualTo("Wireless Mouse");

		verify(productRepository).findByCategories_CategoryIdWithPrimaryImage(10, pageable);
	}
}
