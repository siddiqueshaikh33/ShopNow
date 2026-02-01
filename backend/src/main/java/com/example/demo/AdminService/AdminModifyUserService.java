package com.example.demo.AdminService;





import org.springframework.stereotype.Service;

import com.example.demo.Dto.AdminUpdateUserRequest;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.UserRepository;

@Service
public class AdminModifyUserService {
  
	private UserRepository userRepository;

	
	public AdminModifyUserService(UserRepository userRepository) {
		super();
		this.userRepository = userRepository;
	}

	public Users getUserDetails(int userId) {
		
		Users users = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Users not found"));
		
		return users;
	}
	
	 public Users updateUserDetails(int userId, AdminUpdateUserRequest updateRequest) {

	        Users existingUser = userRepository.findById(userId)
	                .orElseThrow(() -> new RuntimeException("User not found"));

	     
	        if (updateRequest.getUsername() != null) {
	            existingUser.setUsername(updateRequest.getUsername());
	        }

	        if (updateRequest.getEmail() != null) {
	            existingUser.setEmail(updateRequest.getEmail());
	        }


	        return userRepository.save(existingUser);
	    }
}
