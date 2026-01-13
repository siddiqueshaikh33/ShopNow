package com.example.demo.Repository;





import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.Productimages;

@Repository
public interface ProductImgRepository extends JpaRepository<Productimages, Integer>{
   
	@Query("""
	        SELECT pi.imageUrl
	        FROM Productimages pi
	        WHERE pi.products.productId = :productId
	    """)
	    String findImageUrlByProductId(@Param("productId") int productId);
	
	
	
	@Query(""" 
			select pi from Productimages pi where pi.products.productId IN :productIds
			""")
	List<Productimages> findImageByListOfProductId(@Param("productIds") List<Integer> productIds);
}
