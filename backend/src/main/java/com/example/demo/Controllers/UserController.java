package com.example.demo.Controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Dto.RegisterRequest;
import com.example.demo.Dto.UserResponseDto;
import com.example.demo.Entity.Users;
import com.example.demo.Services.UserService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/users")
public class UserController {
	
	private final UserService userService;
	
	public UserController(UserService userService) {
		this.userService = userService;
	}
	
    @PostMapping("/register")
	public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
    	try {
			Users registerUser = userService.registerUser(registerRequest);
			UserResponseDto userDto = UserResponseDto.fromEntity(registerUser);
			return ResponseEntity.ok(Map.of(
				"message", "User Register Successfully",
				"User", userDto
			));
    	}
    	catch(RuntimeException e) {
    		return ResponseEntity.badRequest().body(Map.of("Error", e.getMessage()));
    	}
	}
}
