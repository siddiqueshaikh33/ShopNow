package com.example.demo.Dto;

import java.math.BigDecimal;

public class YearlyBusinessDto {
     
	private String month;
	
	private BigDecimal saleAmount;
	
	
	public YearlyBusinessDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}



	public YearlyBusinessDto(String month, BigDecimal saleAmount) {
		super();
		this.month = month;
		this.saleAmount = saleAmount;
	}



	public BigDecimal getSaleAmount() {
		return saleAmount;
	}



	public void setSaleAmount(BigDecimal saleAmount) {
		this.saleAmount = saleAmount;
	}

	
	
	
}
