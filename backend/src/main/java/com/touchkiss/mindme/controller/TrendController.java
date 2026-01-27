package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.TrendItem;
import com.touchkiss.mindme.service.TrendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrendController {

    private final TrendService trendService;

    @GetMapping
    public List<TrendItem> getTrends(@RequestParam(defaultValue = "false") boolean aiFilter) {
        try {
            return trendService.getTrends(aiFilter);
        } catch (Exception e) {
            log.error("Failed to fetch trends", e);
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshTrends() {
        try {
            trendService.fetchAndProcessTrends();
            return ResponseEntity.ok("Trends refreshed successfully");
        } catch (Exception e) {
            log.error("Failed to refresh trends", e);
            return ResponseEntity.internalServerError().body("Failed to refresh trends");
        }
    }
}
