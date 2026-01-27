package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.domain.WatchedPage;
import com.touchkiss.mindme.service.PageWatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final com.touchkiss.mindme.service.KnowledgeService knowledgeService;
    private final PageWatchService pageWatchService;

    @PostMapping("/activity")
    public ResponseEntity<Void> ingestActivity(@RequestBody List<ActivityRecord> records) {
        log.info("Received {} activity records", records.size());
        knowledgeService.saveActivities(records);
        return ResponseEntity.ok().build();
    }

    // ============== Browser Agent Endpoints ==============

    @PostMapping("/agent-result")
    public ResponseEntity<Map<String, Object>> receiveAgentResult(@RequestBody AgentResultRequest request) {
        log.info("Agent result: {} (quality: {})", request.url(), request.qualityScore());

        // If this is a watch check, update the watched page
        if ("watch_check".equals(request.taskType())) {
            WatchedPage page = pageWatchService.addOrUpdateWatch(
                    request.url(),
                    request.title(),
                    request.contentSummary());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "pageId", page.getId().toString(),
                    "hasUpdates", page.getHasUpdates()));
        }

        // For other task types (pre-validate, deep-dive), just log
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/watch")
    public ResponseEntity<List<WatchedPage>> getWatchList() {
        return ResponseEntity.ok(pageWatchService.getWatchList());
    }

    @GetMapping("/watch/updates")
    public ResponseEntity<List<WatchedPage>> getPagesWithUpdates() {
        return ResponseEntity.ok(pageWatchService.getPagesWithUpdates());
    }

    @PostMapping("/watch/{pageId}/seen")
    public ResponseEntity<Void> markUpdatesSeen(@PathVariable UUID pageId) {
        pageWatchService.markUpdatesSeen(pageId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/watch/{pageId}")
    public ResponseEntity<Void> deleteWatch(@PathVariable UUID pageId) {
        pageWatchService.deleteWatch(pageId);
        return ResponseEntity.ok().build();
    }

    // Request record for agent results
    public record AgentResultRequest(
            String taskId,
            String taskType,
            String url,
            String title,
            String contentSummary,
            Integer contentLength,
            Integer qualityScore,
            List<String> qualityIssues,
            List<Map<String, String>> relatedLinks,
            Map<String, Object> metadata) {
    }
}
