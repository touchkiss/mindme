package com.touchkiss.mindme.service;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.repository.ActivityRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final ActivityRecordRepository repository;
    private final VectorSearchService vectorSearchService;

    public void saveActivities(List<ActivityRecord> records) {
        // 1. Save to Postgres
        List<ActivityRecord> saved = repository.saveAll(records);

        // 2. Index to Vector Store (Async usually)
        saved.forEach(vectorSearchService::indexActivity);
    }

    public List<ActivityRecord> search(String query) {
        return repository.search(query);
    }
}
