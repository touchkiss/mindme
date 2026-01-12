package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.ReadingQueue;
import com.touchkiss.mindme.repository.ReadingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/reading-queue")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReadingQueueController {

    private final ReadingQueueRepository repository;

    @GetMapping
    public Page<ReadingQueue> list(@PageableDefault(size = 20) Pageable pageable) {
        return repository.findByIsReadFalseOrderByPriorityDescAddedAtDesc(pageable);
    }

    @PostMapping
    public ResponseEntity<ReadingQueue> add(@RequestBody ReadingQueue item) {
        if (repository.existsByUrlAndIsReadFalse(item.getUrl())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(repository.save(item));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        return repository.findById(id).map(item -> {
            item.setIsRead(true);
            item.setReadAt(ZonedDateTime.now());
            repository.save(item);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
