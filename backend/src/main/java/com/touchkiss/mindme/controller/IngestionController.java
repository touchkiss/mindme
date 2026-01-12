package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.repository.ActivityRecordRepository;
import com.touchkiss.mindme.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final com.touchkiss.mindme.service.KnowledgeService knowledgeService;

    @PostMapping("/activity")
    public ResponseEntity<Void> ingestActivity(@RequestBody List<ActivityRecord> records) {
        log.info("Received {} activity records", records.size());
        knowledgeService.saveActivities(records);
        return ResponseEntity.ok().build();
    }
}
