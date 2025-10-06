package com.guardlite.demo.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "check_results")
@Data
public class CheckResult {
    @Id
    UUID id;
    @ManyToOne
    Check check;
    Instant runAt;
    String status;
    String payloadJson;
    String adviceText;
}
