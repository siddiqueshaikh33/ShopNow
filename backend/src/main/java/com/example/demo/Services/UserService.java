package com.example.demo.Services;




import org.springframework.stereotype.Service;

import com.example.demo.Entity.Users;
import com.example.demo.Repository.UserRepository;

@Service
public class UserService {
   
	private UserRepository userRepository;
	

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
		
	}
	
	public Users registerUser(Users user) {
		 if(userRepository.findByEmail(user.getEmail()).isPresent()) {
			 throw new RuntimeException("Email is already taken");
		 }
		 
		 if(userRepository.findByUsername(user.getUsername()).isPresent()) {
			 throw new RuntimeException("Username is already taken");
		 } 
		return  userRepository.save(user); 
	}
}
