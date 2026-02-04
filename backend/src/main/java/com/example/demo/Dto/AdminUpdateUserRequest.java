package com.example.demo.Dto;

public class AdminUpdateUserRequest {

    private String username;
    private String email;
    private String phone;
    private String role;     // if you have role
    private Boolean active;  // if you have status

    public AdminUpdateUserRequest() {}

   

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

