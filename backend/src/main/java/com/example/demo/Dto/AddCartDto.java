package com.example.demo.Dto;


public class AddCartDto {
    
	private String username;
	
	private int product_id;
	
	private int quantity;

	public AddCartDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public AddCartDto(String username, int product_id, int quantity) {
		super();
		this.username = username;
		this.product_id = product_id;
		this.quantity = quantity;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getProduct_id() {
		return product_id;
	}

	public void setProduct_id(int product_id) {
		this.product_id = product_id;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	
	
}
