package com.touchkiss.mindme.service;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.repository.ActivityRecordRepository;
import com.touchkiss.mindme.repository.UserInterestsRepository;
import com.touchkiss.mindme.domain.UserInterest;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final ActivityRecordRepository repository;
    private final UserInterestsRepository interestsRepository;
    private final ChatLanguageModel chatModel;
    private final VectorSearchService vectorSearchService;

    public String generateDailyReport(LocalDate date) {
        List<ActivityRecord> records = repository.findAll();

        String activitiesText = records.stream()
                .filter(r -> r.getVisitTime() != null && r.getVisitTime().toLocalDate().equals(date))
                .map(r -> String.format("- %s [%s] (Summary: %s)",
                        r.getVisitTime().toLocalTime(),
                        r.getTitle(),
                        r.getContentSummary()))
                .collect(Collectors.joining("\n"));

        if (activitiesText.isEmpty()) {
            return "No activities found for " + date;
        }

        String template = """
                You are a helpful personal assistant. Your goal is to analyze the user's daily web browsing history and generate a structured daily work report.
                Focus on:
                1. Identifying the main tasks worked on (e.g. Coding, Research, Meetings).
                2. Summarizing key learnings or consumed content.
                3. Highlighting any specific "Deep Work" sessions based on continuity of topics.

                Input format: List of timestamps, page titles, and short summaries.
                Output format: Markdown.

                IMPORTANT: Always respond in Chinese (Simplified).

                Here is my activity log for {{date}}:
                {{activities}}
                """;

        PromptTemplate promptTemplate = PromptTemplate.from(template);
        Map<String, Object> variables = new HashMap<>();
        variables.put("date", date.toString());
        variables.put("activities", activitiesText);

        Prompt prompt = promptTemplate.apply(variables);

        try {
            return chatModel.generate(prompt.text());
        } catch (Exception e) {
            log.error("Failed to generate report", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    public String askQuestion(String question) {
        try {
            // 1. Retrieve relevant knowledge
            List<String> relevantDocs = vectorSearchService.searchSimilar(question, 5);
            String context = String.join("\n\n", relevantDocs);

            if (context.isEmpty()) {
                return chatModel.generate("Question: " + question + "\n(No context found)");
            }

            // 2. RAG Prompt
            String template = """
                    You are a knowledgeable assistant. Answer the question based on the provided context only.
                    If the answer is not in the context, say "I don't have enough information in my knowledge base".

                    Context:
                    {{context}}

                    Question:
                    {{question}}

                    Answer in Chinese:
                    """;

            PromptTemplate promptTemplate = PromptTemplate.from(template);
            Map<String, Object> variables = new HashMap<>();
            variables.put("context", context);
            variables.put("question", question);

            Prompt prompt = promptTemplate.apply(variables);

            return chatModel.generate(prompt.text());

        } catch (Exception e) {
            log.error("Failed to answer question", e);
            return "Error: " + e.getMessage();
        }
    }

    public String generatePeriodReport(LocalDate start, LocalDate end, String type) {
        // Convert LocalDate to ZonedDateTime range
        var startDateTime = start.atStartOfDay(java.time.ZoneId.systemDefault());
        var endDateTime = end.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault());

        List<ActivityRecord> records = repository.findByVisitTimeBetween(startDateTime, endDateTime);

        String activitiesText = records.stream()
                .map(r -> String.format("- %s [%s] (Summary: %s)",
                        r.getVisitTime().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                        r.getTitle(),
                        r.getContentSummary()))
                .collect(Collectors.joining("\n"));

        if (activitiesText.isEmpty()) {
            return "No activities found for this period.";
        }

        String template = """
                You are a helpful personal assistant. Your goal is to generate a structured {{type}} work report based on the user's web browsing history.

                Focus on:
                1. **Key Accomplishments**: What main topics or projects did the user work on?
                2. **Knowledge Gained**: Summarize the most important articles or pages read.
                3. **Deep Work Analysis**: Identify periods of focused work vs distraction.
                4. **Suggestions**: Provide 1-2 suggestions for improving improved productivity or knowledge management.

                Input format: List of timestamps, page titles, and short summaries.
                Output format: Markdown.

                IMPORTANT: Always respond in Chinese (Simplified).

                Period: {{start}} to {{end}}
                Activity Log:
                {{activities}}
                """;

        PromptTemplate promptTemplate = PromptTemplate.from(template);
        Map<String, Object> variables = new HashMap<>();
        variables.put("type", type);
        variables.put("start", start.toString());
        variables.put("end", end.toString());
        variables.put("activities", activitiesText);

        Prompt prompt = promptTemplate.apply(variables);

        try {
            return chatModel.generate(prompt.text());
        } catch (Exception e) {
            log.error("Failed to generate period report", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    public String generateDailyInsight() {
        // Get today's activities
        var start = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault());
        var end = java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault());

        List<ActivityRecord> records = repository.findByVisitTimeBetween(start, end);

        if (records.isEmpty()) {
            return "{\"summary\": \"No activity today\", \"suggestions\": []}";
        }

        // Limit to last 20 relevant items to save context/tokens
        String activitiesText = records.stream()
                .filter(r -> r.getTitle() != null && !r.getTitle().isBlank())
                .sorted((a, b) -> b.getVisitTime().compareTo(a.getVisitTime()))
                .limit(20)
                .map(r -> String.format("- %s", r.getTitle()))
                .collect(Collectors.joining("\n"));

        String template = """
                Analyze the user's browsing history for today and provide predictions on what they might want to explore next.

                Browsing History:
                {{activities}}

                Return a JSON object with this EXACT structure (no markdown code blocks):
                {
                  "summary": "One short sentence summarizing today's main interest in Chinese",
                  "suggestions": [
                    "Keyword or short question 1",
                    "Keyword or short question 2",
                    "Keyword or short question 3"
                  ],
                  "recommendedContent": {
                    "title": "A suggested title to search/read",
                    "reason": "Why this is relevant"
                  }
                }

                Ensure the content is high quality and relevant.
                """;

        PromptTemplate promptTemplate = PromptTemplate.from(template);
        Map<String, Object> variables = new HashMap<>();
        variables.put("activities", activitiesText);

        Prompt prompt = promptTemplate.apply(variables);

        try {
            String rawResponse = chatModel.generate(prompt.text());
            return cleanJson(rawResponse);
        } catch (Exception e) {
            log.error("Failed to generate insight", e);
            return "{\"error\": \"Failed to generate insight\"}";
        }
    }

    private String cleanJson(String response) {
        if (response == null)
            return "{}";
        // Remove <think>...</think> blocks common in some reasoning models
        response = response.replaceAll("(?s)<think>.*?</think>", "").trim();
        // Remove markdown code blocks if present
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }

    public Map<String, Integer> scoreRelevance(List<String> titles) {
        List<UserInterest> interests = interestsRepository.findAllByOrderByWeightDesc();
        if (interests.isEmpty()) {
            return new HashMap<>(); // No personalization possible yet
        }

        String interestProfile = interests.stream()
                .limit(10)
                .map(i -> i.getCategory() + "(" + i.getWeight() + ")")
                .collect(Collectors.joining(", "));

        String itemsText = String.join("\n", titles);

        String template = """
                You are a smart content curator. Score the following news titles from 0 to 100 based on how relevant they are to the user's interests.

                User Interests: {{interests}}

                News Titles:
                {{items}}

                Return a JSON object where key is the exact title and value is the integer score.
                Example: {"Title A": 80, "Title B": 20}
                """;

        PromptTemplate promptTemplate = PromptTemplate.from(template);
        Map<String, Object> variables = new HashMap<>();
        variables.put("interests", interestProfile);
        variables.put("items", itemsText);

        Prompt prompt = promptTemplate.apply(variables);

        try {
            String json = cleanJson(chatModel.generate(prompt.text()));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {
            });
        } catch (Exception e) {
            log.error("Failed to score relevance", e);
            return new HashMap<>();
        }
    }
}
