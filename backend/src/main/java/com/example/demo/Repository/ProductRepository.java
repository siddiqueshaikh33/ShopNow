package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.Products;

@Repository
public interface ProductRepository extends JpaRepository<Products, Integer> {

	@Query("SELECT DISTINCT p FROM Products p LEFT JOIN FETCH p.primaryImage LEFT JOIN FETCH p.categories")
	List<Products> findAllWithPrimaryImage();

	@Query("SELECT DISTINCT p FROM Products p LEFT JOIN FETCH p.primaryImage LEFT JOIN FETCH p.categories WHERE p.categories.categoryId = :categoryId")
	List<Products> findByCategories_CategoryIdWithPrimaryImage(@Param("categoryId") int categoryId);

	@Query("SELECT p FROM Products p LEFT JOIN FETCH p.primaryImage LEFT JOIN FETCH p.categories")
	Slice<Products> findAllWithPrimaryImage(Pageable pageable);

	@Query("SELECT p FROM Products p LEFT JOIN FETCH p.primaryImage LEFT JOIN FETCH p.categories WHERE p.categories.categoryId = :categoryId")
	Slice<Products> findByCategories_CategoryIdWithPrimaryImage(@Param("categoryId") int categoryId, Pageable pageable);
}
