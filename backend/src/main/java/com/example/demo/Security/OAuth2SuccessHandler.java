package com.example.demo.Security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.demo.Entity.Provider;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Services.AuthService;
import com.example.demo.Services.RefreshTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public OAuth2SuccessHandler(UserRepository userRepository, AuthService authService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String googleId = oauthUser.getAttribute("sub");

        Optional<Users> userOptional = userRepository.findByEmail(email);

        Users user;
        String jwtToken;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = new Users();
            user.setEmail(email);
            user.setUsername(email);
            user.setRole(Role.CUSTOMER);
            user.setProvider(Provider.GOOGLE);
            user.setProviderId(googleId);

            user = userRepository.save(user);
        }

        jwtToken = authService.validateCookie(user);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user);

        authService.addCookietoClient(response, jwtToken);
        response.addHeader(HttpHeaders.SET_COOKIE, authService.createRefreshTokenCookie(rawRefreshToken).toString());

        response.sendRedirect("http://localhost:5173/dashboard");
    }
}
