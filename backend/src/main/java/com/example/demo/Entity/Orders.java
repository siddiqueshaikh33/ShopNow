package com.example.demo.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table
public class Orders {

	@Id
	@Column(name = "order_id", nullable = false, unique = true)
	private String id;

	
	@ManyToOne
	@JoinColumn(name = "user_id")
	private Users user;
	
	@Column
	private BigDecimal total_amount;
	
	
	  @Enumerated(EnumType.STRING)
	  @Column(nullable = false)
	private Status status;

	  @Column(nullable = false, updatable = false)
	  private LocalDateTime updated_at;

      @Column(nullable = false)
	  private LocalDateTime created_at;
	  
      @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	  private List<OrderItems> orderItems;
      
	

	  @PrePersist
	    protected void onCreate() {
	        this.created_at = LocalDateTime.now();
	        this.updated_at = LocalDateTime.now();
	    }
	    
	    @PreUpdate
	    protected void onUpdate() {
	        this.updated_at = LocalDateTime.now();
	    }
	    

		public Orders() {
			super();
			// TODO Auto-generated constructor stub
		}

		public Orders(Users user, BigDecimal total_amount, Status status, LocalDateTime updated_at,
				LocalDateTime created_at) {
			super();
			this.user = user;
			this.total_amount = total_amount;
			this.status = status;
			this.updated_at = updated_at;
			this.created_at = created_at;
		}

	
		

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Users getUser() {
			return user;
		}

		public void setUser(Users user) {
			this.user = user;
		}

		public BigDecimal getTotal_amount() {
			return total_amount;
		}

		public void setTotal_amount(BigDecimal total_amount) {
			this.total_amount = total_amount;
		}

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public LocalDateTime getUpdated_at() {
			return updated_at;
		}

		public void setUpdated_at(LocalDateTime updated_at) {
			this.updated_at = updated_at;
		}

		public LocalDateTime getCreated_at() {
			return created_at;
		}

		public void setCreated_at(LocalDateTime created_at) {
			this.created_at = created_at;
		}

		public List<OrderItems> getOrderItems() {
			return orderItems;
		}

		public void setOrderItems(List<OrderItems> orderItems) {
			this.orderItems = orderItems;
		} 
		
}
