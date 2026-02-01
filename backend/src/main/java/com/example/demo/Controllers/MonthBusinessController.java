package com.example.demo.Controllers;

import java.util.HashMap;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


import com.example.demo.Services.MonthlyBusinessService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MonthBusinessController {
     
	private MonthlyBusinessService monthlyBusinessService;

	public MonthBusinessController(MonthlyBusinessService monthlyBusinessService) {
		super();
		this.monthlyBusinessService = monthlyBusinessService;
	}
	
	

}
