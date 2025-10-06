package com.guardlite.demo.repositories;

import com.guardlite.demo.entities.Website;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebsiteRepository extends JpaRepository<Website, UUID> {
    List<Website> findByOwner_Id(UUID ownerId);

    Optional<Website> findByIdAndOwner_Id(UUID id, UUID ownerId);
}
