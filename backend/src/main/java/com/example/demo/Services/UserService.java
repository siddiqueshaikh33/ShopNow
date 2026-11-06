package com.example.demo.Services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.Dto.RegisterRequest;
import com.example.demo.Entity.Provider;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.UserRepository;

@Service
public class UserService {
   
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	public Users registerUser(RegisterRequest request) {
		if (request.getEmail() == null || request.getEmail().isBlank()) {
			throw new RuntimeException("Email is required");
		}
		if (request.getUsername() == null || request.getUsername().isBlank()) {
			throw new RuntimeException("Username is required");
		}
		if (request.getPassword() == null || request.getPassword().isBlank()) {
			throw new RuntimeException("Password is required");
		}

		if (userRepository.findByEmail(request.getEmail()).isPresent()) {
			throw new RuntimeException("Email is already taken");
		}
		 
		if (userRepository.findByUsername(request.getUsername()).isPresent()) {
			throw new RuntimeException("Username is already taken");
		} 
		 
		Users user = new Users();
		user.setUsername(request.getUsername().trim());
		user.setEmail(request.getEmail().trim());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		
		// Enforce safe defaults - Role is strictly CUSTOMER for public self-registration
		user.setRole(Role.CUSTOMER);
		user.setProvider(Provider.LOCAL);
		
		return userRepository.save(user); 
	}
}
