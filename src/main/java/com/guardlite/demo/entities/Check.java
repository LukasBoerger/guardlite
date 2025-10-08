package com.guardlite.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checks")
@Data
public class Check {
    @Id
    @GeneratedValue
    UUID id;
    @ManyToOne
    Website website;
    String type;
    String cadenceCron;
    boolean enabled;
    Instant lastRunAt;
}
