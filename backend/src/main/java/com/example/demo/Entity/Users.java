package com.example.demo.Entity;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
public class Users {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private int userId;
	
	@Column
	private String username;
	
	@Column
	private String email;
	
	@Column
	private String password;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime created_at;
	
	@Column(nullable = false)
	private LocalDateTime updated_at;
	
	  @Enumerated(EnumType.STRING)
	  @Column(nullable = false)
	  private Role role;
      
	 
	  public Users() {
		super();
	}

	    @PrePersist
	    protected void onCreate() {
	        this.created_at = LocalDateTime.now();
	        this.updated_at = LocalDateTime.now();
	    }
	    
	    @PreUpdate
	    protected void onUpdate() {
	        this.updated_at = LocalDateTime.now();
	    }

	  public Users(String username, String email, String password, Role role) {
		super();
		
		this.username = username;
		this.email = email;
		this.password = password;
		this.role = role;
	}


	  public int getUser_id() {
		  return userId;
	  }

	  public void setUser_id(int userId) {
		  this.userId = userId;
	  }

	  public String getUsername() {
		  return username;
	  }

	  public void setUsername(String username) {
		  this.username = username;
	  }

	  public String getEmail() {
		  return email;
	  }

	  public void setEmail(String email) {
		  this.email = email;
	  }

	  public String getPassword() {
		  return password;
	  }

	  public void setPassword(String password) {
		  this.password = password;
	  }

	  public Role getRole() {
		  return role;
	  }

	  public void setRole(Role role) {
		  this.role = role;
	  }
	  
	  
}
