package com.example.demo.Entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table
public class Products {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int productId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private String stock;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Categories categories;
    
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime created_at;
    
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updated_at;
    
    @OneToOne(
    	    mappedBy = "products",
    	    cascade = CascadeType.ALL,   // 👈 THIS is important
    	    orphanRemoval = true,        // 👈 ensures cleanup
    	    fetch = FetchType.LAZY
    	)
    private Productimages primaryImage;

    


	public Productimages getPrimaryImage() {
		return primaryImage;
	}

	public void setPrimaryImage(Productimages primaryImage) {
		this.primaryImage = primaryImage;
	}

	public Products() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Products(String name, String description, BigDecimal prices, String stock, Categories categories,
			LocalDateTime created_at, LocalDateTime updated_at) {
		super();
		this.name = name;
		this.description = description;
		this.price = prices;
		this.stock = stock;
		this.categories = categories;
		this.created_at = created_at;
		this.updated_at = updated_at;
	}
	
	

	public Products(int productId) {
		super();
		this.productId = productId;
	}

	public int getProduct_id() {
		return productId;
	}

	public void setProduct_id(int product_id) {
		this.productId = product_id;
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

	public BigDecimal getPrices() {
		return price;
	}

	public void setPrices(BigDecimal prices) {
		this.price = prices;
	}

	public String getStock() {
		return stock;
	}

	public void setStock(String stock) {
		this.stock = stock;
	}

	public Categories getCategories() {
		return categories;
	}

	public void setCategories(Categories categories) {
		this.categories = categories;
	}

	public LocalDateTime getCreated_at() {
		return created_at;
	}

	public void setCreated_at(LocalDateTime created_at) {
		this.created_at = created_at;
	}

	public LocalDateTime getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(LocalDateTime updated_at) {
		this.updated_at = updated_at;
	}
      
    
}
