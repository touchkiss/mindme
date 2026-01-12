package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.repository.ActivityRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ActivityController {

    private final ActivityRecordRepository repository;

    @GetMapping
    public Page<ActivityRecord> list(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (query != null && !query.isBlank()) {
            java.util.List<ActivityRecord> results = repository.search(query);
            return new org.springframework.data.domain.PageImpl<>(results, pageable, results.size());
        }
        return repository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityRecord> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        log.info("Deleted activity record: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ActivityStats stats() {
        long total = repository.count();
        long analyzed = repository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAnalyzed()))
                .count();
        return new ActivityStats(total, analyzed, total - analyzed);
    }

    public record ActivityStats(long total, long analyzed, long pending) {
    }
}
