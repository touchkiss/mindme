package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.UserTag;
import com.touchkiss.mindme.repository.UserTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cognition")
@RequiredArgsConstructor
public class CognitionController {

    private final com.touchkiss.mindme.service.CognitiveHorizonService horizonService;
    private final UserTagRepository tagRepository;

    @GetMapping("/gaps")
    public List<GapSuggestion> getKnowledgeGaps() {
        // Simple logic: Find tags with low frequency (<= 2) but exist in the system
        // (implied relevance)
        // In a real system, we'd check co-occurrence or specific categories.
        // Here we just fetch some low-freq tags.
        List<UserTag> allTags = tagRepository.findAll();

        return allTags.stream()
                .filter(t -> t.getFrequency() <= 5) // "Underexplored"
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.shuffle(list);
                    return list.stream();
                }))
                .limit(5)
                .map(t -> new GapSuggestion(t.getTag(), "Underexplored Concept", "General"))
                .collect(Collectors.toList());
    }

    @GetMapping("/horizon")
    public List<HorizonCard> getCognitiveHorizon() {
        return horizonService.generateHorizon();
    }

    public record GapSuggestion(String topic, String reason, String category) {
    }

    public record HorizonCard(String title, String description, String type, String relevance)
            implements java.io.Serializable {
    }
}
