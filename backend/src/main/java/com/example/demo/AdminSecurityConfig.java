package com.example.demo;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfigurationSource;


import com.example.demo.Filter.AuthFilter;

import jakarta.annotation.PostConstruct;

@Configuration
public class AdminSecurityConfig {
	
	private final AuthFilter authFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    
    public AdminSecurityConfig(AuthFilter authFilter, 
                              CorsConfigurationSource corsConfigurationSource) {
        this.authFilter = authFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }
	    @PostConstruct
	    public void check() {
	        System.out.println("🔥 AdminSecurityConfig LOADED");
	    }


    @Bean
    @Order(1)
    SecurityFilterChain adminSecurity(HttpSecurity http) throws Exception {

	    	    http.securityMatcher(request -> 
	    	    request.getRequestURI().startsWith("/admin/") ||
	    	    request.getRequestURI().equals("/api/find/me")
	    	)
	            .csrf(csrf -> csrf.disable())
	            .cors(cors -> cors.configurationSource(corsConfigurationSource))
	            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
	            .authorizeHttpRequests(auth -> auth
	                .requestMatchers("/admin/**").hasAuthority("ADMIN")
	                .requestMatchers("/api/find/me").hasAnyAuthority("ADMIN", "CUSTOMER")
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



