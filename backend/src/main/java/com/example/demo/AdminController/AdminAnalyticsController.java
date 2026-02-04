package com.example.demo.AdminController;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.AdminService.AdminAnalyticsService;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "http://localhost:5173/", allowCredentials = "true")
@RequestMapping("/admin")
public class AdminAnalyticsController {
    
	 private static final Logger log =
	            LoggerFactory.getLogger(AdminAnalyticsController.class);

	
	private AdminAnalyticsService adminAnalyticsService;

	public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
		super();
		this.adminAnalyticsService = adminAnalyticsService;
	}
	
	@GetMapping("/analytics/month")
	public ResponseEntity<Map<String, Object>> getAnalyticsDataByService(@Param("month") int month, @Param("year") long year, HttpServletRequest request) {
		log.info("i am running");
		try {
			 Users users = (Users) request.getAttribute("Authorized_User");
	            
             if(users == null || !(users.getRole() == Role.ADMIN)) {
            	 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Unauthorized User"));
             }
			Map<String, Object> resultMap = adminAnalyticsService.getMonthlyBusiness(month, year);
			
			return ResponseEntity.ok().body(resultMap);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	
	@GetMapping("/analytics/year")
	public ResponseEntity<Map<String, Object>> getyearDayAnalytics(@Param("year") long year, HttpServletRequest request) {
		try {
			log.info("i am running");
			Users users = (Users) request.getAttribute("Authorized_User");
			
            if(users == null || !(users.getRole() == Role.ADMIN)) {
           	 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Unauthorized User"));
            }
            
            Map<String, Object> resultMap = adminAnalyticsService.getYearBusiness(year);
            return ResponseEntity.ok().body(resultMap);
     
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
