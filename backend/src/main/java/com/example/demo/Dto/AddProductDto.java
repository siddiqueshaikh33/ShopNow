package com.example.demo.Dto;

import java.math.BigDecimal;

public class AddProductDto {
	
	private String name;
	
	private String description;
	
	private BigDecimal price;
	
	private String stock;
	
	private int categoryId;
	
	private String imgUrl;

	public AddProductDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public AddProductDto(String name, String description, BigDecimal price, String stock, int categoryId,
			String imgUrl) {
		super();
		this.name = name;
		this.description = description;
		this.price = price;
		this.stock = stock;
		this.categoryId = categoryId;
		this.imgUrl = imgUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getStock() {
		return stock;
	}

	public void setStock(String stock) {
		this.stock = stock;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	public String getImgUrl() {
		return imgUrl;
	}

	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	@Override
	public String toString() {
		return "AddProductDto [name=" + name + ", description=" + description + ", price=" + price + ", stock=" + stock
				+ ", categoryId=" + categoryId + ", imgUrl=" + imgUrl + "]";
	}
	
	
	


	
}
