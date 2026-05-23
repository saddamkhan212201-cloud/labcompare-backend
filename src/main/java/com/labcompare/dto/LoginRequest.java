package com.labcompare.dto;

/**
 * DTO for login and registration requests.
 *
 * LOGIN:    send { email, password }
 *           Backend also accepts { username, password } as fallback for
 *           seeded admin/superadmin accounts that have no email set.
 *
 * REGISTER: send { email, password, phone }
 *           Username is auto-derived server-side from the email local-part.
 */
public class LoginRequest {

    /** Primary identifier — email address (login + register) */
    private String email;

    private String password;

    /** Required on registration — permanently linked to the account */
    private String phone;

    /**
     * Legacy fallback — accepted on login so existing admin/superadmin
     * accounts (seeded without an email) continue to work unchanged.
     */
    private String username;

    public LoginRequest() {}

    public String getEmail()             { return email; }
    public void   setEmail(String e)     { this.email = e; }

    public String getPassword()          { return password; }
    public void   setPassword(String p)  { this.password = p; }

    public String getPhone()             { return phone; }
    public void   setPhone(String p)     { this.phone = p; }

    public String getUsername()          { return username; }
    public void   setUsername(String u)  { this.username = u; }
}