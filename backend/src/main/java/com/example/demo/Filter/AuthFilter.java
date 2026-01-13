package com.example.demo.Filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.Entity.Users;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Services.AuthService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter(urlPatterns = {"/api/*", "/admin/*"})
@Component
public class AuthFilter implements Filter {

    private static final String ALLOW_ORIGIN = "http://localhost:5173";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    private static final String[] UNAUTHENTICATE_PATH = {
            "/api/users/register",
            "/api/auth/login"
    };

    public AuthFilter(UserRepository userRepository, AuthService authService) {
        super();
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            LOGGER.info("AuthFilter started");
            AuthorizationFilter(request, response, chain);
        } catch (Exception e) {
            LOGGER.error("Unexpected Error in Authenticate filter", e);
            sendErrorMessage((HttpServletResponse) response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    public void AuthorizationFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String req_url = req.getRequestURI();
        LOGGER.info("Incoming request URI: {}", req_url);
        LOGGER.info("HTTP Method: {}", req.getMethod());

        if (Arrays.asList(UNAUTHENTICATE_PATH).contains(req_url)) {
            LOGGER.info("Unauthenticated path accessed: {}", req_url);
            chain.doFilter(request, response);
            return;
        }

        if (req.getMethod().equalsIgnoreCase("OPTIONS")) {
            LOGGER.info("OPTIONS request detected, setting CORS headers");
            setCorsHeader(resp);
            return;
        }

        String token = getAuthTokenFromCookies(req);
        LOGGER.info("Token from cookies: {}", token != null ? "FOUND" : "NOT FOUND");

        if (token == null || !authService.verifyToken(token)) {
            LOGGER.warn("Token verification failed or token invalid");
            sendErrorMessage(resp,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Invalid Or Missing Token!");
            return;
        }

        LOGGER.info("Token verification successful");

        String username = authService.extractUsername(token);
        LOGGER.info("Extracted username from token: {}", username);

        Optional<Users> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            LOGGER.warn("User not found for username: {}", username);
            sendErrorMessage(resp,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: User Not Found!");
            return;
        }

        Users authorizedUser = userOptional.get();
        String role = authorizedUser.getRole().name();
        LOGGER.info("Authenticated user role: {}", role);

        if (req_url.startsWith("/admin") && !role.equals("ADMIN")) {
            LOGGER.warn("Admin access denied for user with role: {}", role);
            sendErrorMessage(resp,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Admin Access Required!");
            return;
        }

        if (req_url.startsWith("/api") && !role.equals("CUSTOMER")) {
            LOGGER.warn("Customer access denied for user with role: {}", role);
            sendErrorMessage(resp,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Customer Access Required!");
            return;
        }

        LOGGER.info("Authorization successful for user: {}", username);
        req.setAttribute("Authorized_User", authorizedUser);

        chain.doFilter(request, response);
    }

    public void setCorsHeader(HttpServletResponse resp) {
        LOGGER.info("Setting CORS headers");
        resp.setHeader("Access-Control-Allow-Origin", ALLOW_ORIGIN);
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    public String getAuthTokenFromCookies(HttpServletRequest request) {
        LOGGER.info("Fetching token from cookies");
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> "token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        LOGGER.warn("No cookies found in request");
        return null;
    }

    public void sendErrorMessage(HttpServletResponse resp, int status_code, String message)
            throws IOException {
        LOGGER.error("Sending error response: {} - {}", status_code, message);
        resp.setStatus(status_code);
        resp.getWriter().write(message);
    }
}

