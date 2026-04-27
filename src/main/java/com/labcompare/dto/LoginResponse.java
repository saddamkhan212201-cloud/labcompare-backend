package com.labcompare.dto;

public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private Long adminLabId;

    public LoginResponse(String token, String username, String role, Long adminLabId) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.adminLabId = adminLabId;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Long getAdminLabId() { return adminLabId; }
}
