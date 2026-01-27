package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.TrendItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendRepository extends JpaRepository<TrendItem, UUID> {

    Optional<TrendItem> findByUrl(String url);

    // Get latest items sorted by AI relevance and recency
    @Query("SELECT t FROM TrendItem t ORDER BY t.aiScore DESC, t.createdAt DESC")
    List<TrendItem> findTopRelevant(Pageable pageable);

    // Get latest items sorted by raw score and recency (for non-AI view)
    @Query("SELECT t FROM TrendItem t ORDER BY t.score DESC, t.createdAt DESC")
    List<TrendItem> findTopPopular(Pageable pageable);
}
