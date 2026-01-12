package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public List<ActivityRecord> search(@RequestParam String q) {
        return knowledgeService.search(q);
    }
}
