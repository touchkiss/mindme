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
        List<ActivityRecord> toSave = new java.util.ArrayList<>();

        for (ActivityRecord record : records) {
            // If record has external ID, check for existing and update instead of insert
            if (record.getExternalId() != null && !record.getExternalId().isBlank()) {
                var existing = repository.findByExternalId(record.getExternalId());
                if (existing.isPresent()) {
                    // Merge: update existing record with new content
                    ActivityRecord existingRecord = existing.get();
                    existingRecord.setContentSummary(record.getContentSummary());
                    existingRecord.setTitle(record.getTitle());
                    existingRecord.setInteractionCount(record.getInteractionCount());
                    existingRecord.setVisitTime(record.getVisitTime());
                    existingRecord.setSearchQuery(record.getSearchQuery());
                    existingRecord.setAnalyzed(false); // Mark for re-analysis
                    toSave.add(existingRecord);
                    log.info("Updated existing activity with external ID: {}", record.getExternalId());
                    continue;
                }
            }
            toSave.add(record);
        }

        // 1. Save to Postgres
        List<ActivityRecord> saved = repository.saveAll(toSave);

        // 2. Index to Vector Store (Async usually)
        saved.forEach(vectorSearchService::indexActivity);
    }

    public List<ActivityRecord> search(String query) {
        return repository.search(query);
    }
}
