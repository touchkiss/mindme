package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.KnowledgeEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, UUID> {

    Page<KnowledgeEntry> findByCategory(String category, Pageable pageable);

    @Query("SELECT DISTINCT k.category FROM KnowledgeEntry k WHERE k.category IS NOT NULL")
    List<String> findDistinctCategories();

    @Query("SELECT k FROM KnowledgeEntry k WHERE " +
            "LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(k.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<KnowledgeEntry> search(@Param("query") String query, Pageable pageable);

    List<KnowledgeEntry> findBySourceRecordId(UUID sourceRecordId);
}
