package com.example.demo;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.Security.OAuth2SuccessHandler;

@Configuration
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/error").permitAll()

              
                .requestMatchers("/api/auth/**").permitAll()

            
                .requestMatchers("/api/users/**").permitAll()

                
                .requestMatchers("/oauth2/**").permitAll()

               
                .anyRequest().authenticated()
        )

        .formLogin(form -> form.disable());

        http.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)
        );

        http.httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}

