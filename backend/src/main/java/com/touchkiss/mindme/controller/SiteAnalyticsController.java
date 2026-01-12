package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.domain.KnowledgeEntry;
import com.touchkiss.mindme.repository.ActivityRecordRepository;
import com.touchkiss.mindme.repository.KnowledgeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/sites")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SiteAnalyticsController {

    private final ActivityRecordRepository activityRepository;
    private final KnowledgeEntryRepository knowledgeRepository;

    /**
     * Get site-based activity analytics
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getSiteAnalytics(
            @RequestParam(defaultValue = "visits") String sortBy) {

        List<ActivityRecord> allActivities = activityRepository.findAll();

        // Group by domain
        Map<String, List<ActivityRecord>> byDomain = allActivities.stream()
                .filter(a -> a.getUrl() != null)
                .collect(Collectors.groupingBy(a -> extractDomain(a.getUrl())));

        List<Map<String, Object>> siteStats = byDomain.entrySet().stream()
                .map(entry -> {
                    String domain = entry.getKey();
                    List<ActivityRecord> records = entry.getValue();

                    long totalDuration = records.stream()
                            .mapToLong(r -> r.getDurationSeconds() != null ? r.getDurationSeconds() : 0)
                            .sum();

                    long analyzedCount = records.stream().filter(r -> Boolean.TRUE.equals(r.getAnalyzed())).count();

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("domain", domain);
                    stats.put("visits", records.size());
                    stats.put("totalDuration", totalDuration);
                    stats.put("averageDuration", records.isEmpty() ? 0 : totalDuration / records.size());
                    stats.put("analyzedCount", analyzedCount);
                    stats.put("lastVisit", records.stream()
                            .map(ActivityRecord::getVisitTime)
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .orElse(null));

                    return stats;
                })
                .sorted((a, b) -> {
                    switch (sortBy) {
                        case "duration":
                            return Long.compare((Long) b.get("totalDuration"), (Long) a.get("totalDuration"));
                        case "analyzed":
                            return Long.compare((Long) b.get("analyzedCount"), (Long) a.get("analyzedCount"));
                        default:
                            return Integer.compare((Integer) b.get("visits"), (Integer) a.get("visits"));
                    }
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(siteStats);
    }

    /**
     * Get knowledge grouped by source domain
     */
    @GetMapping("/knowledge")
    public ResponseEntity<List<Map<String, Object>>> getKnowledgeBySite(
            @RequestParam(defaultValue = "count") String sortBy) {

        List<KnowledgeEntry> allKnowledge = knowledgeRepository.findAll();
        List<ActivityRecord> allActivities = activityRepository.findAll();

        // Create mapping of record ID to domain
        Map<UUID, String> recordToDomain = allActivities.stream()
                .filter(a -> a.getUrl() != null)
                .collect(Collectors.toMap(ActivityRecord::getId, a -> extractDomain(a.getUrl()), (a, b) -> a));

        // Group knowledge by domain
        Map<String, List<KnowledgeEntry>> byDomain = allKnowledge.stream()
                .filter(k -> k.getSourceRecordId() != null)
                .collect(Collectors.groupingBy(k -> recordToDomain.getOrDefault(k.getSourceRecordId(), "unknown")));

        List<Map<String, Object>> result = byDomain.entrySet().stream()
                .filter(e -> !"unknown".equals(e.getKey()))
                .map(entry -> {
                    String domain = entry.getKey();
                    List<KnowledgeEntry> entries = entry.getValue();

                    // Count categories
                    Map<String, Long> categories = entries.stream()
                            .collect(Collectors.groupingBy(KnowledgeEntry::getCategory, Collectors.counting()));

                    // Collect all tags
                    List<String> allTags = entries.stream()
                            .flatMap(k -> Arrays.stream(k.getTags()))
                            .distinct()
                            .limit(10)
                            .collect(Collectors.toList());

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("domain", domain);
                    stats.put("knowledgeCount", entries.size());
                    stats.put("categories", categories);
                    stats.put("tags", allTags);
                    stats.put("titles", entries.stream()
                            .map(KnowledgeEntry::getTitle)
                            .limit(5)
                            .collect(Collectors.toList()));

                    return stats;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("knowledgeCount"), (Integer) a.get("knowledgeCount")))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null)
                return "unknown";
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
