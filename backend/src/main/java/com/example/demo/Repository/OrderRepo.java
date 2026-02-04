package com.example.demo.Repository;

import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demo.Dto.CountTotalCatgorySalesDto;
import com.example.demo.Dto.YearlyBusinessDto;
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
	       
	       
	       
	       @Query("""
	    		    select coalesce(sum(o.total_amount), 0)
	    		    from Orders o
	    		    where month(o.created_at) = :month
	    		      and year(o.created_at) = :year
	    		      and o.status = :status
	    		""")
	    		Double sumTotalAmountByMonthAndYear(@Param("month") int month,
	    		                                   @Param("year") long year,
	    		                                   @Param("status") Status status);

	
	       
	       @Query("""
	    		    select new com.example.demo.Dto.CountTotalCatgorySalesDto(
	    		       p.categories.categoryName,
	    		        count(distinct o.id)
	    		    )
	    		    from Orders o
	    		    join o.orderItems oi
	    		    join oi.products p
	    		    where month(o.created_at) = :month
	    		      and year(o.created_at) = :year
	    		      and o.status = :status
	    		    group by p.categories.categoryId, p.categories.categoryName
	    		""")
	    		List<CountTotalCatgorySalesDto> countOrdersPerCategory(
	    		    @Param("month") int month,
	    		    @Param("year") long year,
	    		    @Param("status") Status status
	    		);


	      
	       
	       @Query("""
	    		   SELECT new com.example.demo.Dto.YearlyBusinessDto(
	    		       FUNCTION('MONTHNAME', o.created_at),
	    		       SUM(o.total_amount)
	    		   )
	    		   
	    		   FROM Orders o
	    		   WHERE FUNCTION('YEAR', o.created_at) = :year
	    		   AND o.status = :status
	    		   GROUP BY FUNCTION('MONTH', o.created_at), FUNCTION('MONTHNAME', o.created_at)
	    		   ORDER BY FUNCTION('MONTH', o.created_at)
	    		   """)
	    		   List<YearlyBusinessDto> getMonthlyTotal(@Param("year") long year,
	    		                                          @Param("status") Status status);

            
	       

	       
}
