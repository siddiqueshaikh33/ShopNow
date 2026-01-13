package com.example.demo.Repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.Products;

@Repository
public interface ProductRepository extends JpaRepository<Products, Integer>{
	List<Products> findByCategories_CategoryId(int category_id);

}
