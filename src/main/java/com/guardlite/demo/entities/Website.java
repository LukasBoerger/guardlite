package com.guardlite.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "websites")
@Data
public class Website {
    @Id
    UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id")   // <— wichtig!
    User owner;
    String url;
    String cms;
    boolean active;
    Instant createdAt;
}
