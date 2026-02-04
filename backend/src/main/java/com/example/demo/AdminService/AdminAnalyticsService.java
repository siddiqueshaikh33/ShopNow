package com.example.demo.AdminService;




import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.Dto.CountTotalCatgorySalesDto;
import com.example.demo.Entity.Status;
import com.example.demo.Repository.OrderRepo;

@Service
public class AdminAnalyticsService {
	
	 private static final Logger LOGGER = LoggerFactory.getLogger(AdminAnalyticsService.class);
	
	private OrderRepo orderRepo;

	public AdminAnalyticsService(OrderRepo orderRepo) {
		super();
		this.orderRepo = orderRepo;
	}
	
	public Map<String, Object> getMonthlyBusiness(int month, long year) {

	        double totalSales = orderRepo.sumTotalAmountByMonthAndYear(month, year, Status.SUCCESS);
	    

	        List<CountTotalCatgorySalesDto> categorySalesCount =
	                orderRepo.countOrdersPerCategory(month, year, Status.SUCCESS);

	       

	        // Optional: print full list (only if small)
	     

	        Map<String, Object> responseMap = new HashMap<>();
	        responseMap.put("total_amount", totalSales);
	        responseMap.put("category_sales_count", categorySalesCount);

	     
	        return responseMap;
	}


}
