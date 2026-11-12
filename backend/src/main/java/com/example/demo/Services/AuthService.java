package com.example.demo.Services;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.Users;
import com.example.demo.Repository.JwtRepository;
import com.example.demo.Repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

	private final Key Secret_Key;

	private final UserRepository userRepository;
	private final JwtRepository jwtRepository;
	private final PasswordEncoder passwordEncoder;
	private final boolean cookieSecure;

	@Autowired
	public AuthService(UserRepository userRepository, JwtRepository jwtRepository,
			PasswordEncoder passwordEncoder, @Value("${jwt.secret}") String jwtSecret,
			@Value("${cookie.secure:false}") boolean cookieSecure) {
		super();
		this.userRepository = userRepository;
		this.jwtRepository = jwtRepository;
		this.passwordEncoder = passwordEncoder;
		this.cookieSecure = cookieSecure;

		if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
			throw new IllegalArgumentException("jwt secret length is less then 64 please make it greater or equal to 64");
		}

		this.Secret_Key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public AuthService(UserRepository userRepository, JwtRepository jwtRepository,
			PasswordEncoder passwordEncoder, String jwtSecret) {
		this(userRepository, jwtRepository, passwordEncoder, jwtSecret, false);
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

	public String createNewCookie(Integer userId, String username, String role) {
		return Jwts.builder()
				.setSubject(username)
				.claim("userId", userId)
				.claim("role", role)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + (15 * 60 * 1000))) // 15 minutes validity
				.signWith(Secret_Key, SignatureAlgorithm.HS256)
				.compact();
	}

	public String createNewCookie(String username, String role) {
		return createNewCookie(null, username, role);
	}

	public String validateCookie(Users user) {
		// Pure Stateless JWT: token contains userId, username, and role; zero DB queries needed
		return createNewCookie(user.getUser_id(), user.getUsername(), user.getRole().name());
	}

	public ResponseCookie createAccessTokenCookie(String token) {
		return ResponseCookie.from("accessToken", token)
				.path("/")
				.maxAge(Duration.ofMinutes(15))
				.httpOnly(true)
				.sameSite("Lax")
				.secure(this.cookieSecure)
				.build();
	}

	public ResponseCookie createRefreshTokenCookie(String rawRefreshToken) {
		return ResponseCookie.from("refreshToken", rawRefreshToken)
				.path("/api/auth")
				.maxAge(Duration.ofDays(7))
				.httpOnly(true)
				.sameSite("Strict")
				.secure(this.cookieSecure)
				.build();
	}

	public ResponseCookie clearAccessTokenCookie() {
		return ResponseCookie.from("accessToken", "")
				.path("/")
				.maxAge(0)
				.httpOnly(true)
				.sameSite("Lax")
				.secure(this.cookieSecure)
				.build();
	}

	public ResponseCookie clearRefreshTokenCookie() {
		return ResponseCookie.from("refreshToken", "")
				.path("/api/auth")
				.maxAge(0)
				.httpOnly(true)
				.sameSite("Strict")
				.secure(this.cookieSecure)
				.build();
	}

	public ResponseCookie clearLegacyTokenCookie() {
		return ResponseCookie.from("token", "")
				.path("/")
				.maxAge(0)
				.httpOnly(true)
				.sameSite("Lax")
				.secure(this.cookieSecure)
				.build();
	}

	public void addCookietoClient(HttpServletResponse response, String token) {
		ResponseCookie accessCookie = createAccessTokenCookie(token);
		response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

		// Legacy token cookie for backward compatibility with older clients
		ResponseCookie legacyCookie = ResponseCookie.from("token", token)
				.path("/")
				.maxAge(Duration.ofMinutes(15))
				.httpOnly(true)
				.sameSite("Lax")
				.secure(this.cookieSecure)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, legacyCookie.toString());
	}

	public Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(Secret_Key)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public String extractUsername(String token) {
		return extractAllClaims(token).getSubject();
	}

	public boolean verifyToken(String token) {
		try {
			Claims claims = extractAllClaims(token);
			return claims.getExpiration().after(new Date());
		} catch (ExpiredJwtException e) {
			LOGGER.warn("JWT token expired: {}", e.getMessage());
			return false;
		} catch (JwtException e) {
			LOGGER.warn("Invalid JWT token signature or format: {}", e.getMessage());
			return false;
		} catch (Exception e) {
			LOGGER.error("JWT verification failed: {}", e.getMessage());
			return false;
		}
	}

	public void validateToken(String token) {
		extractAllClaims(token);
	}

	public void processingLogout(Users user, HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, clearAccessTokenCookie().toString());
		response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString());
		response.addHeader(HttpHeaders.SET_COOKIE, clearLegacyTokenCookie().toString());

		if (user != null && user.getUser_id() != 0) {
			try {
				jwtRepository.deleteByUsersUserId(user.getUser_id());
			} catch (Exception ignored) {
			}
		}
	}
}
