package com.example.demo.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "jwt_tokens")
public class Jwttoken {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int tokenId;
   
    @ManyToOne
    @JoinColumn(name = "user_id")
     private Users users;
  
    @Column
    private String token;
    
    @Column
    private LocalDateTime created_at;
    
    @Column
    private LocalDateTime expires_at;
    
    @PrePersist
    protected void onCreate() {
    	this.created_at = LocalDateTime.now();
    }

	public Jwttoken(Users users, String token, LocalDateTime expires_at) {
		super();
		this.users = users;
		this.token = token;
		this.expires_at = expires_at; 
	}
   
 
	public Jwttoken() {
		super();
	}



	public int getToken_id() {
		return tokenId;
	}

	public void setToken_id(int token_id) {
		this.tokenId = token_id;
	}

	public Users getUsers() {
		return users;
	}

	public void setUsers(Users users) {
		this.users = users;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public LocalDateTime getCreated_at() {
		return created_at;
	}

	public void setCreated_at(LocalDateTime created_at) {
		this.created_at = created_at;
	}

	public LocalDateTime getExpires_at() {
		return expires_at;
	}

	public void setExpires_at(LocalDateTime expires_at) {
		this.expires_at = expires_at;
	}
   
}
