package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.annotation.PostConstruct;

@Configuration
public class AdminSecurityConfig {
	
	@PostConstruct
	public void check() {
	    System.out.println("🔥 AdminSecurityConfig LOADED");
	}

	
	
	
	@Order(1)
	  @Bean
	    public SecurityFilterChain adminSecurity(HttpSecurity http) throws Exception {

	        http
	            .securityMatcher("/admin/**")
	            .csrf(csrf -> csrf.disable())

	            .authorizeHttpRequests(auth -> auth
	                .anyRequest().permitAll()
	            )

	            // 🔥 CRITICAL
	            .oauth2Login(oauth -> oauth.disable())
	            .formLogin(form -> form.disable())
	            .httpBasic(basic -> basic.disable())

	            // 🔥 THIS STOPS REDIRECTS
	            .exceptionHandling(ex -> ex
	                .authenticationEntryPoint(
	                    (req, res, e) -> res.sendError(401, "Unauthorized")
	                )
	            );

	        return http.build();
	    }
}

