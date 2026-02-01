package com.example.demo.Services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.Entity.Orders;
import com.example.demo.Repository.OrderRepo;

@Service
public class MonthlyBusinessService {
     
	private OrderRepo orderRepo;

	public MonthlyBusinessService(OrderRepo orderRepo) {
		super();
		this.orderRepo = orderRepo;
	}
	
	public List<Orders> getMonthBusiness(int month, int year) {
		
		List<Orders> orders = orderRepo.findByMonthAndYear(month, year);
		
		return orders;
	}
}
