

package com.example.demo;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


import com.example.demo.Repository.UserRepository;
import com.example.demo.Security.OAuth2SuccessHandler;
import com.example.demo.Services.AuthService;



@Configuration
public class SecurityConfig {
	
	private OAuth2SuccessHandler oAuth2SuccessHandler;
	
	
	
	public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler) {
		super();
		this.oAuth2SuccessHandler = oAuth2SuccessHandler;
	}



	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
            		.requestMatchers("/api/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()   // allow public access
                .requestMatchers("/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
            		  .successHandler(oAuth2SuccessHandler)  // optional
            )

            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}


