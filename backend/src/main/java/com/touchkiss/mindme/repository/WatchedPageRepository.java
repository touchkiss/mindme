package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.WatchedPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchedPageRepository extends JpaRepository<WatchedPage, UUID> {

    Optional<WatchedPage> findByUrl(String url);

    List<WatchedPage> findByIsActiveTrue();

    List<WatchedPage> findByHasUpdatesTrue();

    @Query("SELECT w FROM WatchedPage w WHERE w.isActive = true AND " +
            "(w.lastChecked IS NULL OR w.lastChecked < :cutoff)")
    List<WatchedPage> findPagesNeedingCheck(ZonedDateTime cutoff);
}
