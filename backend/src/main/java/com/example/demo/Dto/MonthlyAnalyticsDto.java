package com.example.demo.Dto;

import java.util.List;

public class MonthlyAnalyticsDto {
    private double totalSale;
    
    private List<CountTotalCatgorySalesDto> catgorySalesDtos;

	public MonthlyAnalyticsDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public double getTotalSale() {
		return totalSale;
	}

	public void setTotalSale(double totalSale) {
		this.totalSale = totalSale;
	}

	public List<CountTotalCatgorySalesDto> getCatgorySalesDtos() {
		return catgorySalesDtos;
	}

	public void setCatgorySalesDtos(List<CountTotalCatgorySalesDto> catgorySalesDtos) {
		this.catgorySalesDtos = catgorySalesDtos;
	}
    
    
}
