package com.example.demo.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.CartItems;

import jakarta.transaction.Transactional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItems, Integer> {
	
	@Query("""
		    SELECT c 
		    FROM CartItems c
		    WHERE c.user.userId = :userId
		    AND c.product.productId = :productId
		""")
		Optional<CartItems> findByUserIdAndProductId(
		        @Param("userId") Integer userId,
		        @Param("productId") Integer productId
		);
   
	
	   @Query(""" 
	   	SELECT COALESCE(SUM(quantity), 0) 
	   	FROM CartItems c 
	   	WHERE c.user.userId = :userId
	   		""")
	   int countCartItems(@Param("userId") Integer userId);
       
	   
	   @Query("""
	   		 select c from CartItems c join fetch c.product p left join fetch Productimages pi on p.productId = pi.products.productId
	   		 where c.user.userId = :userId
	   		     """)
	   List<CartItems> fetchAllCartItems(@Param("userId") Integer userId);
	   
	   
	   
	   @Modifying
	   @Transactional
	   @Query(" delete from CartItems c where c.user.userId = :userId AND c.product.productId = :productId")
	   void deleteTheCartItem(@Param("userId") Integer userId, @Param("productId") Integer productId);
	   
	
}
