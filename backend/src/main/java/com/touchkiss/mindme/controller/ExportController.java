package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.KnowledgeEntry;
import com.touchkiss.mindme.domain.ReadingQueue;
import com.touchkiss.mindme.repository.KnowledgeEntryRepository;
import com.touchkiss.mindme.repository.ReadingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExportController {

    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final ReadingQueueRepository readingQueueRepository;

    @GetMapping
    public ResponseEntity<ExportData> exportAllData() {
        log.info("Exporting all user data");
        List<KnowledgeEntry> knowledge = knowledgeEntryRepository.findAll();
        List<ReadingQueue> queue = readingQueueRepository.findAll();

        return ResponseEntity.ok(new ExportData(knowledge, queue));
    }

    public record ExportData(List<KnowledgeEntry> knowledge, List<ReadingQueue> queue) {
    }
}
