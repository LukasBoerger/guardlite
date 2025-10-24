package com.guardlite.demo.repositories;

import com.guardlite.demo.entities.CheckResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CheckResultRepository extends JpaRepository<CheckResult, UUID> {
    List<CheckResult> findAllByCheck_IdOrderByRunAtDesc(UUID checkId);

    @Query("""
            select cr
            from CheckResult cr
            where cr.check.website.id = :websiteId
            order by cr.runAt desc
            """)
    List<CheckResult> findLatestByWebsiteId(@Param("websiteId") UUID websiteId, Pageable pageable);
}
