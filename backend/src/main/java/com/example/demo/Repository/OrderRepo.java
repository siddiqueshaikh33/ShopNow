package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.Orders;

public interface OrderRepo extends JpaRepository<Orders, String>{

}
