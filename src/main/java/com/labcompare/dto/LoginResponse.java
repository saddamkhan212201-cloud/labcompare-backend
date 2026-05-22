package com.labcompare.dto;

public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private Long   adminLabId;
    private String phone;   // returned on login so frontend stores it

    public LoginResponse(String token, String username, String role, Long adminLabId, String phone) {
        this.token      = token;
        this.username   = username;
        this.role       = role;
        this.adminLabId = adminLabId;
        this.phone      = phone;
    }

    // Backward-compat constructor (no phone)
    public LoginResponse(String token, String username, String role, Long adminLabId) {
        this(token, username, role, adminLabId, null);
    }

    public String getToken()      { return token; }
    public String getUsername()   { return username; }
    public String getRole()       { return role; }
    public Long   getAdminLabId() { return adminLabId; }
    public String getPhone()      { return phone; }
}
