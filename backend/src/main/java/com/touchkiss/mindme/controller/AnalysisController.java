package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.service.AiAnalysisService;
import com.touchkiss.mindme.service.ScheduledAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final ScheduledAnalysisService scheduledAnalysisService;
    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerAnalysis() {
        log.info("Manual analysis triggered");
        int processed = scheduledAnalysisService.triggerAnalysis();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "processed", processed,
                "message", "Analyzed " + processed + " records"));
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, String>> generateReport(
            @RequestParam(required = false) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        log.info("Generating report for: {}", reportDate);

        try {
            String report = aiAnalysisService.generateDailyReport(reportDate);
            return ResponseEntity.ok(Map.of(
                    "date", reportDate.toString(),
                    "report", report));
        } catch (Exception e) {
            log.error("Failed to generate report: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }
}
