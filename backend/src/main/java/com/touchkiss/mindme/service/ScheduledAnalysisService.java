package com.touchkiss.mindme.service;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.domain.KnowledgeEntry;
import com.touchkiss.mindme.domain.UserInterest;
import com.touchkiss.mindme.domain.UserTag;
import com.touchkiss.mindme.repository.ActivityRecordRepository;
import com.touchkiss.mindme.repository.KnowledgeEntryRepository;
import com.touchkiss.mindme.repository.UserInterestRepository;
import com.touchkiss.mindme.repository.UserTagRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ScheduledAnalysisService {

    private final ActivityRecordRepository activityRepository;
    private final KnowledgeEntryRepository knowledgeRepository;
    private final UserInterestRepository interestRepository;
    private final UserTagRepository tagRepository;
    private final ChatLanguageModel chatModel;

    private static final int BATCH_SIZE = 10;

    public ScheduledAnalysisService(
            ActivityRecordRepository activityRepository,
            KnowledgeEntryRepository knowledgeRepository,
            UserInterestRepository interestRepository,
            UserTagRepository tagRepository,
            ChatLanguageModel chatModel) {
        this.activityRepository = activityRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.interestRepository = interestRepository;
        this.tagRepository = tagRepository;
        this.chatModel = chatModel;
    }

    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void analyzeUnprocessedRecords() {
        List<ActivityRecord> unanalyzed = activityRepository.findByAnalyzedFalse();

        if (unanalyzed.isEmpty()) {
            log.debug("No unanalyzed records found");
            return;
        }

        log.info("Found {} unanalyzed records, processing in batches of {}", unanalyzed.size(), BATCH_SIZE);

        for (int i = 0; i < Math.min(unanalyzed.size(), BATCH_SIZE); i++) {
            ActivityRecord record = unanalyzed.get(i);
            try {
                analyzeRecord(record);
                record.setAnalyzed(true);
                activityRepository.save(record);
                log.info("Analyzed record: {}", record.getTitle());
            } catch (Exception e) {
                log.error("Failed to analyze record {}: {}", record.getId(), e.getMessage());
            }
        }
    }

    private void analyzeRecord(ActivityRecord record) {
        String systemPrompt = """
                You are a knowledge extraction and content quality assessment assistant.

                First, evaluate the relevance and value of this web page visit:
                - Score 1-3: Low value (entertainment, gossip, social media browsing, idle chatting, forums without educational content)
                - Score 4-6: Medium value (general news, casual reading, reference lookup)
                - Score 7-10: High value (learning, research, technical content, professional development, deep work)

                Output format:
                ---SCORE---
                Value: [1-10]
                Reason: [brief reason in Chinese]
                ---END_SCORE---

                If the score is 4 or above, also extract knowledge points in this format:
                ---KNOWLEDGE---
                Title: [concise title]
                Category: [one of: Technology, Business, Research, Learning, News, Personal, Other]
                Tags: [comma-separated tags]
                Content: [key insight or takeaway in 1-3 sentences]
                InterestLevel: [high/medium/low based on depth and learning value]
                ---END---

                If the score is 3 or below, output only the SCORE section (no knowledge extraction needed).
                If the page is a login page, error page, or has no meaningful content, output:
                ---NO_KNOWLEDGE---

                IMPORTANT: All content (Title, Tags, Content, Reason) MUST be in Chinese (Simplified).
                """;

        String userPrompt = String.format("""
                Page Title: %s
                URL: %s
                Summary: %s
                Time Spent: %d seconds
                """,
                record.getTitle(),
                record.getUrl(),
                record.getContentSummary() != null ? record.getContentSummary() : "N/A",
                record.getDurationSeconds() != null ? record.getDurationSeconds() : 0);

        try {
            // Use LangChain4j ChatModel
            String response = chatModel.generate(systemPrompt + "\n\n" + userPrompt);

            if (response.contains("---NO_KNOWLEDGE---")) {
                log.debug("No knowledge extracted from: {}", record.getTitle());
                return;
            }

            parseAndSaveKnowledge(response, record);

        } catch (Exception e) {
            log.error("AI analysis failed for record {}: {}", record.getId(), e.getMessage());
        }
    }

    private void parseAndSaveKnowledge(String response, ActivityRecord source) {
        Pattern pattern = Pattern.compile(
                "---KNOWLEDGE---\\s*" +
                        "Title:\\s*(.+?)\\s*" +
                        "Category:\\s*(.+?)\\s*" +
                        "Tags:\\s*(.+?)\\s*" +
                        "Content:\\s*(.+?)\\s*" +
                        "---END---",
                Pattern.DOTALL);

        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            String title = matcher.group(1).trim();
            String category = matcher.group(2).trim();
            String[] tags = matcher.group(3).trim().split("\\s*,\\s*");
            String content = matcher.group(4).trim();

            // Save knowledge entry
            KnowledgeEntry entry = new KnowledgeEntry();
            entry.setTitle(title);
            entry.setCategory(category);
            entry.setTags(tags);
            entry.setContent(content);
            entry.setSourceRecordId(source.getId());
            knowledgeRepository.save(entry);
            log.info("Saved knowledge entry: {}", entry.getTitle());

            // Update user interest for category
            updateInterest(category);

            // Update user tags
            for (String tag : tags) {
                updateTag(tag.trim());
            }
        }
    }

    private void updateInterest(String category) {
        UserInterest interest = interestRepository.findByCategory(category)
                .orElseGet(() -> {
                    UserInterest newInterest = new UserInterest();
                    newInterest.setCategory(category);
                    newInterest.setWeight(0.0);
                    return newInterest;
                });
        interest.setWeight(interest.getWeight() + 1.0);
        interestRepository.save(interest);
    }

    private void updateTag(String tagName) {
        if (tagName.isEmpty())
            return;
        UserTag tag = tagRepository.findByTag(tagName)
                .orElseGet(() -> {
                    UserTag newTag = new UserTag();
                    newTag.setTag(tagName);
                    newTag.setFrequency(0);
                    return newTag;
                });
        tag.setFrequency(tag.getFrequency() + 1);
        tagRepository.save(tag);
    }

    @Transactional
    public int triggerAnalysis() {
        List<ActivityRecord> unanalyzed = activityRepository.findByAnalyzedFalse();
        int processed = 0;

        for (ActivityRecord record : unanalyzed) {
            try {
                analyzeRecord(record);
                record.setAnalyzed(true);
                activityRepository.save(record);
                processed++;
            } catch (Exception e) {
                log.error("Failed to analyze record {}: {}", record.getId(), e.getMessage());
            }
        }

        return processed;
    }
}
