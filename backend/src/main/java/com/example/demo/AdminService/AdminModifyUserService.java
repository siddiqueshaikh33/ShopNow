package com.example.demo.AdminService;

import org.springframework.stereotype.Service;

import com.example.demo.Dto.AdminUpdateUserRequest;
import com.example.demo.Dto.UserResponseDto;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.UserRepository;

@Service
public class AdminModifyUserService {
  
	private final UserRepository userRepository;

	public AdminModifyUserService(UserRepository userRepository) {
		super();
		this.userRepository = userRepository;
	}

	public UserResponseDto getUserDetails(int userId) {
		Users user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
		return UserResponseDto.fromEntity(user);
	}
	
	public UserResponseDto updateUserDetails(int userId, AdminUpdateUserRequest updateRequest) {
		Users existingUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (updateRequest.getUsername() != null && !updateRequest.getUsername().isBlank()) {
			existingUser.setUsername(updateRequest.getUsername().trim());
		}

		if (updateRequest.getEmail() != null && !updateRequest.getEmail().isBlank()) {
			existingUser.setEmail(updateRequest.getEmail().trim());
		}

		if (updateRequest.getRole() != null && !updateRequest.getRole().isBlank()) {
			try {
				existingUser.setRole(Role.valueOf(updateRequest.getRole().trim().toUpperCase()));
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Invalid role specified");
			}
		}

		Users savedUser = userRepository.save(existingUser);
		return UserResponseDto.fromEntity(savedUser);
	}
}
