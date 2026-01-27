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
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ActivityController {

    private final ActivityRecordRepository repository;

    @GetMapping
    public Page<ActivityRecord> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam(required = false) Boolean analyzed,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 20, sort = "visitTime", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        return repository.findAll(
                com.touchkiss.mindme.repository.ActivitySpecifications.withFilters(query, hostname, startDate, endDate,
                        analyzed, maxScore, maxDuration, tag),
                pageable);
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

    @PostMapping("/batch/delete")
    public ResponseEntity<Void> batchDelete(@RequestBody java.util.List<UUID> ids) {
        repository.deleteAllById(ids);
        log.info("Batch deleted {} activities", ids.size());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/analyze")
    public ResponseEntity<Void> batchAnalyze(@RequestBody java.util.List<UUID> ids) {
        java.util.List<ActivityRecord> records = repository.findAllById(ids);
        records.forEach(r -> r.setAnalyzed(false)); // Reset to allow re-analysis
        repository.saveAll(records);
        log.info("Marked {} activities for re-analysis", records.size());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch/boost")
    public ResponseEntity<Void> batchBoost(@RequestBody java.util.List<UUID> ids) {
        java.util.List<ActivityRecord> records = repository.findAllById(ids);
        records.forEach(r -> {
            r.setInterestScore(Math.min(100, (r.getInterestScore() == null ? 0 : r.getInterestScore()) + 20));
        });
        repository.saveAll(records);
        log.info("Boosted interest for {} activities", records.size());
        return ResponseEntity.ok().build();
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
