package com.touchkiss.mindme.service;

import com.touchkiss.mindme.domain.WatchedPage;
import com.touchkiss.mindme.repository.WatchedPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageWatchService {

    private final WatchedPageRepository repository;

    /**
     * Add a new page to watch list or update existing
     */
    @Transactional
    public WatchedPage addOrUpdateWatch(String url, String title, String contentSummary) {
        Optional<WatchedPage> existing = repository.findByUrl(url);

        WatchedPage page;
        if (existing.isPresent()) {
            page = existing.get();
            page.setTitle(title);

            // Check if content changed
            String newHash = hashContent(contentSummary);
            if (page.getLastContentHash() != null && !page.getLastContentHash().equals(newHash)) {
                page.setHasUpdates(true);
                page.setUpdateCount(page.getUpdateCount() + 1);
                log.info("Page content changed: {} (updates: {})", url, page.getUpdateCount());
            }

            page.setLastContentHash(newHash);
            page.setLastContentSummary(contentSummary);
            page.setLastChecked(ZonedDateTime.now());

        } else {
            page = new WatchedPage();
            page.setUrl(url);
            page.setTitle(title);
            page.setLastContentHash(hashContent(contentSummary));
            page.setLastContentSummary(contentSummary);
            page.setLastChecked(ZonedDateTime.now());
            page.setIsActive(true);
            log.info("New page added to watch list: {}", url);
        }

        return repository.save(page);
    }

    /**
     * Get all watched pages
     */
    public List<WatchedPage> getWatchList() {
        return repository.findByIsActiveTrue();
    }

    /**
     * Get pages with updates
     */
    public List<WatchedPage> getPagesWithUpdates() {
        return repository.findByHasUpdatesTrue();
    }

    /**
     * Mark page updates as seen
     */
    @Transactional
    public void markUpdatesSeen(UUID pageId) {
        repository.findById(pageId).ifPresent(page -> {
            page.setHasUpdates(false);
            repository.save(page);
        });
    }

    /**
     * Get pages that need to be checked
     */
    public List<WatchedPage> getPagesNeedingCheck() {
        // Get pages that haven't been checked in their specified interval
        ZonedDateTime cutoff = ZonedDateTime.now().minusHours(1); // Minimum 1 hour
        return repository.findPagesNeedingCheck(cutoff);
    }

    /**
     * Stop watching a page
     */
    @Transactional
    public void stopWatching(UUID pageId) {
        repository.findById(pageId).ifPresent(page -> {
            page.setIsActive(false);
            repository.save(page);
        });
    }

    /**
     * Delete a watched page
     */
    @Transactional
    public void deleteWatch(UUID pageId) {
        repository.deleteById(pageId);
    }

    /**
     * Hash content for comparison
     */
    private String hashContent(String content) {
        if (content == null || content.isBlank()) {
            return "empty";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16); // Use first 16 chars
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(content.hashCode());
        }
    }
}
