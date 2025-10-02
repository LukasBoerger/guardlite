package com.guardlite.demo.entities;

import com.guardlite.demo.user.Role;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getUsername() {
        return email;
    }

    public String getPassword() {
        return passwordHash;
    }
}
