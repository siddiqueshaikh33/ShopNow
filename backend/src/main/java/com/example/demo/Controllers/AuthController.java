package com.example.demo.Controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Dto.ErrorResponse;
import com.example.demo.Dto.LoginRequest;
import com.example.demo.Dto.TokenPairResponse;
import com.example.demo.Entity.Users;
import com.example.demo.Services.AuthService;
import com.example.demo.Services.RateLimitingService;
import com.example.demo.Services.RefreshTokenService;

import io.github.bucket4j.EstimationProbe;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/auth")
public class AuthController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
	private final AuthService authService;
	private final RefreshTokenService refreshTokenService;
	private final RateLimitingService rateLimitingService;

	public AuthController(AuthService authService, RefreshTokenService refreshTokenService, RateLimitingService rateLimitingService) {
		this.authService = authService;
		this.refreshTokenService = refreshTokenService;
		this.rateLimitingService = rateLimitingService;
	}

	@PostMapping("/login")
	public ResponseEntity<?> authLogin(HttpServletResponse res, @RequestBody LoginRequest loginRequest) {
		String username = loginRequest.getUsername();

		// 1. Dual-Key Check: Is this username temporarily locked due to rotating-IP attacks?
		EstimationProbe probe = rateLimitingService.checkUsernameAllowed(username);
		if (probe != null) {
			long retryAfter = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
			LOGGER.warn("Login rejected: account for username '{}' is temporarily locked out", username);
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.header("Retry-After", String.valueOf(retryAfter))
					.body(new ErrorResponse(
							HttpStatus.TOO_MANY_REQUESTS.value(),
							HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
							"Too many requests. Please try again later.",
							"/api/auth/login"
					));
		}

		try {
			Users users = authService.authenicate(loginRequest.getUsername(), loginRequest.getPassword());

			// 2. Successful login: reset any failed attempts on this username
			rateLimitingService.resetUsernameLimit(username);

			// 1. Create short-lived access token (15 mins) & attach cookie
			String accessToken = authService.validateCookie(users);
			authService.addCookietoClient(res, accessToken);

			// 2. Create long-lived refresh token (7 days) & attach HttpOnly SameSite=Strict cookie
			String rawRefreshToken = refreshTokenService.createRefreshToken(users);
			res.addHeader(HttpHeaders.SET_COOKIE, authService.createRefreshTokenCookie(rawRefreshToken).toString());

			LOGGER.info("User {} logged in successfully with token pair", users.getUsername());
			return ResponseEntity.ok(Map.of(
					"username", users.getUsername(),
					"role", users.getRole(),
					"message", "Login successful"
			));
		} catch (Exception e) {
			// 3. Failed password: record 1 failed attempt against this username
			rateLimitingService.recordFailedLogin(username);

			LOGGER.warn("Login failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> authRefresh(HttpServletRequest req, HttpServletResponse res) {
		try {
			String rawRefreshToken = extractCookieValue(req, "refreshToken");
			if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("message", "Refresh token is missing. Please log in."));
			}

			// Perform rotation: verifies token, invalidates old, checks reuse, issues new pair
			TokenPairResponse tokenPair = refreshTokenService.rotateRefreshToken(rawRefreshToken);

			// Attach new access token cookie
			authService.addCookietoClient(res, tokenPair.getAccessToken());

			// Attach new rotated refresh token cookie
			res.addHeader(HttpHeaders.SET_COOKIE, authService.createRefreshTokenCookie(tokenPair.getRefreshToken()).toString());

			return ResponseEntity.ok(Map.of(
					"username", tokenPair.getUser().getUsername(),
					"role", tokenPair.getUser().getRole(),
					"message", "Token refreshed successfully"
			));
		} catch (SecurityException | IllegalArgumentException e) {
			LOGGER.warn("Token refresh failed: {}", e.getMessage());
			// Clear compromised or expired cookies
			res.addHeader(HttpHeaders.SET_COOKIE, authService.clearAccessTokenCookie().toString());
			res.addHeader(HttpHeaders.SET_COOKIE, authService.clearRefreshTokenCookie().toString());
			res.addHeader(HttpHeaders.SET_COOKIE, authService.clearLegacyTokenCookie().toString());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			LOGGER.error("Unexpected error during token refresh: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "An unexpected error occurred during session refresh"));
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<String> handleLogout(HttpServletResponse response, HttpServletRequest req) {
		try {
			String rawRefreshToken = extractCookieValue(req, "refreshToken");
			if (rawRefreshToken != null) {
				refreshTokenService.revokeRefreshToken(rawRefreshToken);
			}

			Users users = (Users) req.getAttribute("Authorized_User");
			authService.processingLogout(users, response);

			return ResponseEntity.status(HttpStatus.OK).body("Logout Successful");
		} catch (Exception e) {
			LOGGER.error("Logout failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Logout Failed");
		}
	}

	private String extractCookieValue(HttpServletRequest request, String cookieName) {
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (cookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
