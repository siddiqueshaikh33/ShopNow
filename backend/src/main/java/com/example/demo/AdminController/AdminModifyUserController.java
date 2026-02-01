package com.example.demo.AdminController;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.AdminService.AdminModifyUserService;
import com.example.demo.Dto.AdminUpdateUserRequest;
import com.example.demo.Entity.Users;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "http://localhost:5173/", allowCredentials = "true")
@RequestMapping("/admin")
public class AdminModifyUserController {
    
	private AdminModifyUserService adminModifyUserService;

	
	
	public AdminModifyUserController(AdminModifyUserService adminModifyUserService) {
		super();
		this.adminModifyUserService = adminModifyUserService;
	}



	@GetMapping("/getDetails/{Userid}")
	public ResponseEntity<?> getUserDetails(@PathVariable int Userid, HttpServletRequest request){
		try {
			Users user = (Users) request.getAttribute("Authorized_User");
			
			if(user == null) {
				return ResponseEntity.badRequest().body("Unauthenticated User");
			}
			
			Users enteredUsers = adminModifyUserService.getUserDetails(Userid);
			
			return ResponseEntity.ok().body(enteredUsers);
			
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	  @PutMapping("/updateUser/{Userid}")
	    public ResponseEntity<?> updateUserDetails(
	            @PathVariable int Userid,
	            @RequestBody AdminUpdateUserRequest updateRequest,
	            HttpServletRequest request) {

	        try {
	            Users adminUser = (Users) request.getAttribute("Authorized_User");

	            if (adminUser == null) {
	                return ResponseEntity.badRequest().body("Unauthenticated User");
	            }

	            Users updatedUser = adminModifyUserService.updateUserDetails(Userid, updateRequest);

	            return ResponseEntity.ok(updatedUser);

	        } catch (Exception e) {
	            return ResponseEntity.badRequest().body(e.getMessage());
	        }
	    }
}
