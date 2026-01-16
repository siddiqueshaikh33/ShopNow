package com.example.demo.Repository;



import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.Entity.OrderItems;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Integer>{
       
}
