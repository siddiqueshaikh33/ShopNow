package com.example.demo.Controllers;



import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Dto.LoginRequest;
import com.example.demo.Entity.Users;
import com.example.demo.Services.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/auth")
public class AuthController {

	private AuthService authService;

	public AuthController(AuthService authService) {
		super();
		this.authService = authService;
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> authLogin(HttpServletResponse res,@RequestBody LoginRequest loginRequest) {
		try {
		Users users = authService.authenicate(loginRequest.getUsername(), loginRequest.getPassword());
		
		String tokenString = authService.validateCookie(users);
		authService.addCookietoClient(res, tokenString);
			
		return ResponseEntity.ok(Map.of("username", users.getUsername() ,"role", users.getRole(),"message", "Login successful"));
			
		} catch (Exception e) {
			// TODO: handle exception
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
		}
	}
	
	@GetMapping("/me")
	public ResponseEntity<?> getUser(HttpServletRequest req) {
		Users users = (Users) req.getAttribute("Authorized_User");
		if(users != null) {
			System.out.println("user" + users.getUsername() + "pass" + users.getPassword());
			return ResponseEntity.ok(Map.of("username", users.getUsername() , "role", users.getRole()));
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized User"));
	}
	
	@PostMapping("/logout")
	public ResponseEntity<String> handleLogout(HttpServletResponse response, HttpServletRequest req) {
		try {
		Users users= (Users) req.getAttribute("Authorized_User");
		authService.processingLogout(users, response);
	    return ResponseEntity.status(HttpStatus.OK).body("Logout Successfull");
		}catch (Exception e) {
			 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Logout Failed");
		}
	}
}
