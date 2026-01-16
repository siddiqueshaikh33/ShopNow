package com.example.demo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table
public class Productimages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int image_id;
	
	
	@Column(nullable = false, columnDefinition = "TEXT")
	private String imageUrl;
	
	
	@OneToOne
	@JoinColumn(name = "productId", unique = true)
	@JsonIgnore
	private Products products;



	public Productimages() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Productimages(String imageUrl, Products products) {
		super();
		this.imageUrl = imageUrl;
		this.products = products;
	}


	public String getImageUrl() {
		return imageUrl;
	}


	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}


	public Products getProducts() {
		return products;
	}


	public void setProducts(Products products) {
		this.products = products;
	}


	public int getImage_id() {
		return image_id;
	}


	public void setImage_id(int image_id) {
		this.image_id = image_id;
	}
	
}
