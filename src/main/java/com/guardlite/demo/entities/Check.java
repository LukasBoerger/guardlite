package com.guardlite.demo.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checks")
@Data
public class Check {
    @Id
    UUID id;
    @ManyToOne
    Website website;
    String type;
    String cadenceCron;
    boolean enabled;
    Instant lastRunAt;
}
