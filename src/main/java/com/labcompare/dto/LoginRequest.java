package com.labcompare.dto;

public class LoginRequest {
    private String username;
    private String password;
    private String email;   // ← this line must be present

    public LoginRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }        // ← this must be present
    public void setEmail(String email) { this.email = email; }  // ← and this
}