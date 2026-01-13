package com.example.demo.Repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.Users;

public interface UserRepository extends JpaRepository<Users, Integer>{
    Optional<Users> findByEmail(String email);
    Optional<Users> findByUsername(String username);
    
}
