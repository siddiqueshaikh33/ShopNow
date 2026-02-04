package com.example.demo.Dto;

public class CountTotalCatgorySalesDto {
    
	private String category;
	
	private long categoryCount;

	public CountTotalCatgorySalesDto() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public CountTotalCatgorySalesDto(String category, long categoryCount) {
		super();
		this.category = category;
		this.categoryCount = categoryCount;
	}



	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public long getCategoryCount() {
		return categoryCount;
	}

	public void setCategoryCount(int categoryCount) {
		this.categoryCount = categoryCount;
	}
	
	
}
