package com.example.demo.Filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.Entity.Users;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Services.AuthService;

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
        //LOGGER.info("JwtAuthFilter request: {} {}", req.getMethod(), reqUrl);

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
       // LOGGER.warn("JWT token", token);
        if (token == null || !authService.verifyToken(token)) {
            LOGGER.warn("JWT Missing or Invalid");
            chain.doFilter(req, resp); 
            return;
        }

        String username = authService.extractUsername(token);
      //  LOGGER.warn("username", username);
        Optional<Users> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            LOGGER.warn("User not found for username {}", username);
            chain.doFilter(req, resp);
            return;
        }

        Users user = userOptional.get();
       // LOGGER.info("user ", user);
        String role = user.getRole().name(); 
     //   LOGGER.info("user Role ", role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,  
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        //LOGGER.info("user auth", user);
        req.setAttribute("Authorized_User", user);


        chain.doFilter(req, resp);
    }

    private String getAuthTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(cookie -> "token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
