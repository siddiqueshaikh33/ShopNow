package com.example.demo.AdminService;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.Dto.CountTotalCatgorySalesDto;
import com.example.demo.Dto.YearlyBusinessDto;
import com.example.demo.Entity.Status;
import com.example.demo.Repository.OrderRepo;

@Service
public class AdminAnalyticsService {
	
	 private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsService.class);
	
	private OrderRepo orderRepo;

	public AdminAnalyticsService(OrderRepo orderRepo) {
		super();
		this.orderRepo = orderRepo;
	}
	
	public Map<String, Object> getMonthlyBusiness(int month, long year) {

	        double totalSales = orderRepo.sumTotalAmountByMonthAndYear(month, year, Status.SUCCESS);
	    
	        List<CountTotalCatgorySalesDto> categorySalesCount =
	                orderRepo.countOrdersPerCategory(month, year, Status.SUCCESS);

	        Map<String, Object> responseMap = new HashMap<>();
	        responseMap.put("total_amount", totalSales);
	        responseMap.put("category_sales_count", categorySalesCount);

	        return responseMap;
	}
	
	public Map<String, Object> getYearBusiness(long year) {
	   // log.info("✅ Service running... year={}", year);
	    Map<String, Object> responseMap = new HashMap<>();
	    try {
	      //  log.info("📌 Calling repository: getMonthlyTotal(year={}, status={})", year, Status.SUCCESS);
	        List<YearlyBusinessDto> yearlyBusinessDtos =
	                orderRepo.getMonthlyTotal(year, Status.SUCCESS);

	        if (yearlyBusinessDtos == null) {
	           // log.warn("⚠️ Repository returned NULL list!");
	            yearlyBusinessDtos = new ArrayList<>();
	        }
	     //   log.info("✅ Repository success. Monthly data count={}", yearlyBusinessDtos.size());
	        responseMap.put("yearly_data", yearlyBusinessDtos);
	        return responseMap;
	    } catch (Exception e) {
	        responseMap.put("error", "Failed to fetch yearly analytics");
	        responseMap.put("message", e.getMessage());
	        return responseMap;
	        
	    }
	}


}
