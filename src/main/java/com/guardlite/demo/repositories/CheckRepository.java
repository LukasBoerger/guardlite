package com.guardlite.demo.repositories;

import com.guardlite.demo.entities.Check;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckRepository extends JpaRepository<Check, UUID> {
    @Query("select c from Check c")
    List<Check> findAllForScheduler(); // simpel für Start

    Optional<Check> findByWebsite_Id(UUID id);

    Optional<Check> findByIdAndWebsite_Owner_Id(UUID id, UUID ownerId);
}