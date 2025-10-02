package com.guardlite.demo.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
class Alert {
    @Id
    UUID id;
    @ManyToOne
    Website website;
    @ManyToOne
    Check check;
    Instant createdAt;
    String status;
    String message;
}
