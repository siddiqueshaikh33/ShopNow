package com.example.demo.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table
public class Categories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
	private int categoryId;
	
    @Column(nullable = false)
	private String categoryName;

	
    
    
    public Categories(String categoryName) {
		super();
		this.categoryName = categoryName;
	}

	public Categories() {
		super();
		// TODO Auto-generated constructor stub
	}

	public int getCategory_id() {
		return categoryId;
	}

	public void setCategory_id(int categoryId) {
		this.categoryId= categoryId;
	}

	public String getCategory_name() {
		return categoryName;
	}

	public void setCategory_name(String category_name) {
		this.categoryName = category_name;
	}
}
