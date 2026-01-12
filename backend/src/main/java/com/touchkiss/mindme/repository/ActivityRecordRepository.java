package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.ActivityRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, UUID> {

    List<ActivityRecord> findByAnalyzedFalse();

    @Query(value = "SELECT * FROM activity_records WHERE to_tsvector('english', title || ' ' || COALESCE(content_summary, '')) @@ websearch_to_tsquery('english', :query)", nativeQuery = true)
    List<ActivityRecord> search(@Param("query") String query);

    List<ActivityRecord> findByVisitTimeBetween(java.time.ZonedDateTime start, java.time.ZonedDateTime end);
}
