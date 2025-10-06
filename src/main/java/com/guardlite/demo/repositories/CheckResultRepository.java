package com.guardlite.demo.repositories;

import com.guardlite.demo.entities.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CheckResultRepository extends JpaRepository<CheckResult, UUID> {
}
