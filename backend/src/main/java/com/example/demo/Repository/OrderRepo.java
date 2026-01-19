package com.example.demo.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.Entity.Orders;
import com.example.demo.Entity.Status;

import jakarta.transaction.Transactional;

public interface OrderRepo extends JpaRepository<Orders, String>{
	@Query("""
			SELECT DISTINCT o
			FROM Orders o
			JOIN FETCH o.orderItems oi
			JOIN FETCH oi.products p 
			LEFT JOIN FETCH p.primaryImage
			WHERE o.user.userId = :userId
			AND o.status = :status
			""")
			List<Orders> findOrdersWithItemsByUserAndStatus(
			        @Param("status") Status status,
			        @Param("userId") int userId);
	
	
	       
	       @Modifying
	       @Transactional
	       @Query("delete from Orders o where o.status = :status and o.created_at < :dueDateTime")
	       int deletePendingStatus(@Param("status") Status status, @Param("dueDateTime") LocalDateTime dueDateTime);
}
