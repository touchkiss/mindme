package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.ReadingQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReadingQueueRepository extends JpaRepository<ReadingQueue, UUID> {
    Page<ReadingQueue> findByIsReadFalseOrderByPriorityDescAddedAtDesc(Pageable pageable);

    boolean existsByUrlAndIsReadFalse(String url);

    java.util.List<ReadingQueue> findByIsReadFalseOrderByAddedAtDesc();
}
