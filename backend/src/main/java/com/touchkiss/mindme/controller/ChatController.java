package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiAnalysisService aiService;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        String answer = aiService.askQuestion(question);
        return Map.of("answer", answer);
    }
}
