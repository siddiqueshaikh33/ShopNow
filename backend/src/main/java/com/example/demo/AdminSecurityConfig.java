package com.example.demo;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;

@Configuration
public class AdminSecurityConfig {
	


	    @PostConstruct
	    public void check() {
	        System.out.println("🔥 AdminSecurityConfig LOADED");
	    }

	 
	   
	    @Bean
	    @Order(1)
	    public SecurityFilterChain adminSecurity(HttpSecurity http) throws Exception {

	        http
	            .securityMatcher("/admin/**")
	            .securityMatcher("/api/find/me")
	            .csrf(csrf -> csrf.disable())
	            .cors(cors -> {})
	            .authorizeHttpRequests(auth -> auth
	                .anyRequest().permitAll()
	            )

	            .oauth2Login(oauth -> oauth.disable())
	            .formLogin(form -> form.disable())
	            .httpBasic(basic -> basic.disable())

	            .exceptionHandling(ex -> ex
	                .authenticationEntryPoint((req, res, e) -> res.sendError(401, "Unauthorized"))
	            );

	        return http.build();
	    }
	}



