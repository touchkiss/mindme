package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final AiAnalysisService aiAnalysisService;

    @GetMapping("/daily")
    public ResponseEntity<Map<String, String>> getDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        String report = aiAnalysisService.generateDailyReport(date);
        return ResponseEntity.ok(Map.of("report", report, "period", date.toString()));
    }

    @GetMapping("/weekly")
    public ResponseEntity<Map<String, String>> getWeeklyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        // Calculate start (Monday) and end (Sunday) of the week
        LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        String report = aiAnalysisService.generatePeriodReport(start, end, "Weekly");
        return ResponseEntity.ok(Map.of("report", report, "period", start + " to " + end));
    }

    @GetMapping("/monthly")
    public ResponseEntity<Map<String, String>> getMonthlyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        // Calculate start and end of the month
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = date.with(TemporalAdjusters.lastDayOfMonth());

        String report = aiAnalysisService.generatePeriodReport(start, end, "Monthly");
        return ResponseEntity.ok(Map.of("report", report, "period", start + " to " + end));
    }
}
