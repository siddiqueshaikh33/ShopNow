package com.example.demo.Repository;

import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.Categories;


@Repository
public interface CategoriesRepository extends JpaRepository<Categories, Integer>{
      
	Optional<Categories> findByCategoryName(String categorieName);
}
