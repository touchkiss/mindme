package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.domain.KnowledgeEntry;
import com.touchkiss.mindme.domain.ReadingQueue;
import com.touchkiss.mindme.domain.UserInterest;
import com.touchkiss.mindme.repository.ActivityRecordRepository;
import com.touchkiss.mindme.repository.KnowledgeEntryRepository;
import com.touchkiss.mindme.repository.ReadingQueueRepository;
import com.touchkiss.mindme.repository.UserInterestRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final UserInterestRepository interestRepository;
    private final ReadingQueueRepository queueRepository;
    private final ActivityRecordRepository activityRepository;
    private final KnowledgeEntryRepository knowledgeRepository;
    private final com.touchkiss.mindme.service.AiAnalysisService aiService;

    @GetMapping
    public ResponseEntity<DashboardData> getDashboardData() {
        DashboardData data = new DashboardData();

        // 1. Stats
        long totalKnowledge = knowledgeRepository.count();
        List<String> categories = knowledgeRepository.findDistinctCategories();
        // Tags roughly (just simplified count for now, real tags are in JSON array
        // inside KnowledgeEntry)
        long totalTags = 0; // Better approach would be a proper tag repo, but keeping simple

        // Count today's new knowledge
        // Assuming createdAt/updatedAt logic exists, standard JPA doesn't have it by
        // default in findAll size
        // Using activity logic instead for "today's new"
        long newToday = activityRepository.findByVisitTimeBetween(
                LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault())).size();

        data.setStats(new DashboardStats(totalKnowledge, categories.size(), 100, newToday)); // 100 dummy tags count

        // 2. Latest Knowledge
        List<KnowledgeEntry> latest = knowledgeRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        data.setKnowledge(latest);

        // 3. Interests
        List<UserInterest> interests = interestRepository.findAllByOrderByWeightDesc();
        data.setInterests(interests);

        // 4. Tags (Mock for now or extract from latest)
        // Extract tags from latest knowledge entries as trending
        Map<String, Integer> tagCounts = new HashMap<>();
        for (KnowledgeEntry k : latest) {
            if (k.getTags() != null) {
                for (String t : k.getTags()) {
                    tagCounts.put(t, tagCounts.getOrDefault(t, 0) + 1);
                }
            }
        }
        List<TagStat> tags = tagCounts.entrySet().stream()
                .map(e -> new TagStat(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()))
                .collect(Collectors.toList());
        data.setTags(tags);

        // 5. Smart Recommendations
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // Simple logic: If we have interests, pick content related to them
        List<String> topInterests = interests.stream().limit(3).map(UserInterest::getCategory).toList();

        // A. Rediscover (Relaxed for demo: Any analyzed record)
        List<ActivityRecord> oldRecords = activityRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAnalyzed()))
                .limit(50)
                .collect(Collectors.toList());
        Collections.shuffle(oldRecords);

        oldRecords.stream().limit(2).forEach(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "rediscover");
            item.put("title", r.getTitle());
            item.put("url", r.getUrl());
            item.put("reason", "Based on your history: " + r.getTitle());
            recommendations.add(item);
        });

        // B. Queue (Unread)
        List<ReadingQueue> queue = queueRepository.findByIsReadFalseOrderByAddedAtDesc();
        queue.stream().limit(2).forEach(q -> {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "queue");
            item.put("title", q.getTitle());
            item.put("url", q.getUrl());
            item.put("reason", "Reading Queue");
            recommendations.add(item);
        });

        data.setRecommendations(recommendations);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/daily-insight")
    public ResponseEntity<String> getDailyInsight() {
        // Return raw JSON string from AI service
        return ResponseEntity.ok(aiService.generateDailyInsight());
    }

    @Data
    public static class DashboardData {
        private DashboardStats stats;
        private List<TagStat> tags;
        private List<KnowledgeEntry> knowledge;
        private List<UserInterest> interests;
        private List<Map<String, Object>> recommendations;
    }

    @Data
    public static class DashboardStats {
        private final long totalKnowledge;
        private final long totalCategories;
        private final long totalTags;
        private final long todayNew;
    }

    @Data
    public static class TagStat {
        private final String tag;
        private final int frequency;
    }
}
