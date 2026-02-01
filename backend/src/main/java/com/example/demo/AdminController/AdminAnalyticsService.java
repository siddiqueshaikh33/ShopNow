package com.example.demo.AdminController;

import org.springframework.stereotype.Service;

import com.example.demo.Repository.OrderRepo;

@Service
public class AdminAnalyticsService {
	
	
	private OrderRepo orderRepo;

	public AdminAnalyticsService(OrderRepo orderRepo) {
		super();
		this.orderRepo = orderRepo;
	}
	
	public void getMonthlyBusiness() {
		  
		
	}

}
