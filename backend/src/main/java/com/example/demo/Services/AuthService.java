package com.example.demo.Services;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.Jwttoken;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.JwtRepository;
import com.example.demo.Repository.UserRepository;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {
    
	private final Key Secret_Key;
	
	private UserRepository userRepository;
	private JwtRepository jwtRepository;
	private final PasswordEncoder passwordEncoder;
	
	public AuthService(UserRepository userRepository, JwtRepository jwtRepository,
			PasswordEncoder passwordEncoder, @Value("${jwt.secret}") String jwtSecret) {
		super();
		this.userRepository = userRepository;
		this.jwtRepository = jwtRepository;
		this.passwordEncoder = passwordEncoder;
		
		if(jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
			 throw new IllegalArgumentException("jwt secret length is less then 64 please make it greater or equal to 64");
		}
		
		this.Secret_Key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}
	
	public Users authenicate(String username, String password) {
		Users users = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Invalid Username or No User Found Please Register"));
		
		String storedPassword = users.getPassword();
		if (storedPassword == null) {
			throw new RuntimeException("Invalid credentials");
		}

		// 1. If already BCrypt ($2a$, $2b$, $2y$), verify using PasswordEncoder
		if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
			if (!passwordEncoder.matches(password, storedPassword)) {
				throw new RuntimeException("Invalid Password");
			}
		}
		// 2. Legacy Migration: If plaintext matches, verify and immediately upgrade to BCrypt in DB!
		else if (storedPassword.equals(password)) {
			users.setPassword(passwordEncoder.encode(password));
			userRepository.save(users);
		} else {
			throw new RuntimeException("Invalid Password");
		}
		
		return users;
	}
	
	public String createNewCookie(String username, String role) {
		JwtBuilder jwtBuilder = Jwts.builder();
		
		jwtBuilder.setSubject(username);
		jwtBuilder.claim("role", role);
		jwtBuilder.setIssuedAt(new Date());
		jwtBuilder.setExpiration(new Date(System.currentTimeMillis() + (60 * 60 * 1000)));
		jwtBuilder.signWith(SignatureAlgorithm.HS256, Secret_Key);
		
		return jwtBuilder.compact();
	}
	
	public String validateCookie(Users user) {
		String token = "";
		Jwttoken existingJwttoken = jwtRepository.findByUserId(user.getUser_id());
		LocalDateTime nowDateTime = LocalDateTime.now();
		if(existingJwttoken != null && nowDateTime.isBefore(existingJwttoken.getExpires_at())) {
			token = existingJwttoken.getToken();
		} else {
			
			token = createNewCookie(user.getUsername(),user.getRole().name());
			
			if(existingJwttoken != null) {
			   jwtRepository.delete(existingJwttoken);
			}
			
			saveToken(user, token);
		}
		return token;
	}
	
	
	public void saveToken(Users user, String token) {
		 Jwttoken jwttoken = new Jwttoken(user, token, LocalDateTime.now().plusHours(1));
		 jwtRepository.save(jwttoken);
	}
	
	public void addCookietoClient(HttpServletResponse response, String token) {
		Cookie cookie = new Cookie("token", token);
		
		cookie.setPath("/");
		cookie.setMaxAge(3600);
		cookie.setHttpOnly(true);
		cookie.setDomain("localhost");
		cookie.setSecure(false);
		
		response.addCookie(cookie);
	}
	
	public boolean verifyToken(String token) {
		try {
		
		validateToken(token);
		
       Optional<Jwttoken> jwttoken = jwtRepository.findByToken(token);
       
		if(jwttoken.isPresent()) {
			return jwttoken.get().getExpires_at().isAfter(LocalDateTime.now());
		}
		
		return false;
		
		} catch (Exception e) {
			new RuntimeException("Token failed " + e.getMessage());
			return false;
		}
	}
	
	public String extractUsername(String token) {
		return Jwts.parserBuilder()
		.setSigningKey(Secret_Key)
		.build()
		.parseClaimsJws(token)
		.getBody()
		.getSubject();
	}
	
	public void validateToken(String token) {
		Jwts.parserBuilder()
		.setSigningKey(Secret_Key)
		.build()
		.parseClaimsJws(token);
	}
	
	
	public void processingLogout(Users user , HttpServletResponse response) {
		Cookie cookie = new Cookie("token", null);
	    cookie.setHttpOnly(true);
	    cookie.setSecure(false);
	    cookie.setPath("/");
	    cookie.setMaxAge(0);
	    response.addCookie(cookie);
	    
	   jwtRepository.deleteByUsersUserId(user.getUser_id());
	}
}
