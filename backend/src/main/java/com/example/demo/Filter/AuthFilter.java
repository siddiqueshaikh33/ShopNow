package com.example.demo.Filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Services.AuthService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    public AuthFilter(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {

        String reqUrl = req.getRequestURI();

        if (reqUrl.startsWith("/oauth2") ||
            reqUrl.startsWith("/login/oauth2") ||
            reqUrl.startsWith("/api/auth/login") ||
            reqUrl.startsWith("/api/users")) {
            chain.doFilter(req, resp);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String token = getAuthTokenFromCookies(req);
        if (token == null || !authService.verifyToken(token)) {
            LOGGER.warn("JWT Missing or Invalid");
            chain.doFilter(req, resp);
            return;
        }

        // Pure Stateless JWT Verification: claims are decoded in CPU RAM (0 DB queries)
        Claims claims = authService.extractAllClaims(token);
        String username = claims.getSubject();
        String roleStr = claims.get("role", String.class);
        Integer userId = claims.get("userId", Integer.class);

        // Construct lightweight user object in memory for backward compatibility with existing controllers
        Users user = new Users();
        if (userId != null) {
            user.setUser_id(userId);
        }
        user.setUsername(username);
        if (roleStr != null) {
            try {
                user.setRole(Role.valueOf(roleStr));
            } catch (Exception e) {
                user.setRole(Role.CUSTOMER);
            }
        } else {
            user.setRole(Role.CUSTOMER);
        }

        String role = user.getRole().name();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        req.setAttribute("Authorized_User", user);

        chain.doFilter(req, resp);
    }

    private String getAuthTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        // Prioritize "accessToken", fallback to legacy "token"
        return Arrays.stream(cookies)
                .filter(cookie -> "accessToken".equals(cookie.getName()) || "token".equals(cookie.getName()))
                .sorted((c1, c2) -> "accessToken".equals(c1.getName()) ? -1 : 1)
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
