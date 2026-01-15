package com.example.demo.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.Entity.Jwttoken;

import jakarta.transaction.Transactional;

public interface JwtRepository extends JpaRepository<Jwttoken, Integer>{
   
	@Query("select t from Jwttoken t where t.users.userId = :user_id")
	Jwttoken findByUserId(@Param("user_id") int user_id);
	
	
	Optional<Jwttoken> findByToken(@Param("token") String token);
	
	@Transactional
	void deleteByUsersUserId(int userId);


}
