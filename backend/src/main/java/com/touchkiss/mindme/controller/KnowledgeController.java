package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.KnowledgeEntry;
import com.touchkiss.mindme.repository.KnowledgeEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KnowledgeController {

    private final KnowledgeEntryRepository repository;

    @GetMapping
    public Page<KnowledgeEntry> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return repository.search(query, pageable);
        }
        if (category != null && !category.isBlank()) {
            return repository.findByCategory(category, pageable);
        }
        return repository.findAll(pageable);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return repository.findDistinctCategories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeEntry> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeEntry> update(
            @PathVariable UUID id,
            @RequestBody KnowledgeEntry updated) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setTitle(updated.getTitle());
                    existing.setContent(updated.getContent());
                    existing.setCategory(updated.getCategory());
                    existing.setTags(updated.getTags());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        log.info("Deleted knowledge entry: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public KnowledgeStats stats() {
        long total = repository.count();
        List<String> categories = repository.findDistinctCategories();
        return new KnowledgeStats(total, categories.size(), categories);
    }

    public record KnowledgeStats(long total, int categoryCount, List<String> categories) {
    }
}
