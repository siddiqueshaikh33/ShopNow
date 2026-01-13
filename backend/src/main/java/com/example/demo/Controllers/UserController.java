package com.example.demo.Controllers;

import java.util.Map;



import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entity.Users;
import com.example.demo.Services.UserService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/users")
public class UserController {
	
	UserService userService;
	
	
	public UserController(UserService userService) {
		this.userService = userService;
	}
	
    @PostMapping("/register")
	public ResponseEntity<?> registerUser(@RequestBody Users user) {
    	try {
		Users registerUser =  userService.registerUser(user);
		return ResponseEntity.ok(Map.of("message", "User Register Successfully", "User", registerUser));
    	}
    	catch(RuntimeException e) {
    		return ResponseEntity.badRequest().body(Map.of("Error", e.getMessage()));
    	}
	}
}
