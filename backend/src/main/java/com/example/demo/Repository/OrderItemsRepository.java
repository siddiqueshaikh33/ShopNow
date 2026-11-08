package com.example.demo.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.Entity.OrderItems;
import com.example.demo.Entity.Status;

import jakarta.transaction.Transactional;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Integer>{

    List<OrderItems> findByOrders_Id(String orderId);

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderItems oi WHERE oi.orders.id IN (SELECT o.id FROM Orders o WHERE o.status = :status AND o.created_at < :dueDateTime)")
    int deleteByOrdersStatusAndCreatedAtBefore(
            @Param("status") Status status,
            @Param("dueDateTime") LocalDateTime dueDateTime);

}
