package com.labcompare.dto;

public class LoginRequest {
    private String username;
    private String password;
    private String email;   // optional — registration only
    private String phone;   // required on registration; stored in JWT for ownership checks

    public LoginRequest() {}

    public String getUsername()          { return username; }
    public void   setUsername(String u)  { this.username = u; }

    public String getPassword()          { return password; }
    public void   setPassword(String p)  { this.password = p; }

    public String getEmail()             { return email; }
    public void   setEmail(String e)     { this.email = e; }

    public String getPhone()             { return phone; }
    public void   setPhone(String p)     { this.phone = p; }
}
