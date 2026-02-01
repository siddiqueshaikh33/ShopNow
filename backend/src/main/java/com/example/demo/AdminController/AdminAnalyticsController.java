package com.example.demo.AdminController;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173/", allowCredentials = "true")
public class AdminAnalyticsController {
    
	
	private AdminAnalyticsService adminAnalyticsService;

	public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
		super();
		this.adminAnalyticsService = adminAnalyticsService;
	}
	
	
}
