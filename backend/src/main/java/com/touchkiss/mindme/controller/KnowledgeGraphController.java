package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KnowledgeGraphController {

    private final KnowledgeGraphService graphService;

    @PostMapping("/rebuild")
    public ResponseEntity<Void> rebuild() {
        graphService.rebuildGraph();
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getGraph() {
        return ResponseEntity.ok(graphService.getGraphData());
    }
}
