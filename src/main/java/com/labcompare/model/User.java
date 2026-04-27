package com.labcompare.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * For ADMIN users: the ID of the lab they manage.
     * NULL means the admin can manage ALL labs (used by SUPERADMIN implicitly).
     */
    @Column(name = "admin_lab_id")
    private Long adminLabId;

    public enum Role { SUPERADMIN, ADMIN, USER }

    public User() {}

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(String username, String password, Role role, Long adminLabId) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.adminLabId = adminLabId;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Long getAdminLabId() { return adminLabId; }
    public void setAdminLabId(Long adminLabId) { this.adminLabId = adminLabId; }
}
