package com.example.demo.Controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entity.Users;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/find")
public class MeController {
     

	@GetMapping("/me")
	public ResponseEntity<?> getUser(HttpServletRequest req) {
		Users users = (Users) req.getAttribute("Authorized_User");
		System.out.println(users.getUsername());
		if(users != null) {
			System.out.println("user" + users.getUsername() + "pass" + users.getPassword());
			return ResponseEntity.ok(Map.of("username", users.getUsername() , "role", users.getRole()));
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized User"));
	}
}
